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
package com.pamirs.attach.plugin.lettuce.shadowserver;

import com.pamirs.attach.plugin.common.datasource.redisserver.AbstractRedisServerFactory;
import com.pamirs.attach.plugin.common.datasource.redisserver.RedisClientMediator;
import com.pamirs.attach.plugin.lettuce.LettuceConstants;
import com.pamirs.attach.plugin.lettuce.utils.LettuceUtils;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.internal.config.ShadowRedisConfig;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.util.CollectionUtils;
import com.shulie.instrument.simulator.api.util.StringUtil;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.masterslave.MasterSlave;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author qianfan
 * @package: com.pamirs.attach.plugin.lettuce.factory
 * @Date 2020/11/26 11:26 上午
 */
public class LettuceMasterSlaveFactory extends AbstractRedisServerFactory<Object> {

    private final static Logger LOGGER = LoggerFactory.getLogger(LettuceMasterSlaveFactory.class);

    private static LettuceMasterSlaveFactory factory;

    private LettuceMasterSlaveFactory() {
        super(new LettuceMasterStrategy(manager));
    }

    public static LettuceMasterSlaveFactory getFactory() {
        if (factory == null) {
            synchronized (LettuceMasterSlaveFactory.class) {
                if (factory == null) {
                    factory = new LettuceMasterSlaveFactory();
                }
            }
        }
        return factory;
    }

    public static void release() {
        LettuceMasterSlaveFactory.destroy();
        factory = null;
    }

    @Override
    public <T> T getClient(T client) {
        if (!GlobalConfig.getInstance().isShadowDbRedisServer()) {
            return client;
        }
        if (!doBefore()) {
            return client;
        }

        RedisClientMediator<T> mediator = getMediator(client);
        if (mediator == null) {
            // 抛出相关异常信息
            throw new PressureMeasureError(" get redis shadow server error.");
        }
        return security(mediator.getClient());
    }

    /**
     * 给masterSlave返回连接
     *
     * @param advice
     * @return
     */

    static public Map<Object, Object> pressureConnectionCache = new ConcurrentHashMap<Object, Object>();


    public Object getClient(Advice advice) {
        if (!GlobalConfig.getInstance().isShadowDbRedisServer()) {
            return advice.getReturnObj();
        }
        if (!doBefore()) {
            return advice.getReturnObj();
        }

        RedisClientMediator mediator = getMediator(advice);
        pressureConnectionCache.put(mediator.getPerformanceRedisClient(), advice.getReturnObj());
        if (mediator == null) {
            // 抛出相关异常信息
            throw new PressureMeasureError(" get redis shadow server error.");
        }
        return security(mediator.getClient());
    }

    protected RedisClientMediator getMediator(Advice advice) {
        RedisClientMediator<?> redisClientMediator = getMediators().get(advice.getReturnObj());
        if (redisClientMediator == null) {
            synchronized (monitLock) {
                redisClientMediator = getMediators().get(advice.getReturnObj());
                if (redisClientMediator == null) {
                    return create(advice);
                }
            }
        }
        return redisClientMediator;
    }

    private RedisClientMediator<?> create(Advice advice) {
        ShadowRedisConfig shadowRedisConfig = serverMatch.getConfig(advice);
        if (null == shadowRedisConfig) {
            ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.RedisServer)
                    .setErrorCode("redisServer-0001")
                    .setMessage("没有配置影子Redis Server")
                    .setDetail("没有配置影子Redis Server")
                    .report();
            // 抛出相关异常信息
            throw new PressureMeasureError("not found redis shadow server config error.");
        }


        validationConfig(shadowRedisConfig);

