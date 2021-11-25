/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.rabbitmq.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.pamirs.attach.plugin.rabbitmq.consumer.ConsumerMetaData;
import com.pamirs.attach.plugin.rabbitmq.utils.AdminAccessInfo;
import com.pamirs.pradar.Pradar;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.impl.AMQConnection;
import com.rabbitmq.client.impl.ChannelManager;
import com.rabbitmq.client.impl.ChannelN;
import com.rabbitmq.client.impl.ConsumerWorkService;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import com.shulie.instrument.simulator.api.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 负责与业务 Channel 一对一进行映射，防止直接操作业务 Channel 导致 Channel 关闭的问题
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/25 2:34 下午
 */
public class ChannelHolder {
    private final static Logger LOGGER = LoggerFactory.getLogger(ChannelHolder.class);
    private final static boolean isInfoEnabled = LOGGER.isInfoEnabled();
    public static final Object NULL_OBJECT = new Object();
    private static ConcurrentHashMap<Integer, ConcurrentMap<String, Object>> consumerTags
        = new ConcurrentHashMap<Integer, ConcurrentMap<String, Object>>();
    private static ConcurrentHashMap<String, String> queueTags = new ConcurrentHashMap<String, String>();
    /**
     * 影子channel和业务consumerMeta的映射
     */
    public static ConcurrentHashMap<Channel, ConfigCache.ConsumerMetaDataCacheKey> shadowChannelWithBizMetaCache
        = new ConcurrentHashMap();
    /**
     * queue和channel应该是一对多
     */
    private static ConcurrentHashMap<String, List<Channel>> queueChannel = new ConcurrentHashMap<String, List<Channel>>();
    private static ConcurrentHashMap<Integer, Channel> channelCache = new ConcurrentHashMap<Integer, Channel>();
    private static ConcurrentHashMap<Integer, Connection> connectionCache = new ConcurrentHashMap<Integer, Connection>();
    private static ConcurrentHashMap<Integer, Integer> channelConnectionKeyCache = new ConcurrentHashMap<Integer, Integer>();
    /**
     * 业务 consumerTag -> 影子 consumerTag
     */
    private static ConcurrentHashMap<String, String> shadowTags = new ConcurrentHashMap<String, String>();

    public static void clearOneShadowChannel(Channel shadowChannel, String busQueue) {
        int key = System.identityHashCode(shadowChannel);
        consumerTags.remove(key);
        String ptQueue = null;
        if (Pradar.isClusterTestPrefix(busQueue)) {
            ptQueue = busQueue;
        } else {
            ptQueue = Pradar.addClusterTestPrefix(busQueue);
        }
        queueChannel.remove(ptQueue);
        String ptConsumerTag = null;
        for (Map.Entry<String, String> entry : queueTags.entrySet()) {
            if (entry.getValue().equals(ptQueue)) {
                queueTags.remove(entry.getKey());
                ptConsumerTag = entry.getKey();
                break;
            }
        }
        Integer busConnectionKey = channelConnectionKeyCache.get(key);
        if (busConnectionKey != null) {
            try {
                if (connectionCache.get(busConnectionKey) != null) {
                    connectionCache.get(busConnectionKey).close();
                    connectionCache.remove(busConnectionKey);
                }
            } catch (IOException e) {
                LOGGER.error("ptConnection close error {}", e);
            }
        }

        channelConnectionKeyCache.remove(key);
        for (Map.Entry<Integer, Channel> entry : channelCache.entrySet()) {
            if (entry.getValue().equals(shadowChannel)) {
                channelCache.remove(entry.getKey());
                break;
            }
        }
        for (Map.Entry<String, String> entry : shadowTags.entrySet()) {
            if (entry.getValue().equals(ptConsumerTag)) {
                shadowTags.remove(entry.getKey());
                break;
            }
        }
    }

    public static void release() {
        consumerTags.clear();
        queueTags.clear();
        for (Map.Entry<Integer, Channel> entry : channelCache.entrySet()) {
            if (entry.getValue() != null && entry.getValue().isOpen()) {
                try {
                    entry.getValue().close();
                } catch (Throwable e) {
                    LOGGER.error("rabbitmq close shadow channel error.", e);
                }
            }
        }
        channelCache.clear();
        connectionCache.clear();
        queueChannel.clear();
    }

    public static String getQueueByTag(String consumerTag) {
        return queueTags.get(consumerTag);
    }

