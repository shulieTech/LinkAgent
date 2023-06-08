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
package com.pamirs.attach.plugin.feign;

import com.pamirs.attach.plugin.feign.interceptor.EurekaRestRequestInterceptor;
import com.pamirs.attach.plugin.feign.interceptor.FeignMockInterceptor;
import com.pamirs.attach.plugin.feign.interceptor.GuavaCacheSkipInterceptor;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import org.kohsuke.MetaInfServices;

/**
 * @Author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @package: com.pamirs.attach.plugin.feign
 * @Date 2021/6/7 1:55 下午
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = FeignConstants.PLUGIN_NAME, version = "1.0.0", author = "xiaobin@shulie.io", description = "feign")
public class FeignPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() throws Throwable {

        EnhanceCallback feignInvocationHandler = new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod enqueueMethod = target.getDeclaredMethod("invoke", "java.lang.Object", "java.lang.reflect.Method", "java.lang.Object[]");
                enqueueMethod.addInterceptor(Listeners.of(FeignMockInterceptor.class));
            }
        };

        this.enhanceTemplate.enhance(this, "feign.ReflectiveFeign$FeignInvocationHandler", feignInvocationHandler);
        this.enhanceTemplate.enhance(this, "feign.hystrix.HystrixInvocationHandler", feignInvocationHandler);
        this.enhanceTemplate.enhance(this, "com.alibaba.cloud.sentinel.feign.SentinelInvocationHandler", feignInvocationHandler);
/*
        this.enhanceTemplate.enhance(this, "feign.SynchronousMethodHandler", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod enqueueMethod = target.getDeclaredMethod("executeAndDecode", "feign.RequestTemplate", "feign.Request$Options");
                enqueueMethod.addInterceptor(Listeners.of(FeignDataPassInterceptor.class));
            }
        });*/

        this.enhanceTemplate.enhance(this, "org.springframework.cloud.netflix.eureka.http.RestTemplateEurekaHttpClient", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod declaredMethods = target.getDeclaredMethods("sendHeartBeat", "statusUpdate", "getApplicationsInternal", "getApplication", "getInstance");
                declaredMethods.addInterceptor(Listeners.of(EurekaRestRequestInterceptor.class));
            }
        });

        this.enhanceTemplate.enhance(this, "com.netflix.loadbalancer.LoadBalancerStats", new EnhanceCallback() {

            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod declaredMethods = target.getDeclaredMethods("addServer", "getServerStats");
                declaredMethods.addInterceptor(Listeners.of(GuavaCacheSkipInterceptor.class));
            }
        });
        return true;
    }

}