        RedisClientMediator mediator = createMediator(advice, shadowRedisConfig);
        putMediator(advice.getReturnObj(), mediator);
        return mediator;
    }

    private void validationConfig(ShadowRedisConfig shadowRedisConfig) {
        if (null == shadowRedisConfig.getNodes()) {
            throw new PressureMeasureError("shadow redis nodes is null.");
        }

        String[] nodes = StringUtils.split(shadowRedisConfig.getNodes(), ',');

        if (nodes.length < 1) {
            throw new PressureMeasureError("shadow redis nodes size Less than 1.");
        }

        StringBuilder builder = new StringBuilder();
        for (String node : nodes) {
            int index = node.indexOf("redis://");
            if (index != -1) {
                node = node.substring(index + 8);
            }
            if (!node.contains(":")) {
                builder.append(node).append("格式错误，不包含\":\"\\n");
            } else {
                if (node.length() < 3) {
                    builder.append(node).append("格式错误长度小于3\n");
                } else {
                    String host = node.substring(0, node.indexOf(":"));
                    String[] split = StringUtils.split(host, '.');
                    if (split.length != 4) {
                        builder.append(node).append("ip格式错误\n");
                    }
                    try {
                        int port = Integer.parseInt(node.substring(node.indexOf(":") + 1));
                        if (port < 0) {
                            builder.append(node).append("port格式错误, 小于0\n");
                        }
                    } catch (Throwable e) {
                        builder.append(node).append("port格式错误\n");
                    }
                }
            }
        }

        if (builder.length() > 4) {
            ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.RedisServer)
                    .setErrorCode("redisServer-0002")
                    .setMessage("影子Redis Server配置格式错误")
                    .setDetail(builder.toString())
                    .report();
            // 抛出相关异常信息
            if (GlobalConfig.getInstance().isShadowDbRedisServer()) {
                throw new PressureMeasureError(builder.toString());
            }
        }

        shadowRedisConfig.setNodeNums(Arrays.asList(nodes));
    }


    @Override
    public <T> T security(T client) {
        return client;
    }

    @Override
    public RedisClientMediator<Object> createMediator(Object obj, ShadowRedisConfig shadowRedisConfig) {
        if (!(obj instanceof Advice)) {
            return null;
        }
        boolean isSentinel = false;

        boolean withDataBase = shadowRedisConfig.getDatabase() != null;
        Integer pressureDatabase = shadowRedisConfig.getDatabase();


        Advice advice = (Advice) obj;
        RedisClientMediator<Object> mediator = null;
        try {
            String method = advice.getBehavior().getName();
            Object[] args = advice.getParameterArray();
            Object performanceConnection = null;

            List<RedisURI> performanceUris = new ArrayList<RedisURI>();
            RedisURI performanceUri = null;
            if (args[2] instanceof RedisURI) {
                isSentinel = isSentinel(args[2], shadowRedisConfig);
                String nodes = shadowRedisConfig.getNodes();

                if (isSentinel) {
                    String[] nodesSplitter = nodes.split(",");
                    RedisURI.Builder builder = RedisURI.builder();
                    for (String node : nodesSplitter) {
                        String[] innerSplitter = node.split(":");
                        builder.withSentinel(innerSplitter[0], Integer.parseInt(innerSplitter[1]));
                        if (withDataBase) {
                            builder.withDatabase(pressureDatabase);
                        }
                    }
                    builder.withSentinelMasterId(shadowRedisConfig.getMaster());
                    performanceUri = builder.build();
                } else {
                    String[] nodesSplitter = nodes.split(",");
                    for (String node : nodesSplitter) {
                        node = node.startsWith("redis://") ? node : "redis://" + node;
                        RedisURI uri = RedisURI.create(node);
                        if (withDataBase) {
                            uri.setDatabase(pressureDatabase);
                        }
                        performanceUris.add(uri);
                    }
                }


            } else if (args[2] instanceof List) {
                isSentinel = isSentinel(args[2], shadowRedisConfig);
                String nodes = shadowRedisConfig.getNodes();

                if (isSentinel) {
                    String[] nodesSplitter = nodes.split(",");
                    RedisURI.Builder builder = RedisURI.builder();
                    for (String node : nodesSplitter) {
                        builder.withSentinel(node);
                        if (withDataBase) {
                            builder.withDatabase(pressureDatabase);
                        }
                    }
                    builder.withSentinelMasterId(shadowRedisConfig.getMaster());
                    performanceUri = builder.build();
                } else {
                    String[] nodesSplitter = nodes.split(",");
                    for (String node : nodesSplitter) {
                        node = node.startsWith("redis://") ? node : "redis://" + node;
                        RedisURI uri = RedisURI.create(node);
                        if (withDataBase) {
                            uri.setDatabase(pressureDatabase);
                        }
                        performanceUris.add(uri);
                    }
                    String shadowMaster = shadowRedisConfig.getMaster();
                    if (!StringUtil.isEmpty(shadowMaster)) {
                        shadowMaster = shadowMaster.startsWith("redis://")
                                ? shadowMaster : "redis://" + shadowMaster;
                        RedisURI uri = RedisURI.create(shadowMaster);
                        if (withDataBase) {
                            uri.setDatabase(pressureDatabase);
                        }
                        performanceUris.add(uri);
                    }
                }
            } else {
               /* performanceUris.add(RedisURI.create(
                        shadowRedisConfig.getMaster().startsWith("redis://") ? shadowRedisConfig.getMaster() : "redis://" + shadowRedisConfig.getMaster())
                );
                for (String nodeNum : shadowRedisConfig.getNodeNums()) {
                    performanceUris.add(RedisURI.create("redis://" + nodeNum));
                }*/
            }
            LettuceUtils.cachePressureNode(performanceUri);
            LettuceUtils.cachePressureNode(performanceUris);


            if ("connect".equals(method)) {
                if (isRedisUri(args[2], performanceUri)) {
                    performanceConnection = MasterSlave.connect((RedisClient) args[0], (RedisCodec) args[1], performanceUri);
                } else {
                    performanceConnection = MasterSlave.connect((RedisClient) args[0], (RedisCodec) args[1], performanceUris);
                }
            } else if ("connectAsync".equals(method)) {
                if (isRedisUri(args[2], performanceUri)) {
                    performanceConnection = MasterSlave.connectAsync((RedisClient) args[0], (RedisCodec) args[1], performanceUri);
                } else {
                    performanceConnection = MasterSlave.connectAsync((RedisClient) args[0], (RedisCodec) args[1], performanceUris);
                }
            }
            mediator = new RedisClientMediator<Object>(advice.getReturnObj(), performanceConnection);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.RedisServer)
                    .setErrorCode("redisServer-0001")
                    .setMessage("redis server lettuce master slave error！")
                    .setDetail(ExceptionUtils.getStackTrace(e))
                    .report();
        }
        return mediator;
    }

    @Override
    public void clearAll(IEvent event) {
        try {
            clear();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public boolean isRedisUri(Object uri, Object performanceUri) {
        if (uri instanceof RedisURI && null != performanceUri) {
            return true;
        }
        return false;
    }

    public HostAndPort convert(String hostAndPort) {
        int index = hostAndPort.indexOf(":");
        String host = hostAndPort.substring(0, index);
        int port = Integer.parseInt(hostAndPort.substring(index + 1));
        return HostAndPort.of(host, port);
    }

    boolean isSentinel(Object t, ShadowRedisConfig shadowRedisConfig) {
        boolean isSentinel = false;
        if (shadowRedisConfig.getModel() != null) {
            isSentinel = "sentinel".equals(shadowRedisConfig.getModel());
        } else if (t instanceof RedisURI) {
            RedisURI redisURI = (RedisURI) t;
            isSentinel = CollectionUtils.isNotEmpty(redisURI.getSentinels()) || redisURI.getSentinelMasterId() != null;
        } else if (t instanceof List) {
            for (Object node : (List) t) {
                if (node instanceof RedisURI) {
                    if (CollectionUtils.isNotEmpty(((RedisURI) node).getSentinels())
                            || ((RedisURI) node).getSentinelMasterId() != null) {
                        isSentinel = true;
                    }
                }
            }
        }
        return isSentinel;
    }
}
