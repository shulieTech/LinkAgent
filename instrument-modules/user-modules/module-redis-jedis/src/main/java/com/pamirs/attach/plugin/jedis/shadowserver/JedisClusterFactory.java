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
import com.pamirs.attach.plugin.jedis.util.Model;
import com.pamirs.pradar.internal.config.ShadowRedisConfig;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.util.StringUtil;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisSlotBasedConnectionHandler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JedisCluster 工厂
 *
 * @Author qianfan
 * @package: com.pamirs.attach.plugin.jedis.redisserver
 * @Date 2020/11/27 4:34 下午
 */
public class JedisClusterFactory extends AbstractRedisServerFactory<JedisSlotBasedConnectionHandler> {

    private static JedisClusterFactory jedisClusterFactory;

    private JedisClusterFactory() {
        super(new JedisClusterNodesStrategy());
    }

    public static JedisClusterFactory getFactory() {
        if (jedisClusterFactory == null) {
            synchronized (JedisClusterFactory.class) {
                if (jedisClusterFactory == null) {
                    jedisClusterFactory = new JedisClusterFactory();
                }
            }
        }
        return jedisClusterFactory;
    }

    public static void release() {
        JedisClusterFactory.destroy();
        jedisClusterFactory = null;
    }

    @Override
    public <T> T security(T client) {
        return client;
    }

    Model model = Model.INSTANCE();

    @Override
    public RedisClientMediator<JedisSlotBasedConnectionHandler> createMediator(Object connection, ShadowRedisConfig shadowConfig) {
        model.cachePressureNode(shadowConfig);
        RedisClientMediator<JedisSlotBasedConnectionHandler> mediator = null;
        if (connection instanceof JedisSlotBasedConnectionHandler) {
            Reflect reflect = Reflect.on(Reflect.on(connection).get("cache"));


            GenericObjectPoolConfig poolConfig = reflect.get("poolConfig");
            int connectionTimeout = reflect.get("connectionTimeout");
            int soTimeout = reflect.get("soTimeout");
            JedisSlotBasedConnectionHandler pressureJedisPool = null;
            String shadowPassword = shadowConfig.getPassword();
            if (!StringUtil.isEmpty(shadowPassword)) {
                pressureJedisPool = new JedisSlotBasedConnectionHandler(convert(shadowConfig.getNodeNums()),
                        poolConfig, connectionTimeout, soTimeout,
                        shadowConfig.getPassword());
            } else {
                pressureJedisPool = new JedisSlotBasedConnectionHandler(convert(shadowConfig.getNodeNums()),
                        poolConfig, connectionTimeout, soTimeout);
            }

            mediator = new RedisClientMediator<JedisSlotBasedConnectionHandler>(pressureJedisPool, (JedisSlotBasedConnectionHandler) connection, true);
        }
        return mediator;
    }


    @Override
    public void clearAll(IEvent event) {
        clear();
        RedisConstants.registerShadowNodes.clear();
    }

    public Set<HostAndPort> convert(List<String> nodes) {
        Set<HostAndPort> hostAndPorts = new HashSet<HostAndPort>();
        for (String node : nodes) {
            final int endIndex = node.indexOf(":");
            String host = node.substring(0, endIndex);
            int port = Integer.parseInt(node.substring(endIndex + 1));
            HostAndPort hostAndPort = new HostAndPort(host, port);
            hostAndPorts.add(hostAndPort);
        }
        return hostAndPorts;
    }
}
