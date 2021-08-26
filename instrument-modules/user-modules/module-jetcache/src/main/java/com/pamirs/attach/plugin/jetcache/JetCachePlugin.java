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
package com.pamirs.attach.plugin.jetcache;


import com.pamirs.attach.plugin.jetcache.interceptor.EmbeddedCacheBuildKeyInterceptor;
import com.pamirs.attach.plugin.jetcache.interceptor.ExternalCacheBuildKeyInterceptor;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import org.kohsuke.MetaInfServices;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/8/19 10:26 上午
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = JetCacheConstants.MODULE_NAME, version = "1.0.0", author = "xiaobin@shulie.io",description = "阿里巴巴开源的本地缓存框架 jetcache")
public class JetCachePlugin extends ModuleLifecycleAdapter implements ExtensionModule {
    @Override
    public void onActive() throws Throwable {
        enhanceTemplate.enhance(this, "com.alicp.jetcache.embedded.AbstractEmbeddedCache", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod buildKeyMethod = target.getDeclaredMethod("buildKey", "java.lang.Object");
                buildKeyMethod.addInterceptor(Listeners.of(EmbeddedCacheBuildKeyInterceptor.class));
            }
        });

        enhanceTemplate.enhance(this, "com.alicp.jetcache.external.AbstractExternalCache", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod buildKeyMethod = target.getDeclaredMethod("buildKey", "java.lang.Object");
                buildKeyMethod.addInterceptor(Listeners.of(ExternalCacheBuildKeyInterceptor.class));
            }
        });


    }
}
