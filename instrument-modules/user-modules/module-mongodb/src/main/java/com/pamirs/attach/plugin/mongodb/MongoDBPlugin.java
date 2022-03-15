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
package com.pamirs.attach.plugin.mongodb;

import com.pamirs.attach.plugin.mongodb.interceptor.*;
import com.pamirs.attach.plugin.mongodb.interceptor.mongo2_14_3.*;
import com.pamirs.attach.plugin.mongodb.utils.Caches;
import com.pamirs.attach.plugin.mongodb.utils.OperationAccessorFactory;
import com.pamirs.pradar.interceptor.Interceptors;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import com.shulie.instrument.simulator.api.scope.ExecutionPolicy;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.MetaInfServices;

/**
 * @author angju
 * @date 2020/8/5 18:58
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "mongodb", version = "1.0.0", author = "angju@shulie.io",description = "mongdb 数据库")
public class MongoDBPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() throws Throwable {
        /**
         * 因为这个插件与mongodb322插件冲突，所以当mongodb322插件启用时此插件禁用
         */
        //默认用3
        boolean use = true;
        String mongodb3Enabled = System.getProperty("mongodb3.enabled");
        String mongodb4Enabled = System.getProperty("mongodb4.enabled");

        if (mongodb4Enabled == null){
            mongodb4Enabled = simulatorConfig.getProperty("mongodb4.enabled");
        }
        if (mongodb3Enabled == null){
            mongodb3Enabled = simulatorConfig.getProperty("mongodb3.enabled");
        }

        if (StringUtils.isNotBlank(mongodb4Enabled) && Boolean.valueOf(mongodb4Enabled)){
            use = false;
        }
        if (StringUtils.isNotBlank(mongodb3Enabled) && !Boolean.valueOf(mongodb3Enabled)){
            use = false;
        }
        if (!use) {
            return false;
        }

//        enhanceTemplate.enhance(this, "com.mongodb.MongoClient", new EnhanceCallback() {
//            @Override
//            public void doEnhance(InstrumentClass target) {
//
//                InstrumentMethod mongoConstructor = target.getConstructor(
//                        "java.util.List"
//                        , "java.util.List", "com.mongodb.MongoClientOptions");
//                mongoConstructor.addInterceptor(Listeners.of(MongoDBMongoConstructorInterceptor.class));
//
//                InstrumentMethod mongoConstructor_1 = target.getConstructor(
//                        "com.mongodb.MongoClientURI");
//                mongoConstructor_1.addInterceptor(Listeners.of(MongoDBMongoConstructor_1_Interceptor.class));
//
//                InstrumentMethod mongoConstructor_2 = target.getConstructor(
//                        "java.util.List", "com.mongodb.MongoCredential",
//                        "com.mongodb.MongoClientOptions");
//                mongoConstructor_2.addInterceptor(Listeners.of(MongoDBMongoConstructor_2_Interceptor.class));
//
//                InstrumentMethod mongoConstructor_3 = target.getConstructor(
//                        "java.util.List",
//                        "com.mongodb.MongoClientOptions");
//
//                mongoConstructor_3.addInterceptor(Listeners.of(MongoDBMongoConstructor_3_Interceptor.class));
//            }
//        });


