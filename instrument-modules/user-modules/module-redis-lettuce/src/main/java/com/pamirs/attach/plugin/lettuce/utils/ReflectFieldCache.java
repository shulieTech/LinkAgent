package com.pamirs.attach.plugin.lettuce.utils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.shulie.instrument.simulator.api.reflect.Reflect;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/04/26 6:12 PM
 */
public class ReflectFieldCache {

    private final static Map<String, Field> CACHE = new HashMap<String, Field>();

    public static Field get(String field, Object target) {
        String key = target.getClass().getName() + "." + field;
        Field value = CACHE.get(key);
        if (value == null) {
            synchronized (ReflectFieldCache.class) {
                value = CACHE.get(key);
                if (value == null) {
                    value = Reflect.on(target).field0(field);
                    if (value != null) {
                        CACHE.put(key, value);
                    }
                }
            }
        }
        return value;
    }

    public static void clear() {
        CACHE.clear();
    }
}
