/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pamirs.attach.plugin.spring.web;

import com.pamirs.attach.plugin.spring.web.interceptor.HttpWebHandlerAdapterInterceptor;
import com.pamirs.attach.plugin.spring.web.interceptor.SpringWebFilterChainInterceptor;
import com.pamirs.attach.plugin.spring.web.interceptor.WebHandlerDecoratorInterceptor;
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
 * @Description spring web 插件
 * @Author ocean_wll
 * @Date 2022/3/24 11:18 上午
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = SpringWebConstants.MODULE_NAME, version = "1.0.0", author = "wanglinglong@shulie.io",
        description = "spring-web")
public class SpringWebPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() throws Throwable {
        this.enhanceTemplate.enhance(this, "org.springframework.web.server.handler.WebHandlerDecorator",
                new EnhanceCallback() {
                    @Override
                    public void doEnhance(InstrumentClass target) {
                        final InstrumentMethod handle = target.getDeclaredMethod("handle",
                                "org.springframework.web.server.ServerWebExchange");
                        handle.addInterceptor(
                                Listeners.of(WebHandlerDecoratorInterceptor.class, "SpringWeb",
                                        ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
                    }
                });

        this.enhanceTemplate.enhance(this, "org.springframework.web.server.handler.DefaultWebFilterChain",
                new EnhanceCallback() {
                    @Override
                    public void doEnhance(InstrumentClass target) {
                        final InstrumentMethod filter = target.getDeclaredMethod("filter",
                                "org.springframework.web.server.ServerWebExchange");
                        filter.addInterceptor(Listeners.of(SpringWebFilterChainInterceptor.class, "SpringWeb-forward",
                                ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
                    }
                });

        this.enhanceTemplate.enhance(this, "org.springframework.web.server.adapter.HttpWebHandlerAdapter",
                new EnhanceCallback() {
                    @Override
                    public void doEnhance(InstrumentClass target) {
                        final InstrumentMethod filter = target.getDeclaredMethod("logResponse",
                                "org.springframework.web.server.ServerWebExchange");
                        filter.addInterceptor(Listeners.of(HttpWebHandlerAdapterInterceptor.class, "SpringWeb-handle",
                                ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
                    }
                });


        return true;
    }
}
