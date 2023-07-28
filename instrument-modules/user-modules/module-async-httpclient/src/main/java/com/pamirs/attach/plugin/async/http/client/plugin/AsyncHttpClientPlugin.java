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
package com.pamirs.attach.plugin.async.http.client.plugin;

import com.pamirs.attach.plugin.async.http.client.plugin.interceptor.NettyRequestSenderSendRequestInterceptor;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import org.kohsuke.MetaInfServices;

/**
 * @author angju
 * @date 2021/4/6 20:36
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = AsyncHttpClientConstants.MODULE_NAME, version = "1.0.0", author = "angju@shulie.io", description = "async httpclient")
public class AsyncHttpClientPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() {
        //org.asynchttpclient.netty.request.NettyRequestSender#sendRequest
        enhanceTemplate.enhance(this, "org.asynchttpclient.netty.request.NettyRequestSender", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {

                InstrumentMethod method = target.getDeclaredMethods("sendRequest");
                method.addInterceptor(Listeners.of(NettyRequestSenderSendRequestInterceptor.class));
            }
        });
        return true;
    }
}
