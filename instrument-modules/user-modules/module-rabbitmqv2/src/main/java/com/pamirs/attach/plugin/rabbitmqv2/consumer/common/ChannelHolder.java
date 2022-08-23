/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.rabbitmqv2.consumer.common;

import com.pamirs.attach.plugin.rabbitmqv2.consumer.model.AdminAccessInfo;
import com.pamirs.attach.plugin.rabbitmqv2.utils.RabbitMqUtils;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.impl.AMQConnection;
import com.rabbitmq.client.impl.ChannelManager;
import com.rabbitmq.client.impl.ConsumerWorkService;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import com.shulie.instrument.simulator.api.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 负责与业务 Channel 一对一进行映射，防止直接操作业务 Channel 导致 Channel 关闭的问题
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/25 2:34 下午
 */
public class ChannelHolder {
    private final static Logger LOGGER = LoggerFactory.getLogger(ChannelHolder.class);
    private final static boolean isInfoEnabled = LOGGER.isInfoEnabled();

    private static ConcurrentHashMap<Integer, Connection> connectionCache = new ConcurrentHashMap<Integer, Connection>();

    /**
     * 获取影子 Channel
     *
     * @param target
     * @return
     */
    public static Channel getOrShadowChannel(Channel target) {
        if (target == null || !target.isOpen()) {
            return null;
        }

        Channel channel;
        try {
            Connection ptConnect = getPtConnect(target.getConnection());
            channel = ptConnect.createChannel();
        } catch (Exception e) {
            LOGGER.warn("[RabbitMQ] can't create shadow channel from biz channel", e);
            return null;
        }
        return channel;
    }

    private static Connection getPtConnect(Connection connection) throws IOException, TimeoutException {
        int key = System.identityHashCode(connection);
        Connection ptConnection = connectionCache.get(key);
        if (ptConnection == null || !ptConnection.isOpen()) {
            try {
                ptConnection = connectionFactory(connection).newConnection("pt_connect-" + connection.toString());
            } catch (NoSuchMethodError e) {
                //低版本
                ptConnection = connectionFactory(connection).newConnection();
            }
            connectionCache.put(key, ptConnection);
        }
        return ptConnection;
    }

    private static ConnectionFactory connectionFactory(Connection connection) {
        AdminAccessInfo adminAccessInfo = AdminAccessInfo.solveByConnection(connection);
        String host = connection.getAddress().getHostAddress();
        int port = connection.getPort();
        ConnectionFactory connectionFactory = new ConnectionFactory();
        // 配置连接信息
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(adminAccessInfo.getUsername());
        connectionFactory.setPassword(adminAccessInfo.getPassword());
        connectionFactory.setVirtualHost(adminAccessInfo.getVirtualHost());
        connectionFactory.setConnectionTimeout(10000);
        try {
            connectionFactory.setHandshakeTimeout(10000);
        } catch (Throwable ignore) {
            //版本兼容性
        }
        connectionFactory.setSharedExecutor(initWorkService(connection));
        // 网络异常自动连接恢复
        connectionFactory.setAutomaticRecoveryEnabled(true);
        // 每10秒尝试重试连接一次
        connectionFactory.setNetworkRecoveryInterval(10000);
        return connectionFactory;
    }

