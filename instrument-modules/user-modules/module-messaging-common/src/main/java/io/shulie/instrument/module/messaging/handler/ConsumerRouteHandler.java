/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.shulie.instrument.module.messaging.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/9 14:55
 */
public class ConsumerRouteHandler {

    public static Map<String, Object> NOT_ROUTE_CACHE = new ConcurrentHashMap<>();

    /**
     * 判断当前对象是否需要路由
     *
     * @param obj 判断对象
     * @return true需要路由，false不需要路由
     */
    public static boolean needRoute(Object obj) {
        return NOT_ROUTE_CACHE.get(String.valueOf(System.identityHashCode(obj))) == null;
    }

    /**
     * 增加不需要路由对象
     *
     * @param obj 不路由对象
     */
    public static void addNotRouteObj(Object obj) {
        NOT_ROUTE_CACHE.put(String.valueOf(System.identityHashCode(obj)), obj);
    }

}
