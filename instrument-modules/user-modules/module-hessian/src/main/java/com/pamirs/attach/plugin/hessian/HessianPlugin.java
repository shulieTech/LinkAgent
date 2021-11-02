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
package com.pamirs.attach.plugin.hessian;

import com.pamirs.attach.plugin.hessian.interceptor.*;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import org.kohsuke.MetaInfServices;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/7/15 8:01 下午
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "hessian", version = "1.0.0", author = "xiaobin@shulie.io", description = "hessian 远程调用框架")
public class HessianPlugin extends ModuleLifecycleAdapter implements ExtensionModule {
    @Override
    public boolean onActive() throws Throwable {
        enhanceTemplate.enhance(this, "com.caucho.hessian.client.HessianProxy", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod addRequestHeadersMethod = target.getDeclaredMethod("addRequestHeaders", "com.caucho.hessian.client.HessianConnection");
                addRequestHeadersMethod.addInterceptor(Listeners.of(HessianProxyAddRequestHeadersInterceptor.class));

            }
        });

        enhanceTemplate.enhance(this, "com.caucho.hessian.client.HessianProxyFactory", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod method = target.getDeclaredMethod("create", "java.lang.Class", "java.net.URL", "java.lang.ClassLoader");
                method.addInterceptor(Listeners.of(HessianProxyFactoryCreateInterceptor.class));
            }
        });

        enhanceTemplate.enhance(this, "com.caucho.hessian.server.HessianServlet", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod serviceMethod = target.getDeclaredMethod("service", "javax.servlet.ServletRequest", "javax.servlet.ServletResponse");
                serviceMethod.addInterceptor(Listeners.of(HessianServletServiceInterceptor.class));
                serviceMethod.addInterceptor(Listeners.of(HessianServletWrapperRequestInterceptor.class));
            }
        });

        enhanceTemplate.enhance(this, "com.caucho.burlap.server.BurlapServlet", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod serviceMethod = target.getDeclaredMethod("service", "javax.servlet.ServletRequest", "javax.servlet.ServletResponse");
                serviceMethod.addInterceptor(Listeners.of(BurlapServletServiceInterceptor.class));
                serviceMethod.addInterceptor(Listeners.of(HessianServletWrapperRequestInterceptor.class));
            }
        });

        enhanceTemplate.enhance(this, "org.springframework.remoting.caucho.HessianServiceExporter", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {

                InstrumentMethod serviceMethod = target.getDeclaredMethod("handleRequest", "javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse");
                serviceMethod.addInterceptor(Listeners.of(HessianServiceExporterHandleRequestInterceptor.class));
                serviceMethod.addInterceptor(Listeners.of(HessianServletWrapperRequestInterceptor.class));
            }
        });
        return true;
    }
}
