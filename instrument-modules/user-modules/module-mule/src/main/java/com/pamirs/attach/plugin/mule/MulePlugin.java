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
package com.pamirs.attach.plugin.mule;

import com.pamirs.attach.plugin.mule.interceptor.MuleHttpRequestDispatcherFilterInterceptor;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import org.kohsuke.MetaInfServices;

/**
 * Create by xuyh at 2020/6/18 21:36.
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "mule", version = "1.0.0", author = "xiaobin@shulie.io",description = "mule 支持")
public class MulePlugin extends ModuleLifecycleAdapter implements ExtensionModule {
    @Override
    public void onActive() throws Throwable {
        enhanceTemplate.enhance(this, "org.mule.module.http.internal.listener.grizzly.GrizzlyRequestDispatcherFilter", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod method = target.getDeclaredMethod("handleRead", "org.glassfish.grizzly.filterchain.FilterChainContext");
                method.addInterceptor(Listeners.of(MuleHttpRequestDispatcherFilterInterceptor.class));
            }
        });
    }
}
