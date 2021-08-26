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
package com.pamirs.attach.plugin.websphere;

import com.pamirs.attach.plugin.websphere.interceptor.SRTServletRequestStartAsyncInterceptor;
import com.pamirs.attach.plugin.websphere.interceptor.WebContainerHandleRequestInterceptor;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import org.kohsuke.MetaInfServices;

/**
 * @author vincent
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = WebsphereConstans.MODULE_NAME, version = "1.0.0", author = "xiaobin@shulie.io", description = "websphere web 容器")
public class WebspherePlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public void onActive() throws Throwable {

        enhanceTemplate.enhance(this, "com.ibm.ws.webcontainer.srt.SRTServletRequest", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                final InstrumentMethod startAsyncMethodEditor = target.getDeclaredMethod("startAsync", "javax.servlet.ServletRequest", "javax.servlet.ServletResponse");
                if (startAsyncMethodEditor != null) {
                    startAsyncMethodEditor.addInterceptor(Listeners.of(SRTServletRequestStartAsyncInterceptor.class));
                }
            }
        });

        enhanceTemplate.enhance(this, "com.ibm.ws.webcontainer.WSWebContainer", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                final InstrumentMethod handleMethodEditorBuilder = target.getDeclaredMethod("handleRequest", "com.ibm.websphere.servlet.request.IRequest", "com.ibm.websphere.servlet.response.IResponse");
                handleMethodEditorBuilder.addInterceptor(Listeners.of(WebContainerHandleRequestInterceptor.class));
            }
        });
    }

}
