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
package com.pamirs.attach.plugin.hikariCP;

import com.pamirs.attach.plugin.hikariCP.interceptor.DataSourceConnectionInterceptor;
import com.pamirs.attach.plugin.hikariCP.interceptor.DataSourceConstructorInterceptor;
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
 * @Auther: vernon
 * @Date: 2020/3/28 02:02
 * @Description:
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "hikaricp", version = "1.0.0", author = "xiaobin@shulie.io", description = "hikariCP 数据源")
public class HikariCPPlugin extends ModuleLifecycleAdapter implements ExtensionModule {
    private static Logger logger = LoggerFactory.getLogger(HikariCPPlugin.class.getName());

    @Override
    public boolean onActive() throws Throwable {
        //com.zaxxer.hikari.HikariDataSource.getConnection
        enhanceTemplate.enhance(this, "com.zaxxer.hikari.HikariDataSource",
                new EnhanceCallback() {
                    @Override
                    public void doEnhance(InstrumentClass clazz) {
                        InstrumentMethod method = clazz.getDeclaredMethod("getConnection");
                        method.addInterceptor(Listeners.of(DataSourceConnectionInterceptor.class, "Hikari_Get_Connection_Scope", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                        InstrumentMethod constructor = clazz.getConstructor("com.zaxxer.hikari.HikariConfig");
                        constructor.addInterceptor(Listeners.of(DataSourceConstructorInterceptor.class));
                    }
                });

        enhanceTemplate.enhance(this, "com.zaxxer.hikari.HikariConfig",
                new EnhanceCallback() {
                    @Override
                    public void doEnhance(InstrumentClass clazz) {
                        InstrumentMethod method = clazz.getDeclaredMethod("validate");
                        method.addInterceptor(Listeners.of(DataSourceConstructorInterceptor.class));
                    }
                });
        return true;
    }
}
