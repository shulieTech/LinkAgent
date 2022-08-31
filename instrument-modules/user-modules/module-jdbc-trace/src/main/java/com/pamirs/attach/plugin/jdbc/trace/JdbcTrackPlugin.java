/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pamirs.attach.plugin.jdbc.trace;

import com.pamirs.attach.plugin.jdbc.trace.interceptor.CreatePreparedStatementInterceptor;
import com.pamirs.attach.plugin.jdbc.trace.interceptor.PreparedStatementInterceptor;
import com.pamirs.attach.plugin.jdbc.trace.interceptor.PreparedStatementSetParamsInterceptor;
import com.pamirs.attach.plugin.jdbc.trace.interceptor.StatementTraceInterceptor;
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
 * @Author ocean_wll
 * @Date 2022/8/31 10:26
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "jdbc-trace", version = "1.0.0", author = "wanglinglong@shulie.io", description = "jdbc层面记录trace")
public class JdbcTrackPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() throws Throwable {
        enhanceTemplate.enhanceWithInterface(this, "java.sql.Statement", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod execute = target.getDeclaredMethods("execute", "executeBatch", "executeQuery",
                        "executeUpdate");
                execute.addInterceptor(Listeners.of(StatementTraceInterceptor.class, "jdbc-statement", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
            }
        });

        enhanceTemplate.enhanceWithInterface(this, "java.sql.Connection", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod prepareStatement = target.getDeclaredMethods("prepareStatement");
                prepareStatement.addInterceptor(Listeners.of(CreatePreparedStatementInterceptor.class, "Connection-prepareStatement", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
            }
        });

        enhanceTemplate.enhanceWithInterface(this, "java.sql.PreparedStatement", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod setParamsMethod = target.getDeclaredMethods("set*");
                setParamsMethod.addInterceptor(Listeners.of(PreparedStatementSetParamsInterceptor.class,
                        "prepareStatement-setParams", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                InstrumentMethod execute = target.getDeclaredMethods("execute", "executeQuery", "executeUpdate");
                execute.addInterceptor(Listeners.of(PreparedStatementInterceptor.class, "jdbc-statement",
                        ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
            }
        });
        return true;
    }
}