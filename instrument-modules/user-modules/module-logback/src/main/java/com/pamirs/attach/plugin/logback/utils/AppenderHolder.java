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
package com.pamirs.attach.plugin.logback.utils;

import java.io.File;
import java.util.List;

import ch.qos.logback.core.util.COWArrayList;
import com.pamirs.pradar.Pradar;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import com.shulie.instrument.simulator.message.ConcurrentWeakHashMap;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/06/22 10:28 上午
 */
public class AppenderHolder {

    private static ConcurrentWeakHashMap ptAppenderListCache = new ConcurrentWeakHashMap();

    private static ConcurrentWeakHashMap targetCache = new ConcurrentWeakHashMap();

    private static final Object NULL = new Object();

    //因为agent的类加载器已经加载过了logback的相关类，这里会有类冲突(报转型错误)，所以全部用反射处理
    public static Object getOrCreatePtAppender(ClassLoader bizClassLoader, Object appender, String bizShadowLogPath)
        throws Exception {
        String appenderName = Reflect.on(appender).call("getName").get();
        if (appenderName.startsWith(Pradar.CLUSTER_TEST_PREFIX)) {
            return appender;
        }
        Object ptAppender = targetCache.get(appender);
        if (ptAppender != null) {
            return ptAppender == NULL ? null : ptAppender;
        }
        ptAppender = tryCreatePtAppender(bizClassLoader, appender, appenderName, bizShadowLogPath);
        targetCache.put(appender, ptAppender);
        return ptAppender == NULL ? null : ptAppender;
    }

    private static Object tryCreatePtAppender(ClassLoader bizClassLoader, Object appender, String appenderName,
        String bizShadowLogPath)
        throws Exception {
        if (appender.getClass().getName().equals("ch.qos.logback.classic.AsyncAppender")) {
            return createAsyncAppender(bizClassLoader, appender, appenderName, bizShadowLogPath);
        } else {
            if (!isFileAppender(bizClassLoader, appender)) {
                return NULL;
            }
            if (appender.getClass().getName().equals("ch.qos.logback.core.rolling.RollingFileAppender")) {
                return createRollingFileAppender(bizClassLoader, appender, appenderName, bizShadowLogPath);
            }
            return NULL;
        }
    }

    private static Object createAsyncAppender(ClassLoader bizClassLoader, Object appender, String appenderName,
        String bizShadowLogPath) throws Exception {
        Object appenderAttachable = Reflect.on(appender).get("aai");
        List appenderList = Reflect.on(appenderAttachable).get("appenderList");
        if (appenderList == null) {
            return NULL;
        }
        Object ptAsyncAppender = Reflect.on(appender.getClass()).create().get();
        for (Object subAppender : appenderList) {
            Object ptAppender = AppenderHolder.getOrCreatePtAppender(bizClassLoader, subAppender, bizShadowLogPath);
            if (ptAppender != null) {
                Reflect.on(ptAsyncAppender).call("addAppender", ptAppender).get();
            }
        }
        Reflect.on(ptAsyncAppender).call("setName", Pradar.addClusterTestPrefix(appenderName)).get();
        Reflect.on(ptAsyncAppender).call("start").get();
        return ptAsyncAppender;
    }

    private static Object createRollingFileAppender(ClassLoader bizClassLoader, Object appender, String appenderName,
        String bizShadowLogPath)
        throws ClassNotFoundException {
        String fileName = Reflect.on(appender).call("getFile").get();
        Object encoder = Reflect.on(appender).call("getEncoder").get();
        Object context = Reflect.on(appender).call("getContext").get();

        Object ptAppender = Reflect.on(appender.getClass()).create().get();
        Reflect.on(ptAppender).call("setFile", bizShadowLogPath + File.separator + getSufixFileName(fileName)).get();
        Reflect.on(ptAppender).call("setName", Pradar.CLUSTER_TEST_PREFIX + appenderName).get();
        Object rollingPolicy = Reflect.on(appender).call("getRollingPolicy").get();
        if (bizClassLoader.loadClass("ch.qos.logback.core.rolling.TimeBasedRollingPolicy")
            .isAssignableFrom(rollingPolicy.getClass())) {
            Object ptPolicy = copyTimePolicy(ptAppender, rollingPolicy, bizShadowLogPath);
            Reflect.on(ptPolicy).call("start").get();
            Reflect.on(ptAppender).call("setRollingPolicy", ptPolicy).get();
        } else {
            Reflect.on(rollingPolicy).call("start").get();
            Reflect.on(ptAppender).call("setRollingPolicy", rollingPolicy).get();
        }

        Object triggeringPolicy = Reflect.on(appender).call("getTriggeringPolicy").get();
        if (bizClassLoader.loadClass("ch.qos.logback.core.rolling.TimeBasedRollingPolicy").isAssignableFrom(
            triggeringPolicy.getClass())) {
            Object ptPolicy = copyTimePolicy(ptAppender, triggeringPolicy, bizShadowLogPath);
            Reflect.on(ptPolicy).call("start").get();
            Reflect.on(ptAppender).call("setTriggeringPolicy", ptPolicy).get();
        } else {
            Reflect.on(triggeringPolicy).call("start").get();
            Reflect.on(ptAppender).call("setTriggeringPolicy", triggeringPolicy).get();
        }

        Reflect.on(ptAppender).call("setEncoder", encoder).get();
        Reflect.on(ptAppender).call("setContext", context).get();
        Reflect.on(ptAppender).call("setAppend", true).get();
        Reflect.on(ptAppender).call("start").get();
        return ptAppender;
    }

    private static Object copyTimePolicy(Object ptAppender, Object rollingPolicy, String bizShadowLogPath) {
        Object ptPolicy = Reflect.on(rollingPolicy.getClass()).create().get();
        String ptFilePath = bizShadowLogPath + File.separator + getSufixFileName(Reflect.on(rollingPolicy)
            .call("getFileNamePattern")
            .<String>get());
        Reflect.on(ptPolicy).call("setFileNamePattern", ptFilePath);
        Reflect.on(ptPolicy).call("setContext", Reflect.on(rollingPolicy).call("getContext").get());
        Reflect.on(ptPolicy).call("setMaxHistory", Reflect.on(rollingPolicy).call("getMaxHistory").get());
        Reflect.on(ptPolicy).call("setParent", ptAppender);
        try {
            Reflect.on(ptPolicy).call("setMaxFileSize", Reflect.on(rollingPolicy).get("maxFileSize"));
        } catch (ReflectException ignore) {
        }
        return ptPolicy;
    }

    private static String getSufixFileName(String fileName) {
        if (!fileName.startsWith("/")) {
            return fileName;
        }
        return fileName.substring(fileName.indexOf("/") + 1);
    }

    public static boolean isFileAppender(ClassLoader bizClassLoader, Object appender) throws ClassNotFoundException {
        return bizClassLoader.loadClass("ch.qos.logback.core.FileAppender")
            .isAssignableFrom((appender.getClass()));
    }

    public static void release() {
        targetCache.clear();
        ptAppenderListCache.clear();
    }

    public static COWArrayList<Object> getPtAppenders(Object appenderAttachable) {
        return (COWArrayList<Object>)ptAppenderListCache.get(appenderAttachable);
    }

    public static void putPtAppenders(Object appenderAttachable,
        COWArrayList<Object> ptAppenderList) {
        ptAppenderListCache.put(appenderAttachable, ptAppenderList);
    }
}
