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
package com.pamirs.attach.plugin.log4j;

import com.pamirs.attach.plugin.log4j.interceptor.v1.AppenderV1RegisterInterceptor;
import com.pamirs.attach.plugin.log4j.interceptor.v1.AppenderV1RouterInterceptor;
import com.pamirs.attach.plugin.log4j.interceptor.v2.AppenderRegisterAttachInterceptor;
import com.pamirs.attach.plugin.log4j.interceptor.v2.AppenderRouterInterceptor;
import com.pamirs.attach.plugin.log4j.interceptor.v2.async.AsyncTestMarkSetInterceptor;
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
@ModuleInfo(id = Log4jConstants.PLUGIN_NAME, version = "1.0.0", author = "minzhuo@shulie.io",
    description = "log4j 业务日志与影子日志分流框架")
public class Log4jPlugin extends ModuleLifecycleAdapter implements ExtensionModule {
    private final static Logger logger = LoggerFactory.getLogger(Log4jPlugin.class);
    private boolean isBusinessLogOpen;
    private String bizShadowLogPath;

    @Override
    public boolean onActive() {
        ignoredTypesBuilder.ignoreClass("org.apache.log4j.");

        this.isBusinessLogOpen = simulatorConfig.getBooleanProperty("pradar.biz.log.divider", false);
        this.bizShadowLogPath = simulatorConfig.getProperty("pradar.biz.log.divider.path", simulatorConfig.getLogPath());
        if (!isBusinessLogOpen) {
            logger.info("log4j biz log divider switcher is not open. ignore enhanced log4j.");
            return false;
        }
        addV1AppenderAndRouter();
        addV2AppenderAndRouter();

        addV2AsyncPradarMark();
        return true;
    }

    private void addV1AppenderAndRouter() {
        //org.apache.log4j.Category.addAppender
        enhanceTemplate.enhance(this, "org.apache.log4j.Category",
            new EnhanceCallback() {
                @Override
                public void doEnhance(InstrumentClass target) {
                    InstrumentMethod method = target.getDeclaredMethods("addAppender");
                    method.addInterceptor(Listeners.of(
                        AppenderV1RegisterInterceptor.class, new Object[] {isBusinessLogOpen, bizShadowLogPath}));
                }
            });
        //org.apache.log4j.AppenderSkeleton.doAppend
        enhanceTemplate.enhance(this, "org.apache.log4j.AppenderSkeleton",
            new EnhanceCallback() {
                @Override
                public void doEnhance(InstrumentClass target) {
                    InstrumentMethod method = target.getDeclaredMethod(
                        "doAppend", "org.apache.log4j.spi.LoggingEvent"
                    );
                    method.addInterceptor(Listeners.of(
                        AppenderV1RouterInterceptor.class, new Object[] {isBusinessLogOpen, bizShadowLogPath}));
                }
            });
    }

    private void addV2AppenderAndRouter() {
        //=============v2
        //org.apache.logging.log4j.core.config.AppenderControl.tryCallAppender
        enhanceTemplate.enhance(this, "org.apache.logging.log4j.core.config.AppenderControl",
            new EnhanceCallback() {
                @Override
                public void doEnhance(InstrumentClass target) {
                    InstrumentMethod method = target.getDeclaredMethod("tryCallAppender",
                        "org.apache.logging.log4j.core.LogEvent");
                    method.addInterceptor(
                        Listeners.of(
                            AppenderRouterInterceptor.class, new Object[] {isBusinessLogOpen, bizShadowLogPath})
                    );
                }
            });

        // org.apache.logging.log4j.core.config.LoggerConfig.log
        enhanceTemplate.enhance(this, "org.apache.logging.log4j.core.config.LoggerConfig",
            new EnhanceCallback() {
                @Override
                public void doEnhance(InstrumentClass target) {
                    InstrumentMethod method = target.getDeclaredMethods("log");
                    method.addInterceptor(Listeners.of(AppenderRegisterAttachInterceptor.class,
                        new Object[] {isBusinessLogOpen, bizShadowLogPath}));
                }
            });
    }

    private void addV2AsyncPradarMark() {

        enhanceTemplate.enhance(this, "org.apache.logging.log4j.spi.AbstractLogger",
            new EnhanceCallback() {
                @Override
                public void doEnhance(InstrumentClass target) {
                    target.getDeclaredMethod("logMessageSafely",
                            "java.lang.String",
                            "org.apache.logging.log4j.Level",
                            "org.apache.logging.log4j.Marker",
                            "org.apache.logging.log4j.message.Message",
                            "java.lang.Throwable").
                        addInterceptor(Listeners.of(AsyncTestMarkSetInterceptor.class));
                }
            });
    }

}
