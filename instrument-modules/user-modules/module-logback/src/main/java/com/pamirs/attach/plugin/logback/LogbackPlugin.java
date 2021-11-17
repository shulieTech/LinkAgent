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
package com.pamirs.attach.plugin.logback;

import com.pamirs.attach.plugin.logback.interceptor.*;
import com.pamirs.attach.plugin.logback.utils.AppenderHolder;
import com.pamirs.attach.plugin.logback.utils.ClusterTestMarker;
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
 * @author minzhuo
 * @since 2020/8/13 4:10 下午
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = LogbackConstants.PLUGIN_NAME, version = "1.0.0", author = "minzhuo@shulie.io",
        description = "logback 业务日志与压测日志分流")
public class LogbackPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    private final static Logger logger = LoggerFactory.getLogger(LogbackPlugin.class);
    private boolean isBusinessLogOpen;
    private String bizShadowLogPath;

    @Override
    public boolean onActive() {
        this.isBusinessLogOpen = simulatorConfig.getBooleanProperty("pradar.biz.log.divider", false);
        this.bizShadowLogPath = simulatorConfig.getProperty("pradar.biz.log.divider.path", simulatorConfig.getLogPath());
        if (!isBusinessLogOpen) {
            if (logger.isInfoEnabled()) {
                logger.info("logback biz log divider switcher is not open. ignore enhanced logback.");
            }
            return false;
        }

        //改版后sift appender隔离不了，先不支持，有碰到再说
        // for sifting appender start
        /*enhanceTemplate.enhance(this, "ch.qos.logback.core.sift.SiftingAppenderBase", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {

                InstrumentMethod addAppenderMethod = target.getDeclaredMethods("append", "java.lang.Object");
                addAppenderMethod.addInterceptor(
                        Listeners.of(SiftingAppenderBaseInterceptor.class, new Object[]{isBusinessLogOpen}));
            }
        });

        enhanceTemplate.enhance(this, "ch.qos.logback.core.spi.AbstractComponentTracker", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {

                InstrumentMethod addAppenderMethod = target.getDeclaredMethods("getOrCreate", "java.lang.String", "long");
                addAppenderMethod.addInterceptor(
                        Listeners.of(ComponentTrackerInterceptor.class, new Object[]{isBusinessLogOpen, bizShadowLogPath}));
            }
        });*/
        // for sifting appender end

        enhanceTemplate.enhance(this, "ch.qos.logback.core.spi.AppenderAttachableImpl", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {

                InstrumentMethod addAppenderMethod = target.getDeclaredMethods("appendLoopOnAppenders");
                addAppenderMethod.addInterceptor(
                        Listeners.of(LogInterceptor.class, new Object[]{bizShadowLogPath}));
            }
        });
        return true;
    }

    @Override
    public void onUnload() throws Throwable {
        ClusterTestMarker.release();
        AppenderHolder.release();
    }
}
