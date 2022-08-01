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
package com.pamirs.attach.plugin.apache.kafka.stream;

import com.pamirs.attach.plugin.apache.kafka.stream.interceptor.KStreamMapProcessorProcessInterceptor;
import com.pamirs.attach.plugin.apache.kafka.stream.interceptor.KStreamPeekProcessorProcessInterceptor;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import org.kohsuke.MetaInfServices;

/**
 * @author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @since 2021-05-07 11:40
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "kafka-stream", version = "1.0.0", author = "angju@shulie.io", description = "apache kafka-stream 消息中间件")
public class KafkaStreamPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() throws Throwable {
        return addHookRegisterInterceptor();
    }

    private boolean addHookRegisterInterceptor() {
        this.enhanceTemplate.enhance(this, "org.apache.kafka.streams.kstream.internals.KStreamMap$KStreamMapProcessor", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod processMethod = target.getDeclaredMethod("process", "java.lang.Object", "java.lang.Object");
                processMethod.addInterceptor(Listeners.of(KStreamMapProcessorProcessInterceptor.class));
            }
        });

        this.enhanceTemplate.enhance(this, "org.apache.kafka.streams.kstream.internals.KStreamPeek$KStreamPeekProcessor", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod processMethod = target.getDeclaredMethod("process", "java.lang.Object", "java.lang.Object");
                processMethod.addInterceptor(Listeners.of(KStreamPeekProcessorProcessInterceptor.class));
            }
        });

//        this.enhanceTemplate.enhance(this, "org.apache.kafka.streams.KafkaStreams", new EnhanceCallback() {
//            @Override
//            public void doEnhance(InstrumentClass target) {
//                InstrumentMethod processMethod = target.getDeclaredMethod("close", "long", "java.util.concurrent.TimeUnit");
//                processMethod.addInterceptor(Listeners.of(KafkaStreamsCloseInterceptor.class));
//            }
//        });

        return true;
    }


}
