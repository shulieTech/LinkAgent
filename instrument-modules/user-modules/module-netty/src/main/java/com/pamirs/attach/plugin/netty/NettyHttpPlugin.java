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

import com.pamirs.attach.plugin.netty.interceptor.ChannelPipelineWriteInterceptor;
import com.pamirs.attach.plugin.netty.interceptor.ChannelPromiseAddListenerInterceptor;
import com.pamirs.attach.plugin.netty.interceptor.HttpEncoderInterceptor;
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

@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = NettyConstants.MODULE_NAME, version = "1.0.0", author = "xiaobin@shulie.io",description = "netty 框架，支持 http入口")
public class NettyHttpPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() throws Throwable {
        this.enhanceTemplate.enhance(this, "io.netty.channel.DefaultChannelPipeline", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                final InstrumentMethod writeMethod1 = target.getDeclaredMethod("write", "java.lang.Object");
                writeMethod1.addInterceptor(Listeners.of(ChannelPipelineWriteInterceptor.class, NettyConstants.SCOPE_WRITE, ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                final InstrumentMethod writeMethod2 = target.getDeclaredMethod("write", "java.lang.Object", "io.netty.channel.ChannelPromise");
                writeMethod2.addInterceptor(Listeners.of(ChannelPipelineWriteInterceptor.class, NettyConstants.SCOPE_WRITE, ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                final InstrumentMethod writeAndFlushMethod1 = target.getDeclaredMethod("writeAndFlush", "java.lang.Object");
                writeAndFlushMethod1.addInterceptor(Listeners.of(ChannelPipelineWriteInterceptor.class, NettyConstants.SCOPE_WRITE, ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                final InstrumentMethod writeAndFlushMethod2 = target.getDeclaredMethod("writeAndFlush", "java.lang.Object", "io.netty.channel.ChannelPromise");
                writeAndFlushMethod2.addInterceptor(Listeners.of(ChannelPipelineWriteInterceptor.class, NettyConstants.SCOPE_WRITE, ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
            }
           
        });

        this.enhanceTemplate.enhance(this, "io.netty.channel.DefaultChannelPromise", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                final InstrumentMethod addListenerMethod1 = target.getDeclaredMethod("addListener", "io.netty.util.concurrent.GenericFutureListener");
                addListenerMethod1.addInterceptor(Listeners.of(ChannelPromiseAddListenerInterceptor.class, NettyConstants.SCOPE, ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                final InstrumentMethod addListenerMethod2 = target.getDeclaredMethod("addListeners", "io.netty.util.concurrent.GenericFutureListener[]");
                addListenerMethod2.addInterceptor(Listeners.of(ChannelPromiseAddListenerInterceptor.class, NettyConstants.SCOPE, ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
            }
        });

        this.enhanceTemplate.enhance(this, "io.netty.handler.codec.http.HttpObjectEncoder", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod method = target.getDeclaredMethod("encode", "io.netty.channel.ChannelHandlerContext", "java.lang.Object", "java.util.List");
                method.addInterceptor(Listeners.of(HttpEncoderInterceptor.class));
            }
        });
        return true;
    }

}
