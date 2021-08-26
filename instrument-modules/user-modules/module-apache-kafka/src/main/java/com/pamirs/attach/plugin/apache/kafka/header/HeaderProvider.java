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

import com.pamirs.attach.plugin.apache.kafka.header.impl.DefaultHeaderProcessor;
import com.pamirs.attach.plugin.apache.kafka.header.impl.DisabledHeaderProcessor;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/7/10 7:57 下午
 */
public class HeaderProvider {
    private static ConcurrentMap<Class, HeaderProcessor> builders = new ConcurrentHashMap<Class, HeaderProcessor>();

    private static HeaderProcessor buildHeaderProcessor(Class clazz) {
        try {
            final Method method = clazz.getMethod("headers");
            if (method != null) {
                return new DefaultHeaderProcessor();
            }
        } catch (Throwable e) {
        }
        return new DisabledHeaderProcessor();
    }

    public final static HeaderProcessor getHeaderProcessor(Object record) {
        final Class<?> clazz = record.getClass();
        HeaderProcessor headerProcessor = builders.get(clazz);
        if (headerProcessor != null) {
            return headerProcessor;
        }
        synchronized (builders) {
            headerProcessor = builders.get(clazz);
            if (headerProcessor != null) {
                return headerProcessor;
            }
            headerProcessor = buildHeaderProcessor(clazz);
            builders.putIfAbsent(clazz, headerProcessor);
        }
        return headerProcessor;
    }

    public static void clear() {
        builders.clear();
    }
}
