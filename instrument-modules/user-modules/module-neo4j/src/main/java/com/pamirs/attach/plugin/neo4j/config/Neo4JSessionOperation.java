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
package com.pamirs.attach.plugin.neo4j.config;

import java.util.HashMap;
import java.util.Map;

import com.pamirs.attach.plugin.neo4j.operation.BeginTransactionOperation;
import com.pamirs.attach.plugin.neo4j.operation.ClearOperation;
import com.pamirs.attach.plugin.neo4j.operation.ContextOperation;
import com.pamirs.attach.plugin.neo4j.operation.CountEntitiesOfTypeOperation;
import com.pamirs.attach.plugin.neo4j.operation.DebugOperation;
import com.pamirs.attach.plugin.neo4j.operation.DeleteAllOperation;
import com.pamirs.attach.plugin.neo4j.operation.DeleteOperation;
import com.pamirs.attach.plugin.neo4j.operation.DetachNodeEntityOperation;
import com.pamirs.attach.plugin.neo4j.operation.DetachRelationshipEntityOperation;
import com.pamirs.attach.plugin.neo4j.operation.DisposeOperation;
import com.pamirs.attach.plugin.neo4j.operation.DoInTransactionOperation;
import com.pamirs.attach.plugin.neo4j.operation.EntityTypeOperation;
import com.pamirs.attach.plugin.neo4j.operation.EventsEnabledOperation;
import com.pamirs.attach.plugin.neo4j.operation.GetTransactionOperation;
import com.pamirs.attach.plugin.neo4j.operation.InfoOperation;
import com.pamirs.attach.plugin.neo4j.operation.LoadAllOperation;
import com.pamirs.attach.plugin.neo4j.operation.LoadOperation;
import com.pamirs.attach.plugin.neo4j.operation.MetaDataOperation;
import com.pamirs.attach.plugin.neo4j.operation.NotifyListenersOperation;
import com.pamirs.attach.plugin.neo4j.operation.Operation;
import com.pamirs.attach.plugin.neo4j.operation.PurgeDatabaseOperation;
import com.pamirs.attach.plugin.neo4j.operation.QueryForObjectOperation;
import com.pamirs.attach.plugin.neo4j.operation.QueryOperation;
import com.pamirs.attach.plugin.neo4j.operation.QueryStatementsForOperation;
import com.pamirs.attach.plugin.neo4j.operation.RegisterOperation;
import com.pamirs.attach.plugin.neo4j.operation.RequestHandlerOperation;
import com.pamirs.attach.plugin.neo4j.operation.ResolveGraphIdForOperation;
import com.pamirs.attach.plugin.neo4j.operation.SaveOperation;
import com.pamirs.attach.plugin.neo4j.operation.SetDriverOperation;
import com.pamirs.attach.plugin.neo4j.operation.TransactionManagerOperation;
import com.pamirs.attach.plugin.neo4j.operation.WarnOperation;

/**
 * @ClassName: Neo4JSessionOperationEnum
 * @author: wangjian
 * @Date: 2020/8/1 00:03
 * @Description:
 */
public class Neo4JSessionOperation {

    private static Map<String, Operation> cache = new HashMap<String, Operation>();

    static {
        cache.put("register", new RegisterOperation());
        cache.put("notifyListeners", new NotifyListenersOperation());
        cache.put("eventsEnabled", new EventsEnabledOperation());
        cache.put("dispose", new DisposeOperation());
        cache.put("load", new LoadOperation());
        cache.put("loadAll", new LoadAllOperation());
        cache.put("queryForObject", new QueryForObjectOperation());
        cache.put("query", new QueryOperation());
        cache.put("countEntitiesOfType", new CountEntitiesOfTypeOperation());
        cache.put("purgeDatabase", new PurgeDatabaseOperation());
        cache.put("clear", new ClearOperation());
        cache.put("delete", new DeleteOperation());
        cache.put("deleteAll", new DeleteAllOperation());
        cache.put("save", new SaveOperation());
        cache.put("beginTransaction", new BeginTransactionOperation());
        cache.put("doInTransaction", new DoInTransactionOperation());
        cache.put("getTransaction", new GetTransactionOperation());
        cache.put("resolveGraphIdFor", new ResolveGraphIdForOperation());
        cache.put("detachNodeEntity", new DetachNodeEntityOperation());
        cache.put("detachRelationshipEntity", new DetachRelationshipEntityOperation());
        cache.put("queryStatementsFor", new QueryStatementsForOperation());
        cache.put("entityType", new EntityTypeOperation());
        cache.put("context", new ContextOperation());
        cache.put("metaData", new MetaDataOperation());
        cache.put("setDriver", new SetDriverOperation());
        cache.put("requestHandler", new RequestHandlerOperation());
        cache.put("transactionManager", new TransactionManagerOperation());
        cache.put("info", new InfoOperation());
        cache.put("warn", new WarnOperation());
        cache.put("debug", new DebugOperation());
    }

    public static Operation of(String method) {
        return cache.get(method);
    }

    public static void release() {
        if (cache != null) {
            cache.clear();
            cache = null;
        }
    }
}
