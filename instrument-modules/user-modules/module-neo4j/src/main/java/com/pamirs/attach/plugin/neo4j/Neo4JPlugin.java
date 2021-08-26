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

import com.pamirs.attach.plugin.neo4j.interceptors.Neo4jSessionConstructorInterceptor;
import com.pamirs.attach.plugin.neo4j.interceptors.Neo4jSessionOperationCutOffInterceptor;
import com.pamirs.attach.plugin.neo4j.interceptors.Neo4jSessionOperationTraceInterceptor;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.listener.Listeners;
import org.kohsuke.MetaInfServices;

/**
 * @author wangjian
 * @since 2020/7/27 17:21
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = Neo4JConstants.MODULE_NAME, version = "1.0.0", author = "wangjian@shulie.io", description = "neo4j 图库")
public class Neo4JPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public void onActive() throws Throwable {
        // org.neo4j.ogm.session.Neo4jSession.Neo4jSession
        this.enhanceTemplate.enhance(this, "org.neo4j.ogm.session.Neo4jSession", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                // 针对session初始化监听
                target.getConstructor("org.neo4j.ogm.MetaData", "org.neo4j.ogm.driver.Driver")
                        .addInterceptor(Listeners.of(Neo4jSessionConstructorInterceptor.class));

                target.getDeclaredMethods(Neo4JConstants.SESSION_OPERATIONS)
                        .addInterceptor(Listeners.of(Neo4jSessionOperationTraceInterceptor.class));

                target.getDeclaredMethods(Neo4JConstants.SESSION_OPERATIONS_NO_LOAD_ALL)
                        .addInterceptor(Listeners.of(Neo4jSessionOperationCutOffInterceptor.class));
            }
        });
    }
}
