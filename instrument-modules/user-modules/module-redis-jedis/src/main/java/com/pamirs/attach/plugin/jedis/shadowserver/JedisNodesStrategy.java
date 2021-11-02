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

import com.pamirs.attach.plugin.common.datasource.redisserver.RedisServerNodesStrategy;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.BinaryJedis;
import redis.clients.jedis.Client;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author qianfan
 * @package: jedis单机模式matcher
 * @Date 2020/11/26 11:23 上午
 */
public class JedisNodesStrategy implements RedisServerNodesStrategy {

    private final static Logger LOGGER = LoggerFactory.getLogger(JedisNodesStrategy.class);

    /**
     * obj是redis.clients.jedis.Jedis或者redis.clients.jedis.BinaryJedis
     *
     * @param obj 任意对象
     * @return
     */
    @Override
    public List<String> match(Object obj) {
        if (!BinaryJedis.class.isAssignableFrom(obj.getClass())) {
            LOGGER.info("not support type of jedis single mode , class is not" +
                    " redis.clients.jedis.Jedis or redis.clients.jedis.BinaryJedis");
            return new ArrayList<String>();
        }
        List<String> nodes = new ArrayList<String>();
        try {
            Reflect reflect = Reflect.on(obj);
            Client client = reflect.get("client");
            Reflect clientRef = Reflect.on(client);
            String password = clientRef.get("password");
            String host = clientRef.get("host");
            String port = String.valueOf(clientRef.get("port"));
            String db = String.valueOf(clientRef.get("db"));
            nodes.add(getKey(host, port));

        } catch (Throwable e) {
            LOGGER.error("", e);
        }
        return nodes;
    }

    public String getKey(String host, String port) {
        return host.concat(":").concat(port);
    }


    public Integer getDb(Object obj) {
        if (!BinaryJedis.class.isAssignableFrom(obj.getClass())) {
            return null;
        }
        try {
            Reflect reflect = Reflect.on(obj);
            Client client = reflect.get("client");
            Reflect clientRef = Reflect.on(client);
            return clientRef.get("db");

        } catch (Throwable e) {
            LOGGER.error("", e);
        }
        return null;
    }
}
