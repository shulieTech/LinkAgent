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
package com.pamirs.attach.plugin.lettuce.utils;

import com.shulie.instrument.simulator.api.util.CollectionUtils;
import io.lettuce.core.RedisURI;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @Auther: vernon
 * @Date: 2021/10/19 15:50
 * @Description:
 */
public class LettuceUtils {

    static Set<RedisURI> masterSlaveCache = new HashSet<RedisURI>();

    static Set<String> pressureNodeCache = new HashSet<String>();


    public static void cachePressureNode(List<RedisURI> uris) {
        if (CollectionUtils.isEmpty(uris)) {
            return;
        }
        for (RedisURI uri : uris) {
            cachePressureNode(uri);
        }
    }

    public static void cachePressureNode(Object t) {
        String node = null;
        if (t == null) {
            return;
        }
        if (t instanceof String) {
            node = String.valueOf(t);
        }
        if (!pressureNodeCache.contains(node)) {
            pressureNodeCache.add(node);
        }
        if (t instanceof RedisURI) {
            RedisURI redisURI = (RedisURI) t;
            if (redisURI.getHost() != null) {
                pressureNodeCache.add(redisURI.getHost() + ":" + redisURI.getPort());
            }
            if (CollectionUtils.isNotEmpty(redisURI.getSentinels())) {
                for (RedisURI redisURI1 : redisURI.getSentinels()) {
                    cachePressureNode(redisURI1);
                }
            }
        }
    }

    public static boolean ignore(RedisURI uri) {
        return isPressureNode(uri) || isMasterSalve(uri);
    }

    static boolean isPressureNode(Object t) {
        if (t == null) {
            return false;
        }
        if (t instanceof String) {
            return pressureNodeCache.contains(t);
        } else if (t instanceof RedisURI) {
            RedisURI uri = (RedisURI) t;
            if (uri.getHost() != null) {
                return pressureNodeCache.contains(uri.getHost() + ":" + uri.getPort());
            }
        }
        return false;
    }

    public static boolean isMasterSalve(RedisURI uri) {
        return masterSlaveCache.contains(uri) || CollectionUtils.isNotEmpty(uri.getSentinels());
    }

    public static void cacheMasterSlave(RedisURI uri) {
        if (CollectionUtils.isEmpty(uri.getSentinels())) {
            if (!masterSlaveCache.contains(uri)) {
                masterSlaveCache.add(uri);
            }
        }
        for (RedisURI sentinel : uri.getSentinels()) {
            cacheMasterSlave(sentinel);
        }

    }

    public static void cacheMasterSlave(List<RedisURI> uriList) {
        for (RedisURI uri : uriList) {
            cacheMasterSlave(uri);
        }
    }


}