    private static ExecutorService initWorkService(Connection connection) {
        connection = RabbitMqUtils.unWrapConnection(connection);
        ThreadPoolInfo threadPoolInfo = getThreadPoolInfo(connection);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                    "[RabbitMQ] create shadow consumer ExecutorService with [coreSize : {}, maxSize : {}, keepAliveTime : {}, "
                            + "queueSize : {}]",
                    threadPoolInfo.getCoreSize(), threadPoolInfo.getMaxSize(), threadPoolInfo.getKeepAliveTime(),
                    threadPoolInfo.getBlockQueueCapacity());
        }
        return threadPoolInfo.build();
    }

    private static ThreadPoolInfo getThreadPoolInfo(Connection connection) {
        ThreadPoolInfo result = getThreadPoolInfoFromConfig();
        if (result == null) {
            result = getThreadPoolInfoFromBiz(connection);
        }
        if (result == null) {
            result = getDefaultThreadPoolInfo();
            if (isInfoEnabled) {
                LOGGER.info("[RabbitMQ] consumer thread pool use default value");
            }
        }
        return result;
    }

    private static ThreadPoolInfo getDefaultThreadPoolInfo() {
        return new ThreadPoolInfo(Runtime.getRuntime().availableProcessors() * 3,
                Runtime.getRuntime().availableProcessors() * 3,
                120, Integer.MAX_VALUE);
    }

    private static ThreadPoolInfo getThreadPoolInfoFromConfig() {
        String maxPoolsizeStr = System.getProperty("rabbitmq.consumer.maxPoolsize");
        String coreSizeStr = System.getProperty("rabbitmq.consumer.coreSize");
        String keepAliveTimeStr = System.getProperty("rabbitmq.consumer.keepAliveTime");
        String queueSizeStr = System.getProperty("rabbitmq.consumer.queueSize");
        if (!StringUtil.isEmpty(maxPoolsizeStr) && !StringUtil.isEmpty(coreSizeStr) && !StringUtil.isEmpty(
                keepAliveTimeStr) && !StringUtil.isEmpty(queueSizeStr)) {
            try {
                if (isInfoEnabled) {
                    LOGGER.info("[RabbitMQ] consumer thread pool use config value");
                }
                int queueSize = Integer.parseInt(queueSizeStr);
                queueSize = queueSize < 0 ? Integer.MAX_VALUE : queueSize;
                return new ThreadPoolInfo(Integer.parseInt(coreSizeStr),
                        Integer.parseInt(maxPoolsizeStr),
                        Integer.parseInt(keepAliveTimeStr), queueSize);
            } catch (NumberFormatException e) {
                if (isInfoEnabled) {
                    LOGGER.info("[RabbitMQ] consumer thread pool config error must all num");
                }
            }
        }
        return null;
    }

    private static ThreadPoolInfo getThreadPoolInfoFromBiz(Connection connection) {
        if (connection instanceof AMQConnection) {
            try {
                ConsumerWorkService workService = Reflect.on(connection).get("_workService");
                ExecutorService executorService = Reflect.on(workService).get("executor");
                if (executorService instanceof ThreadPoolExecutor) {
                    ThreadPoolExecutor te = (ThreadPoolExecutor) executorService;
                    int queueSize = getQueueSize(te.getQueue());
                    if (isInfoEnabled) {
                        LOGGER.info(
                                "[RabbitMQ] biz consumer use thread pool info : [coreSize : {}, maxSize : {}, keepAliveTime : "
                                        + "{} "
                                        + "queueSize : {}]",
                                te.getCorePoolSize(), te.getMaximumPoolSize(), te.getKeepAliveTime(TimeUnit.SECONDS), queueSize);
                    }
                    return new ThreadPoolInfo(te.getCorePoolSize(), te.getMaximumPoolSize(),
                            (int) te.getKeepAliveTime(TimeUnit.SECONDS), queueSize);
                }
            } catch (Exception e) {
                LOGGER.warn("[RabbitMQ]  can not get executor from connection"
                                + " try set max pool size equal channel num connection is : {}",
                        connection.getClass().getName(), e);
                try {
                    ChannelManager channelManager = Reflect.on(connection).get("_channelManager");
                    Map map = Reflect.on(channelManager).get("_channelMap");
                    return new ThreadPoolInfo(map.size(), map.size(), 10, Integer.MAX_VALUE);
                } catch (ReflectException ex) {
                    LOGGER.warn("[RabbitMQ]  can not get channels from connection connection is : {}",
                            connection.getClass().getName(), e);
                }
            }
        }
        return null;
    }

    private static int getQueueSize(BlockingQueue<Runnable> queue) {
        if (queue instanceof LinkedBlockingQueue) {
            return Reflect.on(queue).get("capacity");
        }
        if (queue instanceof ArrayBlockingQueue) {
            return ((Object[]) Reflect.on(queue).get("items")).length;
        }
        if (queue instanceof SynchronousQueue) {
            return 0;
        }
        LOGGER.warn("unknown BlockingQueue : {} will use unbound queue", queue.getClass().getName());
        return Integer.MAX_VALUE;
    }
}
