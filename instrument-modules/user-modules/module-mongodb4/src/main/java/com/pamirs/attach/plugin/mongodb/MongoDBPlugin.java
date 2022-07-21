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

import com.pamirs.attach.plugin.mongodb.interceptor.AggregateOperationImplInterceptor;
import com.pamirs.attach.plugin.mongodb.interceptor.DelegateOperationExecutorInterceptor;
import com.pamirs.attach.plugin.mongodb.interceptor.DelegateOperationExecutorTraceInterceptor;
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
@ModuleInfo(id = "mongodb4", version = "1.0.0", author = "angju@shulie.io", description = "mongdb 数据库")
public class MongoDBPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() throws Throwable {

        /**
         * 因为这个插件与mongodb322插件冲突，所以当mongodb322插件启用时此插件禁用
         */

        //默认不用4
        boolean use = false;
        String mongodb4Enabled = System.getProperty("mongodb4.enabled");
        if (mongodb4Enabled == null){
            mongodb4Enabled = simulatorConfig.getProperty("mongodb4.enabled");
        }

        if (StringUtils.isNotBlank(mongodb4Enabled) && Boolean.valueOf(mongodb4Enabled)){
            use = true;
        }

        if (!use) {
            return false;
        }

        enhanceTemplate.enhance(this, "com.mongodb.client.internal.MongoClientDelegate$DelegateOperationExecutor", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                //for new version 4.x
                InstrumentMethod instrumentMethod_3 = target.getDeclaredMethod("execute",
                        "com.mongodb.internal.operation.ReadOperation", "com.mongodb.ReadPreference",
                        "com.mongodb.ReadConcern", "com.mongodb.client.ClientSession");

                InstrumentMethod instrumentMethod_4 = target.getDeclaredMethod("execute",
                        "com.mongodb.internal.operation.WriteOperation", "com.mongodb.ReadConcern",
                        "com.mongodb.client.ClientSession");
                instrumentMethod_3.addInterceptor(Listeners.of(DelegateOperationExecutorTraceInterceptor.class, "execute_trace", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
                instrumentMethod_4.addInterceptor(Listeners.of(DelegateOperationExecutorTraceInterceptor.class, "execute_trace", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                instrumentMethod_3.addInterceptor(Listeners.of(DelegateOperationExecutorInterceptor.class));
                instrumentMethod_4.addInterceptor(Listeners.of(DelegateOperationExecutorInterceptor.class));
            }
        });

        enhanceTemplate.enhance(this, "com.mongodb.internal.operation.AggregateOperationImpl", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                target.getDeclaredMethods("defaultAggregateTarget")
                        .addInterceptor(Listeners.of(AggregateOperationImplInterceptor.class, "DBCollection", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

            }
        });

//        enhanceTemplate.enhance(this, "com.mongodb.client.internal.MongoCollectionImpl", new EnhanceCallback() {
//            @Override
//            public void doEnhance(InstrumentClass target) {
//                target.getDeclaredMethods(MongodbConstants.mongoCollectionList)
//                        .addInterceptor(Listeners.of(MongoCollectionInternalTraceInterceptor.class));
//            }
//        });

        return true;
    }

    @Override
    public void onUnload() throws Throwable {
        OperationAccessorFactory.destroy();
    }

}