    /**
     * 向当前连接绑定一个 consumerTag
     *
     * @param target
     * @param businessConsumerTag 业务的 consumerTag
     * @param shadowConsumerTag   影子 consumerTag
     * @param queue               业务 queue 名称
     */
    public static void addConsumerTag(Channel target, String businessConsumerTag, String shadowConsumerTag, String queue) {
        int identityCode = System.identityHashCode(target);
        Channel shadowChannel = channelCache.get(identityCode);
        if (shadowChannel == null) {
            return;
        }
        int key = System.identityHashCode(shadowChannel);
        ConcurrentMap<String, Object> tags = consumerTags.get(key);
        if (tags == null) {
            tags = new ConcurrentHashMap<String, Object>();
            ConcurrentMap<String, Object> oldTags = consumerTags.putIfAbsent(key, tags);
            if (oldTags != null) {
                tags = oldTags;
            }
        }
        tags.put(shadowConsumerTag, NULL_OBJECT);
        /**
         * 防止并发情况，有其他的 在移除 ConsumerTag 可能导致该 Set 从 consumerTags 里面被移除
         */
        consumerTags.put(key, tags);
        queueTags.put(shadowConsumerTag, queue);
        shadowTags.put(businessConsumerTag, shadowConsumerTag);
        cacheQueuewithShadowChannel(queue, shadowChannel);
        /**
         * 影子channel和consumerMeta的映射
         */
        shadowChannelWithBizMetaCache.put(shadowChannel,
            new ConfigCache.ConsumerMetaDataCacheKey(System.identityHashCode(target), businessConsumerTag));
    }

    private static void cacheQueuewithShadowChannel(String queue, Channel channel) {
        List value = queueChannel.get(queue);
        if (value == null) {
            value = new ArrayList();
            value.add(channel);
        } else {
            value.add(channel);
        }
        queueChannel.put(queue, value);
    }

    public static ConsumeResult consumeShadowQueue(Channel target, ConsumerMetaData consumerMetaData) throws IOException {
        return consumeShadowQueue(target, consumerMetaData.getPtQueue(), consumerMetaData.isAutoAck(),
            consumerMetaData.getPtConsumerTag(), false, consumerMetaData.isExclusive(),
            consumerMetaData.getArguments(), consumerMetaData.getPrefetchCount(),
            new ShadowConsumerProxy(consumerMetaData.getConsumer()));
    }

    public static ConsumeResult consumeShadowQueue(Channel target, String ptQueue, boolean autoAck, String ptConsumerTag,
        boolean noLocal, boolean exclusive, Map<String, Object> arguments, final Consumer consumer) throws IOException {
        return consumeShadowQueue(target, ptQueue, autoAck, ptConsumerTag, noLocal, exclusive, arguments, -1, consumer);
    }

    /**
     * 增加影子消费者的订阅
     *
     * @param target
     * @param ptQueue
     * @param autoAck
     * @param ptConsumerTag
     * @param noLocal
     * @param exclusive
     * @param arguments
     * @param consumer
     * @return 返回 consumerTag
     */
    public static synchronized ConsumeResult consumeShadowQueue(Channel target, String ptQueue, boolean autoAck,
        String ptConsumerTag,
        boolean noLocal, boolean exclusive, Map<String, Object> arguments, int prefetchCount,
        Consumer consumer) throws IOException {
        Channel shadowChannel = getShadowChannel(target);
        if (shadowChannel == null) {
            LOGGER.warn("rabbitmq basicConsume failed. cause by shadow channel is not found. queue={}, consumerTag={}",
                ptQueue, ptConsumerTag);
            return null;
        }
        if (!shadowChannel.isOpen()) {
            LOGGER.warn("rabbitmq basicConsume failed. cause by shadow channel is not closed. queue={}, consumerTag={}",
                ptQueue, ptConsumerTag);
            return null;
        }
        if (prefetchCount > 0) {
            shadowChannel.basicQos(prefetchCount);
        }
        String result = shadowChannel.basicConsume(ptQueue, autoAck, ptConsumerTag, noLocal, exclusive, arguments, consumer);
        final int key = System.identityHashCode(shadowChannel);
        ConfigCache.putQueue(key, ptQueue);
        return new ConsumeResult(shadowChannel, result);
    }

    /**
     * 取消影子 consumer 的订阅，取消后会移除 consumerTag 的缓存
     *
     * @param target      业务 channel
     * @param consumerTag consumerTag
     */
    public static void cancelShadowConsumerTag(Channel target, String consumerTag) {
        Channel shadowChannel = getShadowChannel(target);
        if (shadowChannel == null || !shadowChannel.isOpen()) {
            LOGGER.warn("rabbitmq basicCancel failed. cause by shadow channel is not found or closed. consumerTag={}",
                consumerTag);
            return;
        }
        try {
            shadowChannel.basicCancel(consumerTag);
        } catch (IOException e) {
            LOGGER.error("rabbitmq basicCancel shadow consumer error. consumerTag={}", consumerTag, e);
        }
        removeConsumerTag(target, consumerTag);
    }

