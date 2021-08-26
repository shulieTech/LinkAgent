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
package com.pamirs.attach.plugin.mybatis;

import com.pamirs.pradar.MiddlewareType;
import org.apache.ibatis.cache.Cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author angju
 * @date 2020/10/22 15:43
 */
public class MybatisConstants {

    /**
     * 业务currentName与压测cache映射
     */
    public static Map<String, Cache> currentName2PtCacheMap = new ConcurrentHashMap<String, Cache>();

    public static final String PLUGIN_NAME = "mybatis";
    public static final String MODULE_NAME = "mybatis";

    public static final Integer PLUGIN_TYPE = MiddlewareType.TYPE_LOCAL;

    public static final String DYNAMIC_FIELD_COMMAND = "command";
    public static final String DYNAMIC_FIELD_CACHE = "cache";
    public static final String DYNAMIC_FIELD_CURRENT_NAMESPACE = "currentNamespace";
}
