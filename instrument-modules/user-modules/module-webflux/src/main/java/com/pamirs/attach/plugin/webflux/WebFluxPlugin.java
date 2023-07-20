/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.webflux;


import com.pamirs.attach.plugin.webflux.common.WebFluxConstants;
import com.pamirs.attach.plugin.webflux.interceptor.*;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import org.kohsuke.MetaInfServices;

import java.util.ArrayList;
import java.util.List;


/**
 * @Auther: vernon
 * @Date: 2021/1/11 10:21
 * @Description:
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = WebFluxConstants.MODULE_NAME, version = "1.0.0", author = "angju@shulie.io", description = "webflux,spring cloud gateway 容器")
public class WebFluxPlugin extends ModuleLifecycleAdapter implements ExtensionModule {


    @Override
    public boolean onActive() throws Throwable {
        //org.springframework.cloud.gateway.handler.FilteringWebHandler$DefaultGatewayFilterChain.filter
//        enhanceTemplate.enhance(this, "org.springframework.cloud.gateway.handler.FilteringWebHandler$DefaultGatewayFilterChain", new EnhanceCallback() {
//            @Override
//            public void doEnhance(InstrumentClass target) {
//
//                final InstrumentMethod invokeHandler = target.getDeclaredMethod("filter", "org.springframework.web.server.ServerWebExchange"
//                        );
//                invokeHandler.addInterceptor(Listeners.of(InvokeHandlerInterceptor.class));
//
//
//            }
//        });
        ignoredTypesBuilder.ignoreClass("org.springframework.web.reactive.");

        enhanceTemplate.enhance(this, "org.springframework.web.reactive.DispatcherHandler", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {

                final InstrumentMethod invokeHandler = target.getDeclaredMethod("invokeHandler",
                        "org.springframework.web.server.ServerWebExchange",
                        "java.lang.Object"
                );
                invokeHandler.addInterceptor(Listeners.of(HandlerStartInterceptor.class));


                final InstrumentMethod handleResult = target.getDeclaredMethod("handleResult",
                        "org.springframework.web.server.ServerWebExchange",
                        "org.springframework.web.reactive.HandlerResult"
                );
                handleResult.addInterceptor(Listeners.of(HandlerResultInterceptor.class));


            }
        });

        enhanceTemplate.enhance(this, "org.springframework.http.server.reactive.AbstractServerHttpRequest", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {

                final InstrumentMethod constructor = target.getConstructor("java.net.URI",
                        "java.lang.String",
                        "org.springframework.http.HttpHeaders");
                constructor.addInterceptor(Listeners.of(AbstractServerHttpRequestInterceptor.class));
            }
        });


        enhanceTemplate.enhance(this, "org.springframework.http.ReadOnlyHttpHeaders", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                List<String> list = new ArrayList<String>();
                list.add("add");
                list.add("set");
                final InstrumentMethod methods = target.getDeclaredMethods(list);
                methods.addInterceptor(Listeners.of(HeaderMethodInterceptor.class));
            }
        });

//        enhanceTemplate.enhance(this, "org.springframework.web.reactive.function.client.DefaultClientRequestBuilder$BodyInserterRequest", new EnhanceCallback() {
//            @Override
//            public void doEnhance(InstrumentClass target) {
//                //final InstrumentMethod buildMethod = target.getConstructor("org.springframework.http.HttpMethod", "java.net.URI", "org.springframework.http.HttpHeaders", "org.springframework.util.MultiValueMap", "org.springframework.web.reactive.function.BodyInserter", "java.util.Map");
//                final InstrumentMethod buildMethod = target.getConstructors();
//                buildMethod.addInterceptor(Listeners.of(RequestBuildInterceptor.class));
//            }
//        });


        enhanceTemplate.enhance(this, "org.springframework.web.reactive.function.client.ExchangeFunctions$DefaultExchangeFunction", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                final InstrumentMethod invokeHandler = target.getDeclaredMethod("exchange",
                        "org.springframework.web.reactive.function.client.ClientRequest");
                invokeHandler.addInterceptor(Listeners.of(ExchangeFunInterceptor.class));
            }
        });


        return true;
    }


}
