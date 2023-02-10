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
package com.pamirs.attach.plugin.jedis.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.pamirs.pradar.Throwables;
import com.pamirs.pradar.internal.config.ShadowRedisConfig;
import com.shulie.instrument.simulator.api.reflect.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.BinaryJedis;
import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

/**
 * @Auther: vernon
 * @Date: 2021/9/3 20:10
 * @Description:
 */
public class Model {

    static Model INSTANCE = new Model();

    Logger logger = LoggerFactory.getLogger(getClass());

    public static Model INSTANCE() {
        return INSTANCE;
    }

    static Set<String> pressureNotSingleModelCache = new HashSet<String>();
    static Set<String> clusterNoCache = new HashSet<String>();
    static Set<String> masterSlaveCache = new HashSet<String>();

    public void setClusterMode(String node) {
        if (clusterNoCache.contains(node)) {
            return;
        }
        clusterNoCache.add(node);
    }

    /**
     * 是否集群模式
     */

    public boolean isClusterMode(String name) {
        return clusterNoCache.contains(name);
    }

    /**
     * 是否主从模式
     *
     * @param key
     * @return
     */
    public boolean isMasterSlave(String key) {
        return masterSlaveCache.contains(key);
    }

    public void setMasterSlaveMode(String node) {
        if (masterSlaveCache.contains(node)) {
            return;
        }
        masterSlaveCache.add(node);
    }

    public void setMasterSlaveMode(String slave, String master) {
        setMasterSlaveMode(slave);
        setMasterSlaveMode(master);
        masterSlaveMapping.put(slave, master);
    }

    /**
     * master-> slave
     */
    static Map<String, String> masterSlaveMapping = new HashMap<String, String>();

    public String getMasterBySlave(String slave) {
        return masterSlaveMapping.get(slave);
    }

    /**
     * 是否哨兵模式
     *
     * @param jedis
     * @return
     */
    public boolean isSentinelMode(Object jedis) {
        if (Jedis.class.getName().equals(jedis.getClass().getName())) {
            try {
                Object dataSource = ReflectionUtils.get(jedis,"dataSource");
                if (dataSource == null) {
                    return false;
                }
                return JedisSentinelPool.class.isAssignableFrom(dataSource.getClass());
            } catch (Throwable t) {
                logger.error("[redis-jedis]: judge sentinel model error,{}", Throwables.getStackTraceAsString(t));
            }
        } else if (BinaryJedis.class.getName().equals(jedis.getClass().getName())) {
            return false;
        }
        return false;
    }

    /**
     * 是否单节点模式
     *
     * @param jedis
     * @return
     */
    public boolean isSingleMode(Object jedis) {
        if (isSentinelMode(jedis)) {
            return false;
        }
        Client client = ReflectionUtils.get(jedis,"client");
        String host = client.getHost();
        String port = String.valueOf(client.getPort());
        String node = host.concat(":").concat(String.valueOf(port));
        return !clusterNoCache.contains(node) && !pressureNotSingleModelCache.contains(node);

    }

    private static void addPressureNotSingleModelCache(String node) {
        if (pressureNotSingleModelCache.contains(node)) {
            return;
        }
        pressureNotSingleModelCache.add(node);
    }

    /**
     * 因为jedis比较特殊，最后都会转换为Jedis对象，预先缓存下压测节点信息，方便后续过滤
     *
     * @param shadowRedisConfig
     * @return
     */
    public void cachePressureNode(ShadowRedisConfig shadowRedisConfig) {
        /**
         * 不是单节点模式
         */

        //兼容历史数据
        if (!"single".equals(shadowRedisConfig.getModel()) || shadowRedisConfig.getMaster() != null
            || (shadowRedisConfig.getNodes() != null && (shadowRedisConfig.getNodes().contains(",") ||
            shadowRedisConfig.getNodes().contains(";")))) {
            for (String node : shadowRedisConfig.getNodes().split(",")) {
                addPressureNotSingleModelCache(node);
                addPressureNotSingleModelCache(shadowRedisConfig.getMaster());
            }
        }
    }
}


