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
package com.pamirs.attach.plugin.jedis.shadowserver;

import com.pamirs.attach.plugin.common.datasource.redisserver.AbstractRedisServerFactory;
import com.pamirs.attach.plugin.common.datasource.redisserver.RedisClientMediator;
import com.pamirs.attach.plugin.jedis.RedisConstants;
import com.pamirs.attach.plugin.jedis.util.JedisConstructorConfig;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.internal.config.ShadowRedisConfig;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.BinaryJedis;
import redis.clients.jedis.Client;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Jedis 工厂
 *
 * @Author qianfan
 * @package: com.pamirs.attach.plugin.jedis.redisserver
 * @Date 2020/11/27 4:34 下午
 */
public class JedisFactory extends AbstractRedisServerFactory<JedisPool> {
    private final static Logger LOGGER = LoggerFactory.getLogger(JedisFactory.class);

    private static JedisFactory jedisFactory;

    private final Map<Object, Object> connectionMap = new ConcurrentHashMap<Object, Object>();
    private final List<Object> jedisPools = new ArrayList<Object>();

    protected static DynamicFieldManager manager;

    private JedisFactory() {
        super(new JedisMatchStrategy(new JedisNodesStrategy()));
    }

    private final Map<String, String> configMaps = new HashMap<String, String>();

    public static JedisFactory getFactory() {
        if (jedisFactory == null) {
            synchronized (JedisFactory.class) {
                if (jedisFactory == null) {
                    jedisFactory = new JedisFactory();
                }
            }
        }
        return jedisFactory;
    }

    @Override
    public <T> T security(T client) {
        return client;
    }
    public Object getClient(Object target, String method, Object[] args, Object result) {
        Jedis jedisShadow;
        String host=null;
        Integer port = null;
        if (Jedis.class.isAssignableFrom(target.getClass())) {
            Jedis jedis = (Jedis)target;
            Client client = Reflect.on(jedis).get("client");
            host = client.getHost();
            port = client.getPort();
            //jedisShadow = new Jedis(host, port);

        } else if (target.getClass().getName().equals("redis.clients.jedis.BinaryJedis")) {
            BinaryJedis binaryJedis = (BinaryJedis)target;
            Client client = Reflect.on(binaryJedis).get("client");
            host = client.getHost();
            port = client.getPort();
            //jedisShadow = new Jedis(host, port);
        }
        JedisConstructorConfig config = new JedisConstructorConfig();
        HostAndPort hostAndPort=new HostAndPort(host,port);
        config.setNode(hostAndPort);
        RedisConstants.jedisInstance.put(target, config);
        RedisClientMediator<?> redisClientMediator = getMediators().get(target);
        if (redisClientMediator == null) {
            synchronized (monitLock) {
                redisClientMediator = getMediators().get(target);
                if (redisClientMediator == null) {
                    ShadowRedisConfig shadowRedisConfig = serverMatch.getConfig(target);
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

                    RedisClientMediator<Jedis> mediator = null;
                    JedisConstructorConfig jedisConfig = RedisConstants.jedisInstance.get(target);
                    if (jedisConfig == null) {
                        return null;
                    }
                   jedisShadow= new Jedis(host, port,5000, 5000);
                    jedisShadow.auth(shadowRedisConfig.getPassword());
                    jedisShadow.select(shadowRedisConfig.getDatabase());
                    mediator = new RedisClientMediator<Jedis>(jedisShadow, (Jedis) target, true);
                    putMediator(target, mediator);
                    return mediator.getPerformanceRedisClient();
                }
            }
        }else{
            return redisClientMediator.getPerformanceRedisClient();
        }



        return target;
    }

    /**
     * todo先从父类移植过来后期整改
     * @param shadowRedisConfig
     */
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
    public static void release() {
        JedisFactory.destroy();
        jedisFactory = null;
    }

