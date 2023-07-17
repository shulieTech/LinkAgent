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
package com.pamirs.attach.plugin.hystrix;


import com.pamirs.attach.plugin.hystrix.interceptor.ConstructorInterceptor;
import com.pamirs.attach.plugin.hystrix.interceptor.InvokeInterceptor;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import org.kohsuke.MetaInfServices;

/**
 * hystrix 支持模块
 *
 * @author xiaobin.zfb | xiaobin@shulie.io
 * @since 2020/8/19 10:26 上午
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = HystrixConstants.MODULE_NAME, version = "1.0.0", author = "xiaobin@shulie.io",description = "hystrix 熔断框架")
public class HystrixPlugin extends ModuleLifecycleAdapter implements ExtensionModule {
    @Override
    public boolean onActive() throws Throwable {
        ignoredTypesBuilder.ignoreClass("com.netflix.hystrix.");

        enhanceTemplate.enhance(this, "com.netflix.hystrix.strategy.concurrency.HystrixContextScheduler$HystrixContextSchedulerWorker", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod constructors = target.getConstructors();
                constructors.addInterceptor(Listeners.of(ConstructorInterceptor.class));

                InstrumentMethod scheduleMethod1 = target.getDeclaredMethod("schedule", "rx.functions.Action0");
                scheduleMethod1.addInterceptor(Listeners.of(InvokeInterceptor.class));

                InstrumentMethod scheduleMethod2 = target.getDeclaredMethod("schedule", "rx.functions.Action0", "long", "java.util.concurrent.TimeUnit");
                scheduleMethod2.addInterceptor(Listeners.of(InvokeInterceptor.class));
            }
        });

        enhanceTemplate.enhance(this, "com.netflix.hystrix.strategy.concurrency.HystrixContexSchedulerAction", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {

                InstrumentMethod constructor1 = target.getConstructor("com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy", "rx.functions.Action0");
                constructor1.addInterceptor(Listeners.of(ConstructorInterceptor.class));

                InstrumentMethod callMethod = target.getDeclaredMethod("call");
                callMethod.addInterceptor(Listeners.of(InvokeInterceptor.class));
            }
        });

        enhanceTemplate.enhance(this, "rx.internal.operators.OnSubscribeDefer", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {

                InstrumentMethod constructors = target.getConstructors();
                constructors.addInterceptor(Listeners.of(ConstructorInterceptor.class));

                InstrumentMethod callMethod = target.getDeclaredMethod("call", "rx.Subscriber");
                callMethod.addInterceptor(Listeners.of(InvokeInterceptor.class));
            }
        });

        return true;
    }
}
