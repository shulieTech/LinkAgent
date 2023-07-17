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
package com.pamirs.attach.plugin.atomikos;

import com.pamirs.attach.plugin.atomikos.interceptor.DataSourceGetConnectionCutoffInterceptor;
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
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/8/17 10:13 上午
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "atomikos", version = "1.0.0", author = "xiaobin@shulie.io", description = "atomikos 数据源")
public class AtomikosPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() throws Throwable {
        ignoredTypesBuilder.ignoreClass("com.atomikos.jdbc.");
        enhanceTemplate.enhance(this, "com.atomikos.jdbc.nonxa.AtomikosNonXADataSourceBean", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod getConnectionMethod = target.getDeclaredMethod("getConnection");
                getConnectionMethod.addInterceptor(Listeners.of(DataSourceGetConnectionCutoffInterceptor.class, "Atomikos_Get_Connection_Scope", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
            }
        });
        enhanceTemplate.enhance(this, "com.atomikos.jdbc.AbstractDataSourceBean", new EnhanceCallback() {
                @Override
                public void doEnhance(InstrumentClass target) {
                    InstrumentMethod getConnectionMethod = target.getDeclaredMethod("getConnection");
                    getConnectionMethod.addInterceptor(Listeners.of(DataSourceGetConnectionCutoffInterceptor.class, "Atomikos_Get_Connection_Scope", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
                }
            });
        return true;
    }

}
