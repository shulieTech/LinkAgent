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
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.shulie.instrument.simulator.api.ThrowableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @Author qianfan
 * @package: jedis cluster matcher
 * @Date 2020/11/26 11:23 上午
 */
public class JedisClusterNodesStrategy implements RedisServerNodesStrategy {

    private final static Logger LOGGER = LoggerFactory.getLogger(JedisClusterNodesStrategy.class);

    @Override
    public List<String> match(Object obj) {
        List<String> nodes = new ArrayList<String>();
        try {
            HashMap nodeMap = ReflectionUtils.getFieldValues(obj,"cache","nodes");
            if (nodeMap == null) {
                return new ArrayList<String>();
            }
            Iterator iterator = nodeMap.keySet().iterator();
            while (iterator.hasNext()) {
                String node = (String) iterator.next();
                nodes.add(node);
            }
        } catch (Throwable e) {
            LOGGER.error("", ThrowableUtils.toString(e));
        }
        return nodes;
    }
}
