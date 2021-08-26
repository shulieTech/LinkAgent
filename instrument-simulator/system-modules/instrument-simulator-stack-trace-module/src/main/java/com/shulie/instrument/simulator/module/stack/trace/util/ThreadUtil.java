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
package com.shulie.instrument.simulator.module.stack.trace.util;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

abstract public class ThreadUtil {

    public static TraceInfo getTraceInfo(Thread thread) {
        try {
            Object getInheritableThreadLocalMap = getInheritableThreadLocalMap(thread); //ThreadLocal.ThreadLocalMap
            if (getInheritableThreadLocalMap == null) {
                return null;
            }
            Object threadLocalMap = getThreadLocalMap(thread); //ThreadLocal.ThreadLocalMap
            if (threadLocalMap == null) {
                return null;
            }
            Map<ThreadLocal<?> /*这个线程的所有threadLocal */, Object/* threadLocal的值 */> allThreadLocals
                = getAllThreadLocals(getInheritableThreadLocalMap);
            allThreadLocals.putAll(getAllThreadLocals(threadLocalMap));
            for (Entry<ThreadLocal<?>, Object> entry : allThreadLocals.entrySet()) {
                Object value = entry.getValue();
                if (value == null) {
                    continue;
                }
                if (value.getClass().getName().equals("com.pamirs.pradar.InvokeContext")) {
                   return getTraceInfo(value);
                }
            }

        } catch (Throwable e) {
            // ignore
        }
        return null;
    }

    private static TraceInfo getTraceInfo(Object value) {
        try {
            TraceInfo traceInfo = new TraceInfo();

            Method getTraceIdMethod = value.getClass().getDeclaredMethod("getTraceId");
            getTraceIdMethod.setAccessible(true);
            String traceId = (String) getTraceIdMethod.invoke(value);
            traceInfo.setTraceId(traceId);
            // get rpc id
            Method getRpcIdMethod = value.getClass().getDeclaredMethod("getInvokeId");
            getTraceIdMethod.setAccessible(true);
            String rpcId = (String) getRpcIdMethod.invoke(value);
            traceInfo.setRpcId(rpcId);

            Method isClusterTestMethod = value.getClass().getDeclaredMethod("isClusterTest");
            isClusterTestMethod.setAccessible(true);
            boolean isClusterTest = (Boolean) isClusterTestMethod.invoke(value);
            traceInfo.setClusterTest(isClusterTest);
            return traceInfo;
        } catch (Exception e) {
            return null;
        }
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

    private static Object getInheritableThreadLocalMap(Thread thread) throws Exception {
        Field threadLocalsField = Thread.class.getDeclaredField("inheritableThreadLocals");
        threadLocalsField.setAccessible(true);
        return threadLocalsField.get(thread);
    }
}
