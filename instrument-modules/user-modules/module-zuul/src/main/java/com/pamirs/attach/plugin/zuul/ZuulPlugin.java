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
package com.pamirs.attach.plugin.zuul;

import com.pamirs.attach.plugin.zuul.interceptor.ZuulChannelReadInterceptor;
import com.pamirs.attach.plugin.zuul.interceptor.ZuulEventTriggerInterceptor;
import com.pamirs.attach.plugin.zuul.interceptor.ZuulFilterRunnerInterceptor;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2021/8/30 2:09 下午
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = ZuulConstants.MODULE_NAME, version = "1.0.0", author = "wanglinglong@shulie.io",
    description = "zuul网关")
public class ZuulPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    private final static Logger logger = LoggerFactory.getLogger(ZuulPlugin.class);

    //@Override
    //public boolean onActive() throws Throwable {
    //    enhanceTemplate.enhance(this, "com.netflix.zuul.netty.filter.ZuulFilterChainRunner", new EnhanceCallback() {
    //        @Override
    //        public void doEnhance(InstrumentClass target) {
    //            // filter(T) 这个方法开始trace 然后弹出，并且将trace存放到request上面
    //            InstrumentMethod filterBeforeMethod = target.getDeclaredMethod("filter",
    //                "com.netflix.zuul.message.ZuulMessage");
    //            filterBeforeMethod.addInterceptor(
    //                Listeners.of(ZuulBeforeInterceptor.class));
    //
    //            // filter(T,HttpContent) 从request上面拿出trace 并且endTrace
    //            InstrumentMethod filterMethod = target.getDeclaredMethod("filter",
    //                "com.netflix.zuul.message.ZuulMessage", "io.netty.handler.codec.http.HttpContent");
    //            filterMethod.addInterceptor(
    //                Listeners.of(ZuulInterceptor.class));
    //        }
    //    });
    //    return true;
    //}

    //@Override
    //public boolean onActive() throws Throwable {
    //    enhanceTemplate.enhance(this, "com.netflix.zuul.netty.filter.ZuulFilterChainRunner", new EnhanceCallback() {
    //        @Override
    //        public void doEnhance(InstrumentClass target) {
    //            //InstrumentMethod method = target.getDeclaredMethod("runFilters", "java.lang.Object", "java.util
    //            // .concurrent.atomic.AtomicInteger");
    //            InstrumentMethod method = target.getDeclaredMethod("runFilters", 1,
    //                "java.util.concurrent.atomic.AtomicInteger");
    //            method.addInterceptor(Listeners.of(ZuulBackupInterceptor.class));
    //        }
    //    });
    //    return true;
    //}

    @Override
    public boolean onActive() throws Throwable {
        enhanceTemplate.enhance(this, "com.netflix.zuul.netty.filter.ZuulFilterChainHandler", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod method = target.getDeclaredMethod("userEventTriggered",
                    "io.netty.channel.ChannelHandlerContext",
                    "java.lang.Object");
                method.addInterceptor(Listeners.of(ZuulEventTriggerInterceptor.class));
            }
        });

        enhanceTemplate.enhance(this, "com.netflix.zuul.netty.filter.ZuulFilterChainHandler", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod method = target.getDeclaredMethod("channelRead",
                    "io.netty.channel.ChannelHandlerContext",
                    "java.lang.Object");
                method.addInterceptor(Listeners.of(ZuulChannelReadInterceptor.class));
            }
        });

        enhanceTemplate.enhance(this, "com.netflix.zuul.netty.filter.ZuulFilterChainRunner", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod method = target.getDeclaredMethod("runFilters", 1,
                    "java.util.concurrent.atomic.AtomicInteger");
                method.addInterceptor(Listeners.of(ZuulFilterRunnerInterceptor.class));
            }
        });
        return true;
    }
}
