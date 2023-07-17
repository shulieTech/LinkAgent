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
package com.pamirs.attach.plugin.caffeine;

import com.pamirs.attach.plugin.caffeine.interceptor.*;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import org.kohsuke.MetaInfServices;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/03/22 11:34 上午
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = CaffeineConstants.MODULE_NAME, version = "1.0.0", author = "jirenhe@shulie.io",description = "Caffeine本地缓存")
public class CaffeinePlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() throws Throwable {
        ignoredTypesBuilder.ignoreClass("om.github.benmanes.caffeine.");

        enhanceTemplate.enhance(this, new EnhanceCallback() {
                @Override
                public void doEnhance(InstrumentClass target) {

                    //ConcurrentMap接口所有方法
                    addConcurrentMapMethodInterceptor(target);

                    //特有的方法
                    target.getDeclaredMethod("getIfPresent", "java.lang.Object", "boolean")
                        .addInterceptor(Listeners.of(FirstKeyInterceptor.class));

                    target.getDeclaredMethod("put", "java.lang.Object", "java.lang.Object", "boolean")
                        .addInterceptor(Listeners.of(FirstKeyInterceptor.class));

                    target.getDeclaredMethod("computeIfAbsent", "java.lang.Object", "java.util.function.Function",
                        "boolean", "boolean")
                        .addInterceptor(Listeners.of(ComputeIfAbsentInterceptor.class));

                    target.getDeclaredMethod("remap", "java.lang.Object", "java.util.function.BiFunction")
                        .addInterceptor(Listeners.of(FirstKeyWithBiFunctionInterceptor.class));

                    target.getDeclaredMethod("getAllPresent", "java.lang.Iterable")
                        .addInterceptor(Listeners.of(GetAllInterceptor.class));

                    target.getDeclaredMethod("compute", "java.lang.Object", "java.util.function.BiFunction",
                        "boolean", "boolean", "boolean")
                        .addInterceptor(Listeners.of(ComputeInterceptor.class));

                }
            },
            "com.github.benmanes.caffeine.cache.UnboundedLocalCache");

        enhanceTemplate.enhance(this, new EnhanceCallback() {
                @Override
                public void doEnhance(InstrumentClass target) {

                    //ConcurrentMap接口所有方法
                    addConcurrentMapMethodInterceptor(target);

                    //特有的方法
                    target.getDeclaredMethod("getIfPresent", "java.lang.Object", "boolean")
                        .addInterceptor(Listeners.of(FirstKeyInterceptor.class));

                    target.getDeclaredMethod("getIfPresentQuietly", "java.lang.Object", "long[]")
                        .addInterceptor(Listeners.of(FirstKeyInterceptor.class));

                    target.getDeclaredMethod("put", "java.lang.Object", "java.lang.Object",
                        "com.github.benmanes.caffeine.cache.Expiry", "boolean", "boolean")
                        .addInterceptor(Listeners.of(FirstKeyInterceptor.class));

                    target.getDeclaredMethod("computeIfAbsent", "java.lang.Object", "java.util.function.Function",
                        "boolean", "boolean")
                        .addInterceptor(Listeners.of(ComputeIfAbsentInterceptor.class));

                    target.getDeclaredMethod("remap", "java.lang.Object", "java.lang.Object",
                        "java.util.function.BiFunction", "long[]", "boolean")
                        .addInterceptor(Listeners.of(FirstKeyInterceptor.class));

                    target.getDeclaredMethod("getAllPresent", "java.lang.Iterable")
                        .addInterceptor(Listeners.of(GetAllInterceptor.class));

                    target.getDeclaredMethod("compute", "java.lang.Object", "java.util.function.BiFunction",
                        "boolean", "boolean", "boolean")
                        .addInterceptor(Listeners.of(ComputeInterceptor.class));
                    target.getDeclaredMethod("compute", "java.lang.Object", "java.util.function.BiFunction",
                        "boolean", "boolean")
                        .addInterceptor(Listeners.of(ComputeInterceptor.class));

                    target.getDeclaredMethods("tryExpireAfterRead","expireAfterRead", "expireAfterUpdate", "expireAfterCreate")
                                    .addInterceptor(Listeners.of(CacheLoaderInterceptor.class));

                }
            },
            "com.github.benmanes.caffeine.cache.BoundedLocalCache");

        enhanceTemplate.enhance(this, "com.github.benmanes.caffeine.cache.CacheLoader", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod asyncReload = target.getDeclaredMethod("asyncReload", "java.lang.Object", "java.lang.Object", "java.util.concurrent.Executor");
                asyncReload.addInterceptor(Listeners.of(CacheLoaderInterceptor.class));
            }
        });

        return true;
    }

    private void addConcurrentMapMethodInterceptor(InstrumentClass target) {

        target.getDeclaredMethod("remove", "java.lang.Object", "java.lang.Object")
            .addInterceptor(Listeners.of(FirstKeyInterceptor.class));

        target.getDeclaredMethod("remove", "java.lang.Object")
            .addInterceptor(Listeners.of(FirstKeyInterceptor.class));

        target.getDeclaredMethod("replace", "java.lang.Object", "java.lang.Object", "java.lang.Object")
            .addInterceptor(Listeners.of(FirstKeyInterceptor.class));

        target.getDeclaredMethod("replace", "java.lang.Object", "java.lang.Object")
            .addInterceptor(Listeners.of(FirstKeyInterceptor.class));

        target.getDeclaredMethod("putIfAbsent", "java.lang.Object", "java.lang.Object")
            .addInterceptor(Listeners.of(FirstKeyInterceptor.class));

        target.getDeclaredMethod("getOrDefault", "java.lang.Object", "java.lang.Object")
            .addInterceptor(Listeners.of(FirstKeyInterceptor.class));

        target.getDeclaredMethod("computeIfAbsent", "java.lang.Object", "java.util.function.Function")
            .addInterceptor(Listeners.of(FirstKeyWithBiFunctionInterceptor.class));

        target.getDeclaredMethod("computeIfPresent", "java.lang.Object", "java.util.function.BiFunction")
            .addInterceptor(Listeners.of(FirstKeyWithBiFunctionInterceptor.class));

        target.getDeclaredMethod("compute", "java.lang.Object", "java.util.function.BiFunction")
            .addInterceptor(Listeners.of(FirstKeyWithBiFunctionInterceptor.class));

        target.getDeclaredMethod("merge", "java.lang.Object", "java.lang.Object", "java.util.function.BiFunction")
            .addInterceptor(Listeners.of(FirstKeyWithBiFunctionInterceptor.class));

        target.getDeclaredMethod("get", "java.lang.Object")
            .addInterceptor(Listeners.of(FirstKeyInterceptor.class));

        target.getDeclaredMethod("put", "java.lang.Object", "java.lang.Object")
            .addInterceptor(Listeners.of(FirstKeyInterceptor.class));

        target.getDeclaredMethod("isEmpty")
            .addInterceptor(Listeners.of(IsEmptyInterceptor.class));

        target.getDeclaredMethod("entrySet")
            .addInterceptor(Listeners.of(EntrySetInterceptor.class));

        target.getDeclaredMethod("putAll", "java.util.Map")
            .addInterceptor(Listeners.of(PutAllInterceptor.class));

        target.getDeclaredMethod("keySet")
            .addInterceptor(Listeners.of(KeySetInterceptor.class));

        target.getDeclaredMethod("containsKey", "java.lang.Object")
            .addInterceptor(Listeners.of(FirstKeyInterceptor.class));
    }
}
