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
package com.pamirs.attach.plugin.resin;

import com.pamirs.attach.plugin.resin.interceptor.HttpServletRequestImplStartAsyncInterceptor;
import com.pamirs.attach.plugin.resin.interceptor.ServletInvocationServiceInterceptor;
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
 * @author vincent
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "resin", version = "1.0.0", author = "xiaobin@shulie.io", description = "resin web 容器")
public class ResinPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() throws Throwable {

        enhanceTemplate.enhance(this, "com.caucho.server.http.HttpServletRequestImpl", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                final InstrumentMethod startAsyncMethodEditor = target.getDeclaredMethod("startAsync", "javax.servlet.ServletRequest", "javax.servlet.ServletResponse");
                if (startAsyncMethodEditor != null) {
                    startAsyncMethodEditor.addInterceptor(Listeners.of(HttpServletRequestImplStartAsyncInterceptor.class));
                }
            }
        });

        enhanceTemplate.enhance(this, "com.caucho.server.dispatch.ServletInvocation", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                final InstrumentMethod serviceMethodEditorBuilder = target.getDeclaredMethod("service", "javax.servlet.ServletRequest", "javax.servlet.ServletResponse");
                if (serviceMethodEditorBuilder != null) {
                    serviceMethodEditorBuilder.addInterceptor(Listeners.of(ServletInvocationServiceInterceptor.class, "RESIN_REQUEST", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
                }
            }
        });
        return true;
    }

}
