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
package com.pamirs.attach.plugin.spring.cache;

import com.pamirs.attach.plugin.spring.cache.interceptor.ClusterTestCacheInterceptor;
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
@ModuleInfo(id = SpringCacheConstants.MODULE_NAME, version = "1.0.0", author = "xiaobin@shulie.io",description = "spring 自带的本地缓存")
public class SpringCachePlugin extends ModuleLifecycleAdapter implements ExtensionModule {
    @Override
    public void onActive() throws Throwable {
        /**
         * 只增强具体的Cache实现
         */
        addCacheClusterTestKeyWrapper("org.springframework.cache.concurrent.ConcurrentMapCache");
        addCacheClusterTestKeyWrapper("org.springframework.cache.ehcache.EhCacheCache");
        addCacheClusterTestKeyWrapper("org.springframework.cache.guava.GuavaCache");
        addCacheClusterTestKeyWrapper("org.springframework.cache.jcache.JCacheCache");

    }

    private void addCacheClusterTestKeyWrapper(String className) {
        this.enhanceTemplate.enhance(this, className, new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod getMethod = target.getDeclaredMethod("get", "java.lang.Object");
                getMethod.addInterceptor(Listeners.of(ClusterTestCacheInterceptor.class, "Spring-Cache-Scope", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                InstrumentMethod getMethod0 = target.getDeclaredMethod("get", "java.lang.Object", "java.lang.Class");
                getMethod0.addInterceptor(Listeners.of(ClusterTestCacheInterceptor.class, "Spring-Cache-Scope", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                InstrumentMethod putMethod = target.getDeclaredMethod("put", "java.lang.Object", "java.lang.Object");
                putMethod.addInterceptor(Listeners.of(ClusterTestCacheInterceptor.class, "Spring-Cache-Scope", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                InstrumentMethod putIfAbsentMethod = target.getDeclaredMethod("putIfAbsent", "java.lang.Object", "java.lang.Object");
                putIfAbsentMethod.addInterceptor(Listeners.of(ClusterTestCacheInterceptor.class, "Spring-Cache-Scope", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                InstrumentMethod evictMethod = target.getDeclaredMethod("evict", "java.lang.Object");
                evictMethod.addInterceptor(Listeners.of(ClusterTestCacheInterceptor.class, "Spring-Cache-Scope", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
            }
        });
    }
}
