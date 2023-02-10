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
import com.shulie.instrument.simulator.api.reflect.ReflectionUtils;
import com.shulie.instrument.simulator.api.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

import java.util.*;

/**
 * @Author qianfan
 * @package: jedis sentinel
 * @Date 2020/11/26 11:23 上午
 */
public class JedisSentinelNodesStrategy implements RedisServerNodesStrategy {

    private final static Logger LOGGER = LoggerFactory.getLogger(JedisSentinelNodesStrategy.class);

    @Override
    public List<String> match(Object obj) {
        List<String> nodes = new ArrayList<String>();
        String master = null;
        try {
            if (Jedis.class.isAssignableFrom(obj.getClass())) {
                Object datasource = ReflectionUtils.get(obj,"dataSource");
                if (datasource == null || !(datasource instanceof JedisSentinelPool)) {
                    return Collections.emptyList();
                }
                HashSet masterListeners = ReflectionUtils.get(datasource,"masterListeners");
                if (CollectionUtils.isEmpty(masterListeners)) {
                    return nodes;
                }
                //masterListeners是一个内部类(JedisSentinelPool$MasterListener)，不能直接访问，用反射访问
                Iterator iterator = masterListeners.iterator();

                while (iterator.hasNext()) {
                    Object next = iterator.next();
                    master = ReflectionUtils.get(next,"masterName");
                    String host = ReflectionUtils.get(next,"host");
                    String port = String.valueOf(ReflectionUtils.get(next,"port"));
                    nodes.add(host.concat(":").concat(port));
                }
            }
            nodes.add(master);
        } catch (Throwable e) {
            LOGGER.error("", e);
        }
        return nodes;
    }
}
