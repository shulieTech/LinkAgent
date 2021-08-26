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
package com.pamirs.attach.plugin.apache.kafka.stream.constants;

import com.shulie.instrument.simulator.message.ConcurrentWeakHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author angju
 * @date 2021/5/24 23:04
 */
public class KafkaStreamsCaches {
    private final static Logger LOGGER = LoggerFactory.getLogger(KafkaStreamsCaches.class);
    private static Method closeMethod;
    public static ConcurrentWeakHashMap<String/*business consumer-stream applicationId*/, Object/*shadow consumer-stream*/> caches = new ConcurrentWeakHashMap<String, Object>(8);

    public static void addKafkaStreams(String key, Object kafkaStreams) {
        Object old = caches.put(key, kafkaStreams);
        if (old != null) {
            initMethod(old);
            invokeClose(old);
        }
    }

    public static Object getKafkaStreams(String key) {
        return caches.get(key);
    }

    public static void close(String key, long time, TimeUnit timeUnit) {
        Object kafkaStreams = getKafkaStreams(key);
        if (kafkaStreams != null) {
            initMethod(kafkaStreams);
            invokeClose(kafkaStreams, time, timeUnit);
        }
    }

    private static void invokeClose(Object obj, long time, TimeUnit timeUnit) {
        if (closeMethod == null) {
            return;
        }
        try {
            closeMethod.invoke(obj, time, timeUnit);
        } catch (Throwable e) {
            LOGGER.error("close KafkaStreams error.", e);
        }
    }

    private static void invokeClose(Object obj) {
        if (closeMethod == null) {
            return;
        }
        try {
            closeMethod.invoke(obj, 0, TimeUnit.SECONDS);
        } catch (Throwable e) {
            LOGGER.error("close KafkaStreams error.", e);
        }
    }

    private static void initMethod(Object kafkaStreams) {
        if (closeMethod == null || kafkaStreams == null) {
            return;
        }
        try {
            closeMethod = kafkaStreams.getClass().getDeclaredMethod("close", long.class, TimeUnit.class);
            closeMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            LOGGER.error("can't found close() in KafkaStreams", e);
        }
    }

    public static boolean contains(String key) {
        return caches.containsKey(key);
    }

    public static void release() {
        for (Map.Entry<String, Object> entry : caches.entrySet()) {
            initMethod(entry.getValue());
            invokeClose(entry.getValue());
        }
        caches.clear();
    }
}
