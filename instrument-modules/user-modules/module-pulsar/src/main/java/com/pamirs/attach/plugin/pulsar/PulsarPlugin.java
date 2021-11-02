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
package com.pamirs.attach.plugin.pulsar;

import com.pamirs.attach.plugin.pulsar.interceptor.PulsarTraceConsumerInterceptor;
import com.pamirs.attach.plugin.pulsar.interceptor.PulsarTraceProducerInterceptor;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import org.kohsuke.MetaInfServices;

/**
 * Create by xuyh at 2020/6/23 11:33.
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = PulsarConstants.PLUGIN_NAME, version = "1.0.0", author = "xiaobin@shulie.io",description = "pulsar 消息中间件")
public class PulsarPlugin extends ModuleLifecycleAdapter implements ExtensionModule {
    @Override
    public boolean onActive() throws Throwable {
        enhanceTemplate.enhance(this, "org.apache.pulsar.client.impl.ProducerImpl", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod method = target.getDeclaredMethod("sendAsync", "org.apache.pulsar.client.api.Message", "org.apache.pulsar.client.impl.SendCallback");
                method.addInterceptor(Listeners.of(PulsarTraceProducerInterceptor.class));
            }
        });
        enhanceTemplate.enhance(this, "org.apache.pulsar.client.impl.ConsumerBase", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod method = target.getDeclaredMethod("beforeConsume", "org.apache.pulsar.client.api.Message");
                method.addInterceptor(Listeners.of(PulsarTraceConsumerInterceptor.class));
            }
        });
        return true;
    }
}
