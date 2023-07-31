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
package com.pamirs.attach.plugin.apache.cxf;

import com.pamirs.attach.plugin.apache.cxf.interceptor.InvokeInterceptor;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import org.kohsuke.MetaInfServices;

/**
 * @author jiangjibo
 * @date 2021/11/11 5:26 下午
 * @description:
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "apache-cxf", version = "1.0.0", author = "yubo@shulie.io", description = "apache cxf 远程调用框架")
public class CxfPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() {
        this.enhanceTemplate.enhance(this, "org.apache.cxf.endpoint.ClientImpl", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                final InstrumentMethod invokeMethod = target.getDeclaredMethod("setParameters", "java.lang.Object[]", "org.apache.cxf.message.Message");
                if (invokeMethod != null) {
                    invokeMethod.addInterceptor(Listeners.of(InvokeInterceptor.class));
                }
            }
        });
        return true;
    }
}
