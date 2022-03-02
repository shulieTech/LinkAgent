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
package com.pamirs.attach.plugin.netty;

import com.pamirs.attach.plugin.netty.interceptor.HashedWheelTimerNewTimeoutInterceptor;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.listener.Listeners;
import org.kohsuke.MetaInfServices;

@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = NettyTimeWheelConstants.PLUGIN_NAME, version = "1.0.0", author = "xiaobin@shulie.io",
    description = "netty time wheel")
public class NettyTimeWheelPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() throws Throwable {
        this.enhanceTemplate.enhance(this, "io.netty.util.HashedWheelTimer", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                target.getDeclaredMethods("newTimeout")
                    .addInterceptor(Listeners.of(HashedWheelTimerNewTimeoutInterceptor.class));
            }

        });

        return true;
    }

}
