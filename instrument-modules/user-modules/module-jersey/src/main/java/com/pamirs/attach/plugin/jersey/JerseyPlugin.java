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
package com.pamirs.attach.plugin.jersey;

import com.pamirs.attach.plugin.jersey.interceptor.server.ServerInterceptor;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import org.kohsuke.MetaInfServices;

@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "jersey", version = "1.0.0", author = "jirenhe@shulie.io", description = "Jersey 支持 server 和 client")
public class JerseyPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() throws Throwable {
        //底层用的jdk http, 不需要增强客户端
        /*enhanceTemplate.enhance(this, "org.glassfish.jersey.client.ClientRuntime", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod method = target.getDeclaredMethods("invoke");
                method.addInterceptor(Listeners.of(ClientInterceptor.class));
            }
        });*/

        ignoredTypesBuilder.ignoreClass("org.glassfish.jersey.grizzly2.httpserver.");

        enhanceTemplate.enhance(this, "org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer",
            new EnhanceCallback() {
                @Override
                public void doEnhance(InstrumentClass target) {
                    InstrumentMethod method = target.getDeclaredMethod("service",
                        "org.glassfish.grizzly.http.server.Request",
                        "org.glassfish.grizzly.http.server.Response");
                    method.addInterceptor(Listeners.of(ServerInterceptor.class));
                }
            });

        return true;
    }
}
