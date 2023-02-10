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
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.attach.plugin.jedis.RedisConstants;
import com.pamirs.attach.plugin.jedis.util.Model;
import com.pamirs.pradar.internal.config.ShadowRedisConfig;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.shulie.instrument.simulator.api.util.StringUtil;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

import java.util.HashSet;
import java.util.Set;

/**
 * Jedis 工厂
 *
 * @Author qianfan
 * @package: com.pamirs.attach.plugin.jedis.redisserver
 * @Date 2020/11/27 4:34 下午
 */
public class JedisSentinelFactory extends AbstractRedisServerFactory<JedisSentinelPool> {

    private static JedisSentinelFactory jedisFactory;


    private JedisSentinelFactory() {
        super(new JedisSentinelNodesStrategy());
    }

    public static JedisSentinelFactory getFactory() {
        if (jedisFactory == null) {
            synchronized (JedisSentinelFactory.class) {
                if (jedisFactory == null) {
                    jedisFactory = new JedisSentinelFactory();
                }
            }
        }
        return jedisFactory;
    }

    @Override
    public <T> T security(T client) {
        return client;
    }

    public static void release() {
        JedisSentinelFactory.destroy();
        jedisFactory = null;
    }

    Model model = Model.INSTANCE();

    @Override
    public RedisClientMediator createMediator(Object obj, ShadowRedisConfig shadowConfig) {
        model.cachePressureNode(shadowConfig);
        RedisClientMediator mediator = null;
        if (Jedis.class.isAssignableFrom(obj.getClass())) {

            Object datasource = ReflectionUtils.get(obj,"dataSource");
            if (datasource != null && JedisSentinelPool.class.isAssignableFrom(datasource.getClass())) {
                JedisSentinelPool pressureJedisPool = null;

                String bizPassword = String.valueOf(ReflectionUtils.get(datasource,"password"));
                String masterName = shadowConfig.getMaster();
                Set nodes = new HashSet(shadowConfig.getNodeNums());
                String password = shadowConfig.getPassword(bizPassword);
                GenericObjectPoolConfig poolConfig = ReflectionUtils.get(datasource,"poolConfig");


                password = StringUtil.isEmpty(password) ? null : password;

                int database = shadowConfig.getDatabase() == null ? 0 : shadowConfig.getDatabase();

                pressureJedisPool = new JedisSentinelPool(masterName, nodes, poolConfig,
                        2000, password, database);

               /* else if (StringUtil.isEmpty(password)) {
                    pressureJedisPool = new JedisSentinelPool(masterName, nodes, poolConfig);

                } else {
                    pressureJedisPool = new JedisSentinelPool(masterName, nodes, poolConfig, password);
                }*/
                mediator = new RedisClientMediator<JedisSentinelPool>(pressureJedisPool, (JedisSentinelPool) datasource, true);
            }
        }
        return mediator;
    }

    @Override
    public void clearAll(IEvent event) {
        clear();
        RedisConstants.registerShadowNodes.clear();
    }

}
