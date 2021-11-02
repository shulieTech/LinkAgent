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

import java.util.Map;

import com.pamirs.pradar.Pradar;
import com.shulie.instrument.simulator.api.reflect.Reflect;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/09/14 10:12 下午
 */
public class LogEventTestFlagUtils {

    private static final String CLUSTER_TEST_FLAG_IN_LOG_EVENT = "p-pradar-cluster-test";

    public static boolean isClusterTest(Object event) {
        if (Pradar.isClusterTest()) {
            return true;
        }
        if (event.getClass().getName().equals("ch.qos.logback.classic.spi.LoggingEvent")) {
            Map<String, String> propertyMap = getPropertyMap(event);
            String flag = propertyMap.get(CLUSTER_TEST_FLAG_IN_LOG_EVENT);
            if (flag == null) {
                return false;
            }
            return "true".equals(flag);
        }
        return false;
    }

    public static void setFlag(Object event, boolean flag) {
        if (flag) {
            Map<String, String> propertyMap = getPropertyMap(event);
            propertyMap.put(CLUSTER_TEST_FLAG_IN_LOG_EVENT, "true");
        }
    }

    private static Map<String, String> getPropertyMap(Object event) {
        Object loggerContextVo = Reflect.on(event).get("loggerContextVO");
        return Reflect.on(loggerContextVo).get("propertyMap");
    }

}
