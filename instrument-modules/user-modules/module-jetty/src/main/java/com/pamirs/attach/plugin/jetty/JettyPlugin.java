package com.pamirs.attach.plugin.jetty;

import com.pamirs.attach.plugin.jetty.interceptor.*;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import org.kohsuke.MetaInfServices;

/**
 * @author fabing.zhaofb
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = JettyConstans.MODULE_NAME, version = "1.0.0", author = "xiaobin@shulie.io",description = "jetty 服务器，支持6.x~9.x")
public class JettyPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public void onActive() throws Throwable {
        addServerInterceptor();
    }

    private void addServerInterceptor() {
        enhanceTemplate.enhance(this, "org.eclipse.jetty.server.handler.HandlerWrapper", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                // 7.x ~ 9.x
                final InstrumentMethod handleMethod = target.getDeclaredMethod("handle", "java.lang.String", "org.eclipse.jetty.server.Request", "javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse");
                handleMethod.addInterceptor(Listeners.of(JettyServerHandleInterceptor.class));
            }
        });

        // jetty 6.x
        enhanceTemplate.enhance(this, "org.mortbay.jetty.Server", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod handleMethod = target.getDeclaredMethod("handle", "java.lang.String", "javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse", "int");
                handleMethod.addInterceptor(Listeners.of(Jetty6xServerHandleInterceptor.class));
            }
        });

        enhanceTemplate.enhance(this, "org.eclipse.jetty.server.Request", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                // Add async listener. Servlet 3.0
                final InstrumentMethod startAsyncMethod = target.getDeclaredMethod("startAsync");
                startAsyncMethod.addInterceptor(Listeners.of(RequestStartAsyncInterceptor.class));
                final InstrumentMethod startAsyncMethod0 = target.getDeclaredMethod("startAsync", "javax.servlet.ServletRequest", "javax.servlet.ServletResponse");
                startAsyncMethod0.addInterceptor(Listeners.of(RequestStartAsyncInterceptor.class));
            }
        });
    }

}