//        enhanceTemplate.enhance(this, "com.mongodb.client.internal.MongoClientDelegate", new EnhanceCallback() {
//            @Override
//            public void doEnhance(InstrumentClass target) {
//                /**
//                 * 3.8.2版本
//                 */
//                InstrumentMethod method_1 = target.getConstructor(
//                        "com.mongodb.connection.Cluster",
//                        "java.util.List", "java.lang.Object", "com.mongodb.client.internal.OperationExecutor");
//                method_1.addInterceptor(Listeners.of(MongoDBMongoClientDelegateConstructorInterceptor.class));
//
//                /**
//                 * 3.11.2版本
//                 */
//                InstrumentMethod method_2 = target.getConstructor(
//                        "com.mongodb.connection.Cluster",
//                        "java.util.List", "java.lang.Object", "com.mongodb.client.internal.OperationExecutor",
//                        "com.mongodb.client.internal.Crypt");
//
//                method_2.addInterceptor(Listeners.of(MongoDBMongoClientDelegateConstructorInterceptor.class));
//            }
//        });

        //3.11.2
        enhanceTemplate.enhance(this, "com.mongodb.client.internal.MongoClientDelegate$DelegateOperationExecutor", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {

                //3.11.2 start
                InstrumentMethod instrumentMethod_1 = target.getDeclaredMethod("execute",
                        "com.mongodb.operation.ReadOperation", "com.mongodb.ReadPreference",
                        "com.mongodb.ReadConcern", "com.mongodb.client.ClientSession");

                InstrumentMethod instrumentMethod_2 = target.getDeclaredMethod("execute",
                        "com.mongodb.operation.WriteOperation", "com.mongodb.ReadConcern",
                        "com.mongodb.client.ClientSession");

                instrumentMethod_1.addInterceptor(Listeners.of(DelegateOperationExecutorInterceptor.class));
                instrumentMethod_1.addInterceptor(Listeners.of(DelegateOperationExecutorTraceInterceptor.class, "execute_trace", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
                instrumentMethod_2.addInterceptor(Listeners.of(DelegateOperationExecutorInterceptor.class));
                instrumentMethod_2.addInterceptor(Listeners.of(DelegateOperationExecutorTraceInterceptor.class, "execute_trace", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));


                InstrumentMethod instrumentMethod_3 = target.getDeclaredMethod("execute",
                    "com.mongodb.internal.operation.ReadOperation", "com.mongodb.ReadPreference",
                    "com.mongodb.ReadConcern", "com.mongodb.client.ClientSession");

                InstrumentMethod instrumentMethod_4 = target.getDeclaredMethod("execute",
                    "com.mongodb.internal.operation.WriteOperation", "com.mongodb.ReadConcern",
                    "com.mongodb.client.ClientSession");

                instrumentMethod_3.addInterceptor(Listeners.of(SyncDelegateOperationExecutorInterceptor.class));
                instrumentMethod_4.addInterceptor(Listeners.of(SyncDelegateOperationExecutorInterceptor.class));
                //3.11.2 end


            }
        });


        //3.4.2
        enhanceTemplate.enhance(this, "com.mongodb.Mongo", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {

                InstrumentMethod instrumentMethod_1 = target.getDeclaredMethod("execute",
                        "com.mongodb.operation.WriteOperation");

                InstrumentMethod instrumentMethod_2 = target.getDeclaredMethod("execute",
                        "com.mongodb.operation.ReadOperation", "com.mongodb.ReadPreference");

                instrumentMethod_1.addInterceptor(Listeners.of(MongoExecuteInterceptor.class));
                instrumentMethod_2.addInterceptor(Listeners.of(MongoExecuteInterceptor.class));
                instrumentMethod_1.addInterceptor(Listeners.of(MongoExecuteTraceInterceptor.class));
                instrumentMethod_2.addInterceptor(Listeners.of(MongoExecuteTraceInterceptor.class));
                instrumentMethod_1.addInterceptor(Listeners.of(MongoExecuteCutoffInterceptor.class,"WriteOperation", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
                instrumentMethod_2.addInterceptor(Listeners.of(MongoExecuteCutoffInterceptor.class, "ReadOperation", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
            }
        });


        //2.14.3
        /**
         * com.mongodb.DBCollection#findOne(com.mongodb.DBObject, com.mongodb.DBObject, com.mongodb.DBObject, com.mongodb.ReadPreference, long, java.util.concurrent.TimeUnit)
         */
        enhanceTemplate.enhance(this, "com.mongodb.DBCollection", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {

                InstrumentMethod instrumentMethod_1 = target.getDeclaredMethod("findOne",
                        "com.mongodb.DBObject", "com.mongodb.DBObject", "com.mongodb.DBObject",
                        "com.mongodb.ReadPreference", "long", "java.util.concurrent.TimeUnit");

                instrumentMethod_1.addInterceptor(Listeners.of(DBCollectionFineOneInterceptor.class,"DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                target.getDeclaredMethod("insert", "java.util.List", "com.mongodb.WriteConcern", "com.mongodb.DBEncoder")
                    .addInterceptor(Listeners.of(DBCollectionInsertInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
                target.getDeclaredMethod("insert", "java.util.List", "com.mongodb.InsertOptions")
                    .addInterceptor(Listeners.of(DBCollectionInsertInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                target.getDeclaredMethod("update", "com.mongodb.DBObject", "com.mongodb.DBObject", "boolean", "boolean", "com.mongodb.WriteConcern", "boolean", "com.mongodb.DBEncoder")
                    .addInterceptor(Listeners.of(DBCollectionUpdateInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
                target.getDeclaredMethod("update", "com.mongodb.DBObject", "com.mongodb.DBObject", "boolean", "boolean", "com.mongodb.WriteConcern", "com.mongodb.DBEncoder")
                    .addInterceptor(Listeners.of(DBCollectionUpdateInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                target.getDeclaredMethod("find")
                    .addInterceptor(Listeners.of(DBCollectionFindInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
                target.getDeclaredMethod("find", "com.mongodb.DBObject", "com.mongodb.DBObject")
                    .addInterceptor(Listeners.of(DBCollectionFindInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
                target.getDeclaredMethod("find", "com.mongodb.DBObject")
                    .addInterceptor(Listeners.of(DBCollectionFindInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                target.getDeclaredMethod("drop")
                    .addInterceptor(Listeners.of(DBCollectionDropInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                target.getDeclaredMethod("findAndModify", "com.mongodb.DBObject", "com.mongodb.DBObject", "com.mongodb.DBObject", "boolean", "com.mongodb.DBObject", "boolean", "boolean", "com.mongodb.WriteConcern")
                    .addInterceptor(Listeners.of(DBCollectionFindAndModifyInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
                target.getDeclaredMethod("findAndModify", "com.mongodb.DBObject", "com.mongodb.DBObject", "com.mongodb.DBObject", "boolean", "com.mongodb.DBObject", "boolean", "boolean", "boolean", "long", "java.util.concurrent.TimeUnit", "com.mongodb.WriteConcern")
                    .addInterceptor(Listeners.of(DBCollectionFindAndModifyInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
                target.getDeclaredMethod("findAndModify", "com.mongodb.DBObject", "com.mongodb.DBObject", "com.mongodb.DBObject", "boolean", "com.mongodb.DBObject", "boolean", "boolean", "long", "java.util.concurrent.TimeUnit", "com.mongodb.WriteConcern")
                    .addInterceptor(Listeners.of(DBCollectionFindAndModifyInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                target.getDeclaredMethod("aggregate", "java.util.List", "com.mongodb.ReadPreference")
                    .addInterceptor(Listeners.of(DBCollectionAggregateInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
                target.getDeclaredMethod("aggregate", "java.util.List", "com.mongodb.AggregationOptions")
                    .addInterceptor(Listeners.of(DBCollectionAggregateInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                target.getDeclaredMethod("initializeOrderedBulkOperation")
                    .addInterceptor(Listeners.of(DBCollectionInitializeOrderedBulkOperationInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
                target.getDeclaredMethod("initializeUnorderedBulkOperation")
                    .addInterceptor(Listeners.of(DBCollectionInitializeUnorderedBulkOperationInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                target.getDeclaredMethod("count", "com.mongodb.DBObject", "com.mongodb.ReadPreference")
                    .addInterceptor(Listeners.of(DBCollectionCountInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                target.getDeclaredMethod("getDB")
                    .addInterceptor(Listeners.of(DBCollectionGetDBInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                target.getDeclaredMethod("createIndex", "com.mongodb.DBObject", "java.lang.String", "boolean")
                    .addInterceptor(Listeners.of(DBCollectionCreateIndexInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                target.getDeclaredMethod("dropIndexes", "java.lang.String")
                    .addInterceptor(Listeners.of(DBCollectionDropIndexesInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                target.getDeclaredMethod("distinct", "java.lang.String", "com.mongodb.DBObject", "com.mongodb.ReadPreference")
                    .addInterceptor(Listeners.of(DBCollectionDistinctInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                target.getDeclaredMethod("group", "com.mongodb.DBObject")
                    .addInterceptor(Listeners.of(DBCollectionGroupInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
                target.getDeclaredMethod("group", "com.mongodb.DBObject", "com.mongodb.DBObject", "com.mongodb.DBObject", "java.lang.String", "java.lang.String")
                    .addInterceptor(Listeners.of(DBCollectionGroupInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
                target.getDeclaredMethod("group", "com.mongodb.DBObject", "com.mongodb.DBObject", "com.mongodb.DBObject", "java.lang.String", "java.lang.String", "com.mongodb.ReadPreference")
                    .addInterceptor(Listeners.of(DBCollectionGroupInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
                target.getDeclaredMethod("group", "com.mongodb.GroupCommand", "com.mongodb.ReadPreference")
                    .addInterceptor(Listeners.of(DBCollectionGroupInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                target.getDeclaredMethod("explainAggregate", "java.util.List", "com.mongodb.AggregationOptions")
                    .addInterceptor(Listeners.of(DBCollectionExplainAggregateInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

//                target.getDeclaredMethods("insert", "update","find","drop", "findAndModify","aggregate",
//                    "count","createIndex","dropIndexes", "group", "explainAggregate", "remove", "getIndexInfo", "parallelScan")
//                    .addInterceptor(Listeners.of(DBCollectionTraceInterceptor.class, "TraceScope", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
            }
        });

        enhanceTemplate.enhance(this, "com.mongodb.DBCollectionImpl", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                target.getDeclaredMethod("remove", "com.mongodb.DBObject", "com.mongodb.WriteConcern", "com.mongodb.DBEncoder")
                    .addInterceptor(Listeners.of(DBCollectionRemoveInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                target.getDeclaredMethod("getIndexInfo")
                    .addInterceptor(Listeners.of(DBCollectionGetIndexInfoInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                target.getDeclaredMethod("parallelScan", "com.mongodb.ParallelScanOptions")
                    .addInterceptor(Listeners.of(DBCollectionParallelScanInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
            }
        });

        enhanceTemplate.enhance(this, "com.mongodb.MongoCollectionImpl", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                target.getDeclaredMethods(MongodbConstants.mongoCollectionList)
                    .addInterceptor(Listeners.of(MongoCollectionTraceInterceptor.class));
            }
        });

        enhanceTemplate.enhance(this, "com.mongodb.client.internal.MongoCollectionImpl", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                target.getDeclaredMethods(MongodbConstants.mongoCollectionList)
                    .addInterceptor(Listeners.of(MongoCollectionInternalTraceInterceptor.class));
            }
        });

        enhanceTemplate.enhance(this, "com.mongodb.operation.AggregateOperationImpl", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                target.getDeclaredMethods("defaultAggregateTarget")
                        .addInterceptor(Listeners.of(AggregateOperationImplInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

            }
        });

        return true;
    }

    @Override
    public void onUnload() throws Throwable {
        OperationAccessorFactory.destroy();
        Caches.clean();
    }

}
