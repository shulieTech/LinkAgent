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
package com.pamirs.attach.plugin.mongodb.utils;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoNamespace;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.internal.MongoClientDelegate;
import com.mongodb.client.internal.OperationExecutor;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.shulie.instrument.simulator.api.reflect.Reflect;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/07/01 10:28 上午
 */
public class Caches {

    private final static Map<String, MongoClient> clientMapping
        = new ConcurrentHashMap<String, MongoClient>();

    private final static Map<String, OperationExecutor> operationExecutorMap
        = new ConcurrentHashMap<String, OperationExecutor>();

    private final static Map<Object, MongoClientDelegate> mongoClientDelegates
        = new ConcurrentHashMap<Object, MongoClientDelegate>();

    public static OperationExecutor getPtOperationExecutor(OperationAccessor operationAccessor,
        ShadowDatabaseConfig shadowDatabaseConfig, Object operation, MongoNamespace busMongoNamespace) throws Exception {
        OperationExecutor ptExecutor = operationExecutorMap.get(operation);
        if (ptExecutor == null) {
            ptExecutor = operationExecutorMap.get(operation);
            synchronized (operationExecutorMap) {
                if (ptExecutor == null) {
                    MongoClient ptMongoClient = getPtMongoClient(shadowDatabaseConfig);
                    String ptDB = busMongoNamespace.getDatabaseName();
                    String ptCOL = busMongoNamespace.getCollectionName();
                    if (!Pradar.isClusterTestPrefix(ptDB)){
                        ptDB = Pradar.addClusterTestPrefix(ptDB);
                    }
                    if (!Pradar.isClusterTestPrefix(ptCOL)){
                        ptCOL = Pradar.addClusterTestPrefix(ptCOL);
                    }
                    MongoNamespace ptMongoNamespace = new MongoNamespace(ptDB
                        , ptCOL
                         //大写与影子表模式保持一致
                    );
                    MongoCollection ptMongoCollection = ptMongoClient.getDatabase(ptMongoNamespace.getDatabaseName())
                        .getCollection(ptMongoNamespace.getCollectionName());
                    operationAccessor.setMongoNamespace(operation, ptMongoNamespace);
                    ptExecutor = Reflect.on(ptMongoCollection).get("executor");
                }
            }
        }
        return ptExecutor;
    }

    private static MongoClient getPtMongoClient(ShadowDatabaseConfig config) {
        MongoClient mongoClient = clientMapping.get(config.getUrl());
        if (mongoClient == null) {
            synchronized (clientMapping) {
                mongoClient = clientMapping.get(config.getUrl());
                if (mongoClient == null) {
                    mongoClient = new MongoClient(new MongoClientURI(config.getShadowUrl()));
                    clientMapping.put(config.getUrl(), mongoClient);
                }
            }
        }
        return mongoClient;
    }

    public static MongoClientDelegate getMongoClientDelegate(Object executor) throws Exception {
        MongoClientDelegate mongoClientDelegate = mongoClientDelegates.get(executor);
        if (mongoClientDelegate == null) {
            synchronized (mongoClientDelegates) {
                mongoClientDelegate = mongoClientDelegates.get(executor);
                if (mongoClientDelegate == null) {
                    Field field = null;
                    try {
                        field = executor.getClass().getDeclaredField("this$0");
                        field.setAccessible(true);
                        mongoClientDelegate = (MongoClientDelegate)field.get(executor);
                    } finally {
                        if (field != null) {
                            field.setAccessible(false);
                        }
                    }
                }
            }
        }
        return mongoClientDelegate;
    }

    public static void clean() {
        clientMapping.clear();
        mongoClientDelegates.clear();
        operationExecutorMap.clear();
    }
}
