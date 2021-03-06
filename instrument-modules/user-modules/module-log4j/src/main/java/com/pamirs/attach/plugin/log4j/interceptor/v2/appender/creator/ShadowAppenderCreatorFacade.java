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
package com.pamirs.attach.plugin.log4j.interceptor.v2.appender.creator;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/11/08 7:48 下午
 */
public class ShadowAppenderCreatorFacade {

    private static final Map<Class<? extends Appender>, ShadowAppenderCreator> map = new HashMap<>();

    static {
        for (AppenderCreatorBuilders value : AppenderCreatorBuilders.values()) {
            registerShadowAppenderCreator(value.matchClass, value.build());
        }
    }

    public static <T extends Appender> T createShadowAppenderCreator(T appender, String bizShadowLogPath) {
        ShadowAppenderCreator<T> shadowAppenderCreator = map.get(appender.getClass());
        if (shadowAppenderCreator == null) {
            return null;
        }
        return shadowAppenderCreator.creatorPtAppender(appender, bizShadowLogPath);
    }

    public static void registerShadowAppenderCreator(
        Class<? extends Appender> appenderClass, ShadowAppenderCreator shadowAppenderCreator) {
        map.put(appenderClass, shadowAppenderCreator);
    }

    private interface AppenderCreatorBuilder<T extends Appender> {
        ShadowAppenderCreator<T> build();
    }

    private enum AppenderCreatorBuilders implements AppenderCreatorBuilder {

        FILE(FileAppender.class) {

            private final FileShadowAppenderCreator instance = new FileShadowAppenderCreator();

            @Override
            public ShadowAppenderCreator<?> build() {
                return instance;
            }
        },

        ROLLING_FILE(RollingFileAppender.class) {

            private final RollingFileShadowAppenderCreator instance = new RollingFileShadowAppenderCreator();

            @Override
            public ShadowAppenderCreator<?> build() {
                return instance;
            }
        },

        ROLLING_RANDOM_ACCESS_FILE(RollingRandomAccessFileAppender.class) {

            private final RollingRandomAccessFileShadowAppenderCreator instance
                = new RollingRandomAccessFileShadowAppenderCreator();

            @Override
            public ShadowAppenderCreator<?> build() {
                return instance;
            }
        },
        ;

        private final Class<? extends Appender> matchClass;

        AppenderCreatorBuilders(Class<? extends Appender> matchClass) {this.matchClass = matchClass;}

    }
}
