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
package com.pamirs.attach.plugin.proxool.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author angju
 * @date 2021/4/5 10:24
 */
public class ConnectionPoolUtils {
    private static Set connectionPools = new HashSet();
    private static Map connectionPoolMap = new HashMap();
    private static Map<String, Object> url2ConnectionPoolDefinitionMap = new HashMap<String, Object>(8, 1);

    public static void release() {
        connectionPools.clear();
        connectionPoolMap.clear();
        url2ConnectionPoolDefinitionMap.clear();
    }

    public static Set getConnectionPools() {
        return connectionPools;
    }

    public static void setConnectionPools(Set connectionPools) {
        ConnectionPoolUtils.connectionPools = connectionPools;
    }

    public static Map getConnectionPoolMap() {
        return connectionPoolMap;
    }

    public static void setConnectionPoolMap(Map connectionPoolMap) {
        ConnectionPoolUtils.connectionPoolMap = connectionPoolMap;
    }

    public static void addConnectionPoolDefinition(String url, Object connectionPoolDefinition) {
        url2ConnectionPoolDefinitionMap.put(url, connectionPoolDefinition);
    }
}