    /**
     * 移除 ChannelN对应的 ConsumerTag
     *
     * @param target
     * @param consumerTag
     */
    public static void removeConsumerTag(Channel target, String consumerTag) {
        int identityCode = System.identityHashCode(target);
        Channel shadowChannel = channelCache.get(identityCode);
        if (shadowChannel == null) {
            return;
        }
        int key = System.identityHashCode(shadowChannel);
        ConcurrentMap<String, Object> tags = consumerTags.get(key);
        if (tags == null) {
            return;
        }
        tags.remove(consumerTag);
        if (tags.isEmpty()) {
            consumerTags.remove(key);
        }
    }

    /**
     * 判断当前连接有没有此 consumerTag
     *
     * @param target
     * @param consumerTag
     * @return
     */
    public static boolean isExistsConsumerTag(ChannelN target, String consumerTag) {
        int identityCode = System.identityHashCode(target);
        Channel shadowChannel = channelCache.get(identityCode);
        if (shadowChannel == null) {
            return false;
        }
        int key = System.identityHashCode(shadowChannel);
        ConcurrentMap<String, Object> tags = consumerTags.get(key);
        if (tags == null) {
            return false;
        }
        return tags.containsKey(consumerTag);
    }

    /**
     * 检测 queue 是否存在，与业务 Channel 隔离开来
     * 如果获取Channel 失败，则直接返回 false
     *
     * @param target 业务 Channel
     * @param queue  检测的 Queue
     * @return 返回 queue 是否存在
     */
    public static boolean isQueueExists(Channel target, String queue) {
        Channel channel = getShadowChannel(target);
        if (channel == null) {
            return false;
        }
        try {
            channel.queueDeclarePassive(queue);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * 根据业务的 consumerTag 判断该 consumer 是否已经存在影子消费者
     *
     * @param consumerTag 业务 consumerTag
     * @return 返回是否已经存在影子 Consumer
     */
    public static boolean existsConsumer(String consumerTag) {
        return shadowTags.containsKey(consumerTag);
    }

    public static boolean isShadowChannel(Channel channel) {
        return channelCache.containsValue(channel);
    }

    public static boolean isShadowConnection(Connection connection) {
        return connectionCache.containsValue(connection);
    }

    /**
     * 获取影子 Channel
     *
     * @param target
     * @return
     */
    public static Channel getShadowChannel(Channel target) {
        if (target == null || !target.isOpen()) {
            return null;
        }
        int key = System.identityHashCode(target);
        Channel shadowChannel = channelCache.get(key);
        if (shadowChannel == null || !shadowChannel.isOpen()) {
            Channel channel;
            try {
                Connection ptConnect = getPtConnect(target.getConnection());
                channel = ptConnect.createChannel();
            } catch (Exception e) {
                LOGGER.warn("[RabbitMQ] can't create shadow channel from biz channel", e);
                return null;
            }
            Channel old = channelCache.put(key, channel);
            if (old != null && old.isOpen()) {
                try {
                    old.close();
                    LOGGER.warn("[RabbitMQ] has old shadow channel close!");
                } catch (Throwable e) {
                }
            }
            return channel;
        }
        return shadowChannel;
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
        connection = RabbitmqUtils.unWrapConnection(connection);
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
                    ThreadPoolExecutor te = (ThreadPoolExecutor)executorService;
                    int queueSize = getQueueSize(te.getQueue());
                    if (isInfoEnabled) {
                        LOGGER.info(
                            "[RabbitMQ] biz consumer use thread pool info : [coreSize : {}, maxSize : {}, keepAliveTime : "
                                + "{} "
                                + "queueSize : {}]",
                            te.getCorePoolSize(), te.getMaximumPoolSize(), te.getKeepAliveTime(TimeUnit.SECONDS), queueSize);
                    }
                    return new ThreadPoolInfo(te.getCorePoolSize(), te.getMaximumPoolSize(),
                        (int)te.getKeepAliveTime(TimeUnit.SECONDS), queueSize);
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
            return ((Object[])Reflect.on(queue).get("items")).length;
        }
        if (queue instanceof SynchronousQueue) {
            return 0;
        }
        LOGGER.warn("unknown BlockingQueue : {} will use unbound queue", queue.getClass().getName());
        return Integer.MAX_VALUE;
    }

    public static ConcurrentHashMap<String, List<Channel>> getQueueChannel() {
        return queueChannel;
    }

}
