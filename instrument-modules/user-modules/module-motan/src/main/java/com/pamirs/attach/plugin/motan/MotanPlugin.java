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
package com.pamirs.attach.plugin.motan;

import com.pamirs.attach.plugin.motan.interceptor.AbstractRefererCallInterceptor;
import com.pamirs.attach.plugin.motan.interceptor.ProviderMessageRouterCallInterceptor;
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
 * @Date 2020/8/12 10:37 下午
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = MotanConstants.MODULE_NAME, version = "1.0.0", author = "xiaobin@shulie.io",description = "motan 远程调用框架,微博开源")
public class MotanPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() throws Throwable {
        enhanceTemplate.enhance(this, "com.weibo.api.motan.rpc.AbstractReferer", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod callMethod = target.getDeclaredMethod("call", "com.weibo.api.motan.rpc.Request");
                callMethod.addInterceptor(Listeners.of(AbstractRefererCallInterceptor.class));

            }
        });

        enhanceTemplate.enhance(this, "com.weibo.api.motan.transport.ProviderMessageRouter", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod method = target.getDeclaredMethod("handle", "com.weibo.api.motan.transport.Channel", "java.lang.Object");
                method.addInterceptor(Listeners.of(ProviderMessageRouterCallInterceptor.class));
            }
        });
        return true;
    }
}
