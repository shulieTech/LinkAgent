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
package com.pamirs.attach.plugin.springjms;

import com.pamirs.attach.plugin.springjms.interceptor.*;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import org.kohsuke.MetaInfServices;

/**
 * Created by xiaobin on 2016/12/15.
 */
@SuppressWarnings("all")
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = SpringJmsConstants.MODULE_NAME, version = "1.0.0", author = "xiaobin@shulie.io", description = "spring jms 支持")
public class SpringJmsPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() throws Throwable {
        enhanceTemplate.enhance(this, "org.springframework.jms.core.JmsTemplate", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod methodCreateProducer = target.getDeclaredMethod("createProducer",
                        "javax.jms.Session", "javax.jms.Destination");
                methodCreateProducer.addInterceptor(Listeners.of(SpringJmsTemplateInterceptor.class));

            }
        });

        enhanceTemplate.enhance(this, "org.springframework.jms.config.JmsListenerEndpointRegistrar", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod instrumentMethod = target.getDeclaredMethod("registerEndpoint", "org.springframework.jms.config.JmsListenerEndpoint", "org.springframework.jms.config.JmsListenerContainerFactory");
                instrumentMethod.addInterceptor(Listeners.of(SpringJmsListenerEndpointRegisterInterceptor.class));
            }
        });


        enhanceTemplate.enhance(this, "org.springframework.web.client.RestTemplate", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod instrumentMethod = target.getDeclaredMethod("doExecute",
                        "java.net.URI",
                        "org.springframework.http.HttpMethod",
                        "org.springframework.web.client.RequestCallback",
                        "org.springframework.web.client.ResponseExtractor"

                );
                instrumentMethod.addInterceptor(Listeners.of(RestTemplateInterceptor.class));
            }
        });
        enhanceTemplate.enhance(this, "org.springframework.http.client.support.HttpAccessor", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod instrumentMethod = target.getDeclaredMethod("createRequest",
                        "java.net.URI",
                        "org.springframework.http.HttpMethod"
                );
                instrumentMethod.addInterceptor(Listeners.of(RestTemplateInterceptor.class));
            }
        });

        // URI url, HttpMethod method
        enhanceTemplate.enhance(this, "org.springframework.http.client.AbstractClientHttpRequest", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod instrumentMethod = target.getDeclaredMethod("execute");
                instrumentMethod.addInterceptor(Listeners.of(AbstractClientHttpRequestInterceptor.class));
            }
        });


        enhanceTemplate.enhance(this, "org.springframework.jms.listener.AbstractPollingMessageListenerContainer", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod instrumentMethod = target.getDeclaredMethod("receiveAndExecute",
                        "java.lang.Object",
                        "javax.jms.Session", "javax.jms.MessageConsumer"
                );
                instrumentMethod.addInterceptor(Listeners.of(SpringTxInterceptor.class));
                InstrumentMethod receiveMessage = target.getDeclaredMethod("receiveMessage", "javax.jms.MessageConsumer");
                receiveMessage.addInterceptor(Listeners.of(SpringJmsReceiveMessageInterceptor.class));
            }
        });

        return true;

    }

}
