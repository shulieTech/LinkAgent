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

    public static <T extends Appender> T createShadowAppenderCreator(T appender, String bizShadowLogPath) {
        AppenderCreatorBuilder<T> appenderCreatorBuilder = AppenderCreatorBuilders.match(appender.getClass());
        ShadowAppenderCreator<T> shadowAppenderCreator = appenderCreatorBuilder.build();
        return shadowAppenderCreator.creatorPtAppender(appender, bizShadowLogPath);
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

            private final RollingRandomAccessFileShadowAppenderCreator instance = new RollingRandomAccessFileShadowAppenderCreator();

            @Override
            public ShadowAppenderCreator<?> build() {
                return instance;
            }
        },
        ;

        private final Class<? extends Appender> matchClass;

        private static final Map<Class<? extends Appender>, AppenderCreatorBuilders> map
            = new HashMap<Class<? extends Appender>, AppenderCreatorBuilders>();

        static {
            for (AppenderCreatorBuilders value : AppenderCreatorBuilders.values()) {
                map.put(value.matchClass, value);
            }
        }

        public static AppenderCreatorBuilders match(Class<? extends Appender> clazz) {
            return map.get(clazz);
        }

        AppenderCreatorBuilders(Class<? extends Appender> matchClass) {this.matchClass = matchClass;}

    }
}
