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
package com.pamirs.attach.plugin.saturn;

import com.pamirs.attach.plugin.saturn.interceptor.SaturnJavaJobInterceptor;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import org.kohsuke.MetaInfServices;

/**
 * Create by xuyh at 2020/8/17 10:40.
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "saturn", version = "1.0.0", author = "xiaobin@shulie.io", description = "saturn 定时调用")
public class SaturnPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() throws Throwable {
        this.enhanceTemplate.enhance(this, "com.vip.saturn.job.java.SaturnJavaJob", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod method = target.getDeclaredMethod("doExecution",
                        "java.lang.String",
                        "java.lang.Integer",
                        "java.lang.String",
                        "com.vip.saturn.job.basic.SaturnExecutionContext",
                        "com.vip.saturn.job.java.JavaShardingItemCallable");
                method.addInterceptor(Listeners.of(SaturnJavaJobInterceptor.class));
            }
        });
        return true;
    }
}
