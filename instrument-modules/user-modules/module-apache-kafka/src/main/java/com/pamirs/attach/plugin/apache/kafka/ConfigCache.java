/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.apache.kafka;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/10/27 7:23 下午
 */
public final class ConfigCache {
    private static Map<Integer, String> servers = new HashMap<Integer, String>();
    private static Map<Integer, String> groups = new HashMap<Integer, String>();
    private static ConcurrentMap<Integer, Boolean> isInited = new ConcurrentHashMap<Integer, Boolean>();


    public static void release() {
        servers.clear();
        groups.clear();
        isInited.clear();
    }

    public static boolean isInited(Object target) {
        int code = System.identityHashCode(target);
        return isInited.get(code) != null && isInited.get(code);
    }

    public static void setInited(Object target) {
        int code = System.identityHashCode(target);
        isInited.put(code, true);

    }

    static Map<Thread, Boolean> cache = new ConcurrentHashMap();

    public static boolean isBiz() {
        /**
         *
         */
        if (cache.get(Thread.currentThread()) != null) {
            return cache.get(Thread.currentThread());
        } else {
            if (cache.isEmpty()) {
                cache.put(Thread.currentThread(), Boolean.TRUE);
                return Boolean.TRUE;
            } else {
                int size = cache.size();
                if (size % 2 == 1) {
                    //下一个为压测
                    cache.put(Thread.currentThread(), Boolean.FALSE);
                    return Boolean.FALSE;
                } else {
                    cache.put(Thread.currentThread(), Boolean.TRUE);
                    return Boolean.TRUE;
                }
            }
        }
    }


    public static ConcurrentMap<Integer, Boolean> getIsInited() {
        return isInited;
    }

    public static void setServers(Object target, String server) {
        servers.put(System.identityHashCode(target), server);
    }

    public static String getServers(Object target) {
        return servers.get(System.identityHashCode(target));
    }

    public static void setGroup(Object target, String group) {
        groups.put(System.identityHashCode(target), group);
    }

    public static String getGroup(Object target) {
        return groups.get(System.identityHashCode(target));
    }
}
