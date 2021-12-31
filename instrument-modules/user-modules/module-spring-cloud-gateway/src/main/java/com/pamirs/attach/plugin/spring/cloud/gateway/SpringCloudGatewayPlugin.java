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
package com.pamirs.attach.plugin.spring.cloud.gateway;

import com.pamirs.attach.plugin.spring.cloud.gateway.interceptor.FilteringWebHandlerHandleInterceptor;
import com.pamirs.attach.plugin.spring.cloud.gateway.interceptor.GatewayFilterChainFilterInterceptor;
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
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/8/19 10:26 上午
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = SpringCloudGatewayConstants.MODULE_NAME, version = "1.0.0", author = "liqiyu@shulie.io",
    description = "spring cloud gateway")
public class SpringCloudGatewayPlugin extends ModuleLifecycleAdapter implements ExtensionModule {
    @Override
    public boolean onActive() throws Throwable {
        this.enhanceTemplate.enhance(this, "org.springframework.cloud.gateway.handler.FilteringWebHandler",
            new EnhanceCallback() {
                @Override
                public void doEnhance(InstrumentClass target) {
                    final InstrumentMethod handle = target.getDeclaredMethod("handle",
                        "org.springframework.web.server.ServerWebExchange");
                    handle.addInterceptor(
                        Listeners.of(FilteringWebHandlerHandleInterceptor.class, "SpringCloudGatewayScope",
                            ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
                }
            });

        this.enhanceTemplate.enhance(this, "org.springframework.cloud.gateway.filter.NettyRoutingFilter",
            new EnhanceCallback() {
                @Override
                public void doEnhance(InstrumentClass target) {
                    final InstrumentMethod filter = target.getDeclaredMethod("filter",
                        "org.springframework.web.server.ServerWebExchange",
                        "org.springframework.cloud.gateway.filter.GatewayFilterChain");
                    filter.addInterceptor(
                        Listeners.of(GatewayFilterChainFilterInterceptor.class, "SpringCloudGatewayFilterScope",
                            ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
                }
            });
        return true;
    }
}
