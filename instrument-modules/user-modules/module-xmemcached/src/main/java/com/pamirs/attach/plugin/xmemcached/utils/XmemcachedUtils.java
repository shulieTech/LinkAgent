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
package com.pamirs.attach.plugin.xmemcached.utils;

import com.pamirs.pradar.Pradar;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author vincent
 */
public final class XmemcachedUtils {

    private XmemcachedUtils() { /* no instance */ }

    /**
     * redis 的 server 目前有 redis、 codis、 pika
     * redis 的 client 目前有 jedis、 redission、 lettuce
     * client 访问的接口大部分相同，小部分有差异。这个对象保存所有 client 的方法合集。
     */
    private final static Map<String, String> METHOD_NAMES = new HashMap<String, String>();

    public static final ConcurrentHashMap<String, String> HOST_SERVER_TYPE_MAP = new ConcurrentHashMap<String, String>();

    private final static int KEY_LENGTH_LIMITED = 200;

    public static final String REDIS_SERVER_REDIS = "Redis";

    public static final String REDIS_SERVER_PIKA = "Pika";

    public static final String REDIS_SERVER_CODIS = "Codis";

    public static final String REDIS_CLIENT_JEDIS = "Jedis";

    public static final String REDIS_CLIENT_LETTUCE = "Lettuce";

    public static final String REDIS_CLIENT_REDISSION = "Redisson";

    public static final String READ = "R";

    public static final String WRITE = "W";

    static {
        METHOD_NAMES.put("touch", WRITE);
        METHOD_NAMES.put("set", WRITE);
        METHOD_NAMES.put("add", WRITE);
        METHOD_NAMES.put("get", READ);
        METHOD_NAMES.put("deleteWithNoReply", WRITE);
    }

    public static Map<String, String> get() {
        return Collections.unmodifiableMap(METHOD_NAMES);
    }

    public static boolean isReadMethod(String methodName) {
        return READ.equals(METHOD_NAMES.get(methodName));
    }

    public static boolean isWriteMethod(String methodName) {
        return WRITE.equals(METHOD_NAMES.get(methodName));
    }

    public static String getMethodNameExt(Object... args) {
        if (args == null || args.length == 0) {
            return "";
        }
        return toString(KEY_LENGTH_LIMITED, args[0]);
    }

    public static String toString(int limitSize, Object... args) {
        if (args == null || args.length == 0 || limitSize < 1) {
            return StringUtils.EMPTY;
        }
        StringBuilder builder = new StringBuilder();
        for (Object obj : args) {
            if (obj == null) {
                continue;
            }
            builder.append(',');
            if (obj instanceof byte[]) {
                builder.append(new String((byte[]) obj));
            } else if (obj instanceof char[]) {
                builder.append(new String((char[]) obj));
            } else if (obj.getClass().isPrimitive()) {
                builder.append(String.valueOf(obj));
            } else {
                builder.append(obj.toString());
            }
        }
        if (builder.length() != 0) {
            builder.deleteCharAt(0);
        }
        String argsStr = builder.toString();
        if (builder.length() > 0) {
            for (String c : Pradar.SPECIAL_CHARACTORS) {
                argsStr = argsStr.replace(c, " ");
            }
            if (argsStr.length() > limitSize) {
                argsStr = argsStr.substring(0, limitSize);
            }
        }
        return argsStr;
    }

    public static String remoteIpStr(String host, Integer port) {
        if (host == null || host.trim().length() == 0) {
            return "Unknown Host";
        }
        if (port == null) {
            port = 80;
        }
        return host + ":" + port;
    }


    public static String methodStr(String methodName, String argsExt) {
        if (argsExt == null || argsExt.trim().length() == 0) {
            return methodName;
        }
        return methodName + "~" + argsExt;
    }

    private static String defaultServiceStr(String cacheKey) {
        HOST_SERVER_TYPE_MAP.put(cacheKey, REDIS_SERVER_REDIS);
        return REDIS_SERVER_REDIS;
    }
}
