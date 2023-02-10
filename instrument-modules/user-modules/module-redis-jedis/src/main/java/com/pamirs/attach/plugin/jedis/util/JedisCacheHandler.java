/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pamirs.attach.plugin.jedis.util;

import com.pamirs.attach.plugin.dynamic.Attachment;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.attach.plugin.dynamic.template.RedisTemplate;
import com.pamirs.attach.plugin.jedis.RedisConstants;
import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description jedis反射缓存类
 * @Author ocean_wll
 * @Date 2022/6/28 4:28 下午
 */
public class JedisCacheHandler {

    private final static Integer INIT_CAPACITY = 128;

    private final static Integer LOAD_FACTOR = 1;

    public final static Map<Jedis, Client> JedisClientCache = new ConcurrentHashMap<Jedis, Client>(INIT_CAPACITY, LOAD_FACTOR);

    public static Client getClientByJedis(Jedis jedis) {
        Client client = JedisClientCache.get(jedis);
        if (client == null) {
            client = ReflectionUtils.get(jedis, "client");
            putData(JedisClientCache, jedis, client);
        }
        return client;
    }


    public final static Map<Jedis, Object> JedisDbCache = new ConcurrentHashMap<Jedis, Object>(INIT_CAPACITY, LOAD_FACTOR);

    public static Object getDbByJedis(Jedis jedis) {
        Object db = JedisDbCache.get(jedis);
        if (db == null) {
            db = ReflectionUtils.get(jedis, "dataSource");
            putData(JedisDbCache, jedis, db);
        }
        return db;
    }


    public final static Map<JedisSentinelPool, Attachment> SentinelAttachmentCache = new ConcurrentHashMap<JedisSentinelPool, Attachment>(INIT_CAPACITY, LOAD_FACTOR);

    public static Attachment getSentinelAttachment(JedisSentinelPool jedisSentinelPool) {
        Attachment ext = SentinelAttachmentCache.get(jedisSentinelPool);
        if (ext == null) {
            String password = ReflectionUtils.get(jedisSentinelPool, "password");
            Integer database = ReflectionUtils.get(jedisSentinelPool, "database");
            Set set = ReflectionUtils.get(jedisSentinelPool, "masterListeners");
            Iterator iterator = set.iterator();
            StringBuilder nodeBuilder = new StringBuilder();
            String masterName = null;
            while (iterator.hasNext()) {
                Object t = iterator.next();
                masterName = ReflectionUtils.get(t, "masterName");
                String host = ReflectionUtils.get(t, "host");
                String port = String.valueOf(ReflectionUtils.get(t, "port"));
                nodeBuilder.append(host.concat(":").concat(port))
                        .append(",");

            }
            ext = new Attachment(
                    null, RedisConstants.PLUGIN_NAME, new String[]{RedisConstants.MIDDLEWARE_NAME},
                    new RedisTemplate.JedisSentinelTemplate()
                            .setMaster(masterName)
                            .setNodes(nodeBuilder.deleteCharAt(nodeBuilder.length() - 1).toString())
                            .setDatabase(database)
                            .setPassword(password));

            putData(SentinelAttachmentCache, jedisSentinelPool, ext);
        }

        return ext;
    }


    public final static Map<Client, Attachment> SingleAttachmentCache = new ConcurrentHashMap<Client, Attachment>(INIT_CAPACITY, LOAD_FACTOR);

    public static Attachment getSingleAttachment(Client client) {
        Attachment ext = SingleAttachmentCache.get(client);

        if (ext == null) {
            String password = ReflectionUtils.get(client, "password");
            int db = Integer.parseInt(String.valueOf(ReflectionUtils.get(client, "db")));
            String node = client.getHost().concat(":").concat(String.valueOf(client.getPort()));
            ext = new Attachment(node, RedisConstants.PLUGIN_NAME,
                    new String[]{RedisConstants.MIDDLEWARE_NAME}
                    , new RedisTemplate.JedisSingleTemplate()
                    .setNodes(node)
                    .setPassword(password)
                    .setDatabase(db)
            );

            putData(SingleAttachmentCache, client, ext);
        }
        return ext;
    }

    private static void putData(Map map, Object key, Object value) {
        // 防止内存溢出
        if (map.size() >= INIT_CAPACITY) {
            map.clear();
        }
        map.put(key, value);
    }
}
