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
package com.shulie.instrument.simulator.core.util;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/06/17 9:59 下午
 */
public class ThreadLocalCleaner {

    private final static Logger LOGGER = LoggerFactory.getLogger(ThreadLocalCleaner.class);

    public static void clear() {
        Collection<Thread> allThreads = getAllThreads();
        try {
            for (Thread thread : allThreads) {
                Object threadLocalMap = getThreadLocalMap(thread); //ThreadLocal.ThreadLocalMap
                if (threadLocalMap == null) {
                    continue;
                }
                Map<ThreadLocal<?> /*这个线程的所有threadLocal */, Object/* threadLocal的值 */> allThreadLocals
                    = getAllThreadLocals(threadLocalMap);
                for (Entry<ThreadLocal<?>, Object> entry : allThreadLocals.entrySet()) {
                    Object value = entry.getValue();
                    if (value == null) {
                        continue;
                    }
                    if (isNeedRemove(value)) {
                        removeFromThreadLocalMap(threadLocalMap, entry.getKey());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("clear ThreadLocal fail!", e);
        }
    }

    private static boolean isNeedRemove(Object value) {
        String className = value.getClass().getName();
        return className.startsWith("com.shulie") ||
            className.startsWith("io.shulie") ||
            className.startsWith("com.pamirs");
    }

    private static void removeFromThreadLocalMap(Object threadLocalMap, ThreadLocal<?> key) throws Exception {
        Method method = threadLocalMap.getClass().getDeclaredMethod("remove", ThreadLocal.class);
        method.setAccessible(true);
        method.invoke(threadLocalMap, key);
    }

    private static Map<ThreadLocal<?>, Object> getAllThreadLocals(Object threadLocalMap) throws Exception {
        Field tableField = threadLocalMap.getClass().getDeclaredField("table");
        tableField.setAccessible(true);
        Object[] table = (Object[])tableField.get(threadLocalMap); //ThreadLocal.ThreadLocalMap.Entry
        if (table == null || table.length == 0) {
            return Collections.emptyMap();
        }
        Map<ThreadLocal<?>, Object> result = new HashMap<ThreadLocal<?>, Object>(table.length);
        for (Object entry : table) {
            if (entry != null) {
                WeakReference<ThreadLocal<?>> threadLocalRef = (WeakReference<ThreadLocal<?>>)entry;
                ThreadLocal<?> threadLocal = threadLocalRef.get();
                if (threadLocal != null) {
                    result.put(threadLocal, getThreadLocalValueFromEntry(entry));
                }
            }
        }
        return result;
    }

    private static Object getThreadLocalValueFromEntry(Object entry) throws Exception {
        Field field = entry.getClass().getDeclaredField("value");
        field.setAccessible(true);
        return field.get(entry);
    }

    private static Object getThreadLocalMap(Thread thread) throws Exception {
        Field threadLocalsField = Thread.class.getDeclaredField("threadLocals");
        threadLocalsField.setAccessible(true);
        return threadLocalsField.get(thread);
    }

    private static Collection<Thread> getAllThreads() {
        return Thread.getAllStackTraces().keySet();
    }

    public static void main(String[] args) throws InterruptedException {
        final AtomicBoolean flag = new AtomicBoolean(true);
        new Thread(new Runnable() {

            ThreadLocal<Context> threadLocal1 = new ThreadLocal<Context>();

            ThreadLocal<Long> threadLocal2 = new ThreadLocal<Long>();

            @Override
            public void run() {
                threadLocal1.set(new Context());
                threadLocal2.set(1L);
                while (flag.get()) {
                    System.out.println(threadLocal1.get());
                    System.out.println(threadLocal2.get());
                    try {
                        TimeUnit.MILLISECONDS.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }).start();
        TimeUnit.SECONDS.sleep(2);
        clear();
        TimeUnit.SECONDS.sleep(2);
        flag.set(false);
    }

    private static class Context {

    }
}
