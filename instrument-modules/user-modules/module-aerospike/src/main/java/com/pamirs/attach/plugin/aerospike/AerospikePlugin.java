/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.aerospike;

import com.pamirs.attach.plugin.aerospike.interceptor.*;
import com.pamirs.pradar.interceptor.Interceptors;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import com.shulie.instrument.simulator.api.scope.ExecutionPolicy;
import org.kohsuke.MetaInfServices;

/**
 * @author xiaobin.zfb
 * @since 2020/8/13 4:10 下午
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = AerospikeConstants.PLUGIN_NAME, version = "1.0.0", author = "xiaobin@shulie.io", description = "Aerospike NoSql 数据库")
public class AerospikePlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() throws Throwable {
        return addTransformers();
    }

    private boolean addTransformers() {
        this.enhanceTemplate.enhance(this, "com.aerospike.client.Key", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod method = target.getConstructors();
                method.addInterceptor(Listeners.of(KeyConstructorInterceptor.class, "KEY_SCOPE", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
            }
        });
        this.enhanceTemplate.enhance(this, "com.aerospike.client.AerospikeClient", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod method = target.getDeclaredMethods("put", "append", "prepend", "add", "delete", "touch", "exists", "exists", "get", "getHeader", "operate", "getLargeList", "getLargeMap", "getLargeStack", "execute", "query", "queryNode", "queryAggregate");
                method.addInterceptor(Listeners.of(AreospikeOperateInterceptor.class));

                InstrumentMethod scanNodeMethod = target.getDeclaredMethods("scanNode");
                scanNodeMethod.addInterceptor(Listeners.of(ScanNodeParameterWrapInterceptor.class));

                InstrumentMethod scanAllMethod = target.getDeclaredMethods("scanAll");
                scanAllMethod.addInterceptor(Listeners.of(ScanAllParameterWrapInterceptor.class));
            }
        });

        this.enhanceTemplate.enhance(this, "com.aerospike.client.async.AsyncClient", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod method = target.getDeclaredMethods("put", "append", "prepend", "add", "delete", "touch", "exists", "exists", "get", "getHeader", "operate", "getLargeList", "getLargeMap", "getLargeStack", "execute", "query", "queryNode", "queryAggregate");
                method.addInterceptor(Listeners.of(AreospikeOperateInterceptor.class));

                InstrumentMethod scanNodeMethod = target.getDeclaredMethods("scanNode");
                scanNodeMethod.addInterceptor(Listeners.of(ScanNodeParameterWrapInterceptor.class));

                scanNodeMethod.addInterceptor(Listeners.of(ScanNodeParameterWrapInterceptor.class));

                InstrumentMethod scanAllMethod = target.getDeclaredMethods("scanAll");
                scanAllMethod.addInterceptor(Listeners.of(ScanAllParameterWrapInterceptor.class));

                scanAllMethod.addInterceptor(Listeners.of(AreospikeScanAllInterceptor.class));
            }
        });

        ignoredTypesBuilder.ignoreClass("com.aerospike.client.");
        return true;
    }
}
