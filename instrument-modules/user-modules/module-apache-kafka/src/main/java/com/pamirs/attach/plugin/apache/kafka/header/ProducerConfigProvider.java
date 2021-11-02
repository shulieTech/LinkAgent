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
package com.pamirs.attach.plugin.apache.kafka.header;

import com.pamirs.attach.plugin.apache.kafka.header.impl.DisabledProducerConfigProcessor;
import com.pamirs.attach.plugin.apache.kafka.header.impl.ProducerConfigLowerProcessor;
import com.pamirs.attach.plugin.apache.kafka.header.impl.ProducerConfigNewerProcessor;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/7/10 7:57 下午
 */
public class ProducerConfigProvider {
    private static ConcurrentMap<Class, ProducerConfigProcessor> builders = new ConcurrentHashMap<Class, ProducerConfigProcessor>();

    private static ProducerConfigProcessor buildHeaderProcessor(Class clazz) {
        try {
            final Method method = clazz.getMethod("getList", String.class);
            if (method != null) {
                return new ProducerConfigNewerProcessor();
            }
        } catch (Throwable e) {
        }

        try {
            final Method method = clazz.getMethod("getString", String.class);
            if (method != null) {
                return new ProducerConfigLowerProcessor();
            }
        } catch (Throwable e) {
        }
        return new DisabledProducerConfigProcessor();
    }

    public final static ProducerConfigProcessor getProducerConfigProcessor(Object record) {
        final Class<?> clazz = record.getClass();
        ProducerConfigProcessor producerConfigProcessor = builders.get(clazz);
        if (producerConfigProcessor != null) {
            return producerConfigProcessor;
        }
        synchronized (builders) {
            producerConfigProcessor = builders.get(clazz);
            if (producerConfigProcessor != null) {
                return producerConfigProcessor;
            }
            producerConfigProcessor = buildHeaderProcessor(clazz);
            builders.putIfAbsent(clazz, producerConfigProcessor);
        }
        return producerConfigProcessor;
    }

    public static void clear() {
        builders.clear();
    }
}
