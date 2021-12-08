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
package com.pamirs.attach.plugin.log4j.interceptor.v1.creator;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Appender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.RollingFileAppender;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/11/08 7:48 下午
 */
public class ShadowAppenderCreatorFacadeV1 {

    private static final Map<Class<? extends Appender>, ShadowAppenderCreatorV1> map = new HashMap<>();

    static {
        for (AppenderCreatorBuilders value : AppenderCreatorBuilders.values()) {
            registerShadowAppenderCreator(value.matchClass, value.build());
        }
    }

    public static <T extends Appender> T createShadowAppenderCreator(T appender, String bizShadowLogPath) {
        ShadowAppenderCreatorV1<T> shadowAppenderCreator = map.get(appender.getClass());
        if (shadowAppenderCreator == null) {
            return null;
        }
        return shadowAppenderCreator.creatorPtAppender(appender, bizShadowLogPath);
    }

    public static void registerShadowAppenderCreator(
        Class<? extends Appender> appenderClass, ShadowAppenderCreatorV1 shadowAppenderCreator) {
        map.put(appenderClass, shadowAppenderCreator);
    }

    public interface AppenderCreatorBuilderV1<T extends Appender> {
        ShadowAppenderCreatorV1<T> build();
    }

    public enum AppenderCreatorBuilders implements AppenderCreatorBuilderV1 {

        FILE(FileAppender.class) {

            private final FileShadowAppenderCreator instance = new FileShadowAppenderCreator();

            @Override
            public ShadowAppenderCreatorV1<?> build() {
                return instance;
            }
        },

        ROLLING_FILE(RollingFileAppender.class) {

            private final RollingFileShadowAppenderCreator instance = new RollingFileShadowAppenderCreator();

            @Override
            public ShadowAppenderCreatorV1<?> build() {
                return instance;
            }
        },

        DAILY_ROLLING_FILE(DailyRollingFileAppender.class) {

            private final DailyRollingFileShadowAppenderCreator instance
                = new DailyRollingFileShadowAppenderCreator();

            @Override
            public ShadowAppenderCreatorV1<?> build() {
                return instance;
            }
        },
        ;

        private final Class<? extends Appender> matchClass;

        AppenderCreatorBuilders(Class<? extends Appender> matchClass) {this.matchClass = matchClass;}

    }
}
