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
package com.pamirs.attach.plugin.log4j.interceptor.v2.holder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.shulie.instrument.simulator.message.ConcurrentWeakHashMap;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.config.LoggerConfig;

/**
 * @Auther: vernon
 * @Date: 2020/12/10 11:16
 * @Description:
 */
public class Cache {

    static private final Map<Appender, Object> shadowAppenders = new HashMap<Appender, Object>();

    //已经配置过的LoggerConfig
    static private final Set<LoggerConfig> configedLoggerConfigs = new HashSet<LoggerConfig>();

    public static void release() {
        shadowAppenders.clear();
        configedLoggerConfigs.clear();
        AppenderCache.NULL_APPENDER = null;
    }

    public static class AppenderCache {

        private static Object NULL_APPENDER = new Object();

        public static Appender computeIfAbsent(Appender appender, Function<Appender, Appender> function) {
            Object result = shadowAppenders.get(appender);
            if (result == null) {
                synchronized (shadowAppenders) {
                    result = shadowAppenders.get(appender);
                    if (result == null) {
                        result = function.apply(appender);
                        shadowAppenders.put(appender, result == null ? NULL_APPENDER : result);
                    }
                }
            }
            return result == NULL_APPENDER ? null : (Appender)result;
        }
    }

    public static class LoggerConfigCache {

        public static void makeSureConfigOnce(LoggerConfig loggerConfig, Consumer<LoggerConfig> configConsumer) {
            if (!configedLoggerConfigs.contains(loggerConfig)) {
                synchronized (configedLoggerConfigs) {
                    if (!configedLoggerConfigs.contains(loggerConfig)) {
                        configConsumer.accept(loggerConfig);
                        configedLoggerConfigs.add(loggerConfig);
                    }
                }
            }
        }
    }

    public interface Function<T, R> {
        R apply(T t);
    }

    public interface Consumer<T> {
        void accept(T t);
    }
}
