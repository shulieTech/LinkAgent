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
package com.pamirs.attach.plugin.neo4j;

import com.pamirs.pradar.MiddlewareType;

import java.util.Arrays;
import java.util.List;

/**
 * @ClassName: Neo4JConstants
 * @author: wangjian
 * @Date: 2020/7/31 15:29
 * @Description:
 */
public class Neo4JConstants {

    public static final String PLUGIN_NAME = "neo4j";
    public static final String MODULE_NAME = "neo4j";

    public static final int PLUGIN_TYPE = MiddlewareType.TYPE_DB;

    public final static String DYNAMIC_FIELD_DRIVER = "driver";

    public static final List<String> SESSION_OPERATIONS = Arrays.asList("register", "notifyListeners",
            "eventsEnabled", "dispose", "load", "loadAll", "queryForObject", "query",
            "countEntitiesOfType", "purgeDatabase", "clear", "delete", "deleteAll",
            "save", "beginTransaction", "doInTransaction", "getTransaction", "resolveGraphIdFor",
            "detachNodeEntity", "detachRelationshipEntity", "queryStatementsFor", "entityType",
            "context", "metaData", "setDriver", "requestHandler", "transactionManager", "info", "warn", "debug");

    public static final List<String> SESSION_OPERATIONS_NO_LOAD_ALL = Arrays.asList("register", "notifyListeners",
            "eventsEnabled", "dispose", "load", "queryForObject", "query",
            "countEntitiesOfType", "purgeDatabase", "clear", "delete", "deleteAll",
            "save", "beginTransaction", "doInTransaction", "getTransaction", "resolveGraphIdFor",
            "detachNodeEntity", "detachRelationshipEntity", "queryStatementsFor", "entityType",
            "context", "metaData", "setDriver", "requestHandler", "transactionManager", "info", "warn", "debug");
}