    @Override
    public RedisClientMediator<JedisPool> createMediator(Object client, ShadowRedisConfig shadowConfig) {
        RedisClientMediator<JedisPool> mediator = null;
        if (client instanceof JedisPool) {
            JedisConstructorConfig jedisConfig = RedisConstants.jedisInstance.get(client);
            if (jedisConfig == null) {
                return null;
            }

            // 兼容主从模式
            HostAndPort next = null;
            if (StringUtils.isNotBlank(shadowConfig.getMaster())) {
                next = electionMaster(jedisConfig, shadowConfig);
                if (next == null) {
                    next = next(shadowConfig.getNodeNums());
                }
            } else {
                next = next(shadowConfig.getNodeNums());
            }

            if (next == null) {
                return null;
            }


            JedisPool pressureJedisPool = null;
            if (1 == jedisConfig.getConstructorType()) {
                //JedisPool(final String host) 1
                pressureJedisPool = new JedisPool(next.getHost());
            } else if (2 == jedisConfig.getConstructorType()) {
                // 4
                //JedisPool(final String host, final SSLSocketFactory sslSocketFactory,final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier)
                pressureJedisPool = new JedisPool(next.getHost(), jedisConfig.getSslSocketFactory(), jedisConfig.getSslParameters(), jedisConfig.getHostnameVerifier());
            } else if (3 == jedisConfig.getConstructorType()) {
                // 8
                //JedisPool(final GenericObjectPoolConfig poolConfig,
                //final String host, int port,final int connectionTimeout,
                //final int soTimeout, final String password, final int database,final String clientName)
                pressureJedisPool = new JedisPool(jedisConfig.getPoolConfig(), next.getHost(), next.getPort(), jedisConfig.getConnectionTimeout(), jedisConfig.getSoTimeout(), shadowConfig.getPassword(), jedisConfig.getDatabase(), jedisConfig.getClientName());
            } else if (4 == jedisConfig.getConstructorType()) {
                // 12
                //JedisPool(final GenericObjectPoolConfig poolConfig, final String host,
                //int port,final int connectionTimeout, final int soTimeout,
                //final String password, final int database,final String clientName,
                //final boolean ssl, final SSLSocketFactory sslSocketFactory,
                //final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier)
                pressureJedisPool = new JedisPool(jedisConfig.getPoolConfig(), next.getHost(), next.getPort(), jedisConfig.getConnectionTimeout(), jedisConfig.getSoTimeout(), shadowConfig.getPassword(), jedisConfig.getDatabase(), jedisConfig.getClientName(), jedisConfig.isSsl(), jedisConfig.getSslSocketFactory(), jedisConfig.getSslParameters(), jedisConfig.getHostnameVerifier());
            } else if (5 == jedisConfig.getConstructorType()) {
                // 4
                //JedisPool(final GenericObjectPoolConfig poolConfig, final URI uri,final int connectionTimeout, final int soTimeout)
                pressureJedisPool = new JedisPool(jedisConfig.getPoolConfig(), jedisConfig.getUri(), jedisConfig.getConnectionTimeout(), jedisConfig.getSoTimeout());
            } else if (6 == jedisConfig.getConstructorType()) {
                // 7
                //JedisPool(final GenericObjectPoolConfig poolConfig, final URI uri,
                //final int connectionTimeout, final int soTimeout, final SSLSocketFactory sslSocketFactory,
                //final SSLParameters sslParameters, final HostnameVerifier hostnameVerifier)
                pressureJedisPool = new JedisPool(jedisConfig.getPoolConfig(), jedisConfig.getUri(), jedisConfig.getConnectionTimeout(), jedisConfig.getSoTimeout(), jedisConfig.getSslSocketFactory(), jedisConfig.getSslParameters(), jedisConfig.getHostnameVerifier());
            }
            if (null != pressureJedisPool) {
                if (StringUtils.isNotBlank(shadowConfig.getMaster())) {
                    for (String nodeNum : shadowConfig.getNodeNums()) {
                        configMaps.put(nodeNum, shadowConfig.getMaster());
                    }
                }
                mediator = new RedisClientMediator<JedisPool>(pressureJedisPool, (JedisPool) client, true);
            }
        }
        return mediator;
    }

    public HostAndPort electionMaster(JedisConstructorConfig jedisConfig, ShadowRedisConfig shadowConfig) {
        for (Map.Entry<String, ShadowRedisConfig> entry : GlobalConfig.getInstance().getShadowRedisConfigs().entrySet()) {
            if (entry.getValue() == shadowConfig) {
                String configKey = entry.getKey();
                List<String> configKeys = configKey.contains(",")
                        ? Arrays.asList(StringUtils.split(configKey, ','))
                        : Collections.singletonList(configKey);
                if (configKeys.get(0).equals(getKey(jedisConfig))) {
                    return createNode(shadowConfig.getMaster());
                }
            }
        }
        return null;
    }

    public HostAndPort getMaster(String key) {
        String value = configMaps.get(key);
        if (value == null) {
            return null;
        }
        return createNode(value);
    }


    @Override
    public void clearAll(IEvent event) {
        clear();
        RedisConstants.registerShadowNodes.clear();
    }

    private AtomicInteger index = new AtomicInteger(0);

    private HostAndPort next(List<String> nodes) {
        for (String node : nodes) {
            if (!RedisConstants.registerShadowNodes.contains(node)) {
                return createNode(node);
            }
        }
        // TODO 该怎么做？有可能会遇到业务IP * 3,影子IP * 2的情况
        // 目前轮训选取
        return createNode(nodes.get(index.getAndIncrement() % nodes.size()));
    }

    public HostAndPort createNode(String node) {
        final int endIndex = node.indexOf(":");
        String host = node.substring(0, endIndex);
        int port = Integer.parseInt(node.substring(endIndex + 1));
        RedisConstants.registerShadowNodes.add(node);
        return new HostAndPort(host, port);
    }

    public String getKey(JedisConstructorConfig jedisConfig) {
        return jedisConfig.getHost() + ":" + jedisConfig.getPort();
    }

}
