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
import com.pamirs.attach.plugin.jedis.util.JedisConstant;
import com.pamirs.pradar.internal.config.ShadowRedisConfig;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.shulie.instrument.simulator.api.reflect.ReflectionUtils;
import com.shulie.instrument.simulator.api.util.StringUtil;
import redis.clients.jedis.BinaryJedis;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;


/**
 * Jedis 工厂
 *
 * @Author qianfan
 * @package: jedis 单机构造工厂
 * @Date 2020/11/27 4:34 下午
 */
public class JedisFactory extends AbstractRedisServerFactory<JedisPool> {

    private static JedisFactory jedisFactory;

    private JedisFactory() {
        super(new JedisMatchStrategy(new JedisNodesStrategy()));
    }

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

    public static void release() {
        JedisFactory.destroy();
        jedisFactory = null;
    }

    @Override
    public RedisClientMediator<JedisPool> createMediator(Object client, ShadowRedisConfig shadowConfig) {
        RedisClientMediator mediator;
        String className = client.getClass().getName();
        BinaryJedis shadowClient = null;
        Integer shadowDb = shadowConfig.getDatabase();
        /**
         * 反射是为了兼容没有getDb的版本
         * 向下转为Integer是为了兼容Long类型的入参
         */
        int bizDb = Integer.parseInt(String.valueOf(ReflectionUtils.getFieldValues(client,"client","db")));
        String bizPassword = String.valueOf(ReflectionUtils.getFieldValues(client,"client","password"));
        String shadowPassword = shadowConfig.getPassword(bizPassword);
        if (JedisConstant.JEDIS.equals(className)) {
            //单节点
            String nodes = shadowConfig.getNodes();
            if (!nodes.contains(":")) {
                shadowClient = new Jedis(nodes);
            } else {
                String[] splitter = nodes.split(":");
                shadowClient = new Jedis(splitter[0], Integer.parseInt(splitter[1]));
            }
            if (!StringUtil.isEmpty(shadowPassword)) {
                shadowClient.auth(shadowPassword);
            }

            if (shadowDb != null) {
                shadowClient.select(shadowDb);
            } else {
                //用业务的db
                shadowClient.select(bizDb);
            }


        } else if (JedisConstant.BINARY_JEDIS.equals(className)) {
            String nodes = shadowConfig.getNodes();
            if (!nodes.contains(":")) {
                shadowClient = new Jedis(nodes);
            } else {
                String[] splitter = nodes.split(":");
                shadowClient = new BinaryJedis(splitter[0], Integer.parseInt(splitter[1]));
            }
            if (shadowDb != null) {
                shadowClient.select(shadowDb);
            } else {
                //用业务的db
                shadowClient.select(bizDb);
            }

            if (!StringUtil.isEmpty(shadowPassword)) {
                shadowClient.auth(shadowPassword);
            }
        }
        return new RedisClientMediator(client, shadowClient);
    }

    @Override
    public void clearAll(IEvent event) {
        clear();
        RedisConstants.registerShadowNodes.clear();
    }


}
