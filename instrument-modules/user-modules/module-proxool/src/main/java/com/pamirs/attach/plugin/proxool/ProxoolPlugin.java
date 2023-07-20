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
package com.pamirs.attach.plugin.proxool;

import com.pamirs.attach.plugin.proxool.interceptor.ConnectionPoolManagerCreateConnectionPoolInterceptor;
import com.pamirs.attach.plugin.proxool.interceptor.DataSourceGetConnectionCutoffInterceptor;
import com.pamirs.attach.plugin.proxool.interceptor.ProxoolDriverConnectInterceptor;
import com.pamirs.attach.plugin.proxool.interceptor.ProxyStatementInvokeInterceptor;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/8/17 10:13 上午
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "proxool", version = "1.0.0", author = "xiaobin@shulie.io", description = "proxool 数据源")
public class ProxoolPlugin extends ModuleLifecycleAdapter implements ExtensionModule {
    private static Logger logger = LoggerFactory.getLogger(ProxoolPlugin.class);

    @Override
    public boolean onActive() throws Throwable {
        ignoredTypesBuilder.ignoreClass("org.logicalcobwebs.proxool.");

        enhanceTemplate.enhance(this, "org.logicalcobwebs.proxool.ProxoolDataSource", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod getConnection = target.getDeclaredMethod("getConnection");
                getConnection.addInterceptor(Listeners.of(DataSourceGetConnectionCutoffInterceptor.class, "Proxool_Get_Connection_Scope", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
            }
        });

        enhanceTemplate.enhance(this, "org.logicalcobwebs.proxool.ProxyStatement", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod invoke = target.getDeclaredMethod("invoke",
                        "java.lang.Object", "java.lang.reflect.Method", "java.lang.Object[]");
                invoke.addInterceptor(Listeners.of(ProxyStatementInvokeInterceptor.class));
            }
        });


        //org.logicalcobwebs.proxool.ConnectionPoolManager.createConnectionPool
        enhanceTemplate.enhance(this, "org.logicalcobwebs.proxool.ConnectionPoolManager", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod createConnectionPool = target.getDeclaredMethod("createConnectionPool",
                        "org.logicalcobwebs.proxool.ConnectionPoolDefinition");
                createConnectionPool.addInterceptor(Listeners.of(ConnectionPoolManagerCreateConnectionPoolInterceptor.class));
            }
        });


        //org.logicalcobwebs.proxool.ProxoolDriver.connect
        enhanceTemplate.enhance(this, "org.logicalcobwebs.proxool.ProxoolDriver", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod getConnection = target.getDeclaredMethod("connect",
                        "java.lang.String", "java.util.Properties");
                getConnection.addInterceptor(Listeners.of(ProxoolDriverConnectInterceptor.class));
            }
        });

        return true;
    }
}
