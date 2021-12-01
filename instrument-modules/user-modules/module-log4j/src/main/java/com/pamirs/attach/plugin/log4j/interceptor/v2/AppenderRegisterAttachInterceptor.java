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
package com.pamirs.attach.plugin.log4j.interceptor.v2;

import java.util.Map;
import java.util.Map.Entry;

import com.pamirs.attach.plugin.log4j.destroy.Log4jDestroy;
import com.pamirs.attach.plugin.log4j.interceptor.v2.appender.creator.ShadowAppenderCreatorFacade;
import com.pamirs.attach.plugin.log4j.interceptor.v2.holder.Cache;
import com.pamirs.attach.plugin.log4j.interceptor.v2.holder.Cache.Consumer;
import com.pamirs.attach.plugin.log4j.interceptor.v2.holder.Cache.Function;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mocheng
 * @since 2021/09/14 10:49
 */
@Destroyable(Log4jDestroy.class)
public class AppenderRegisterAttachInterceptor extends AroundInterceptor {

    static final Logger logger = LoggerFactory.getLogger(AppenderRegisterAttachInterceptor.class);
    protected boolean isBusinessLogOpen;
    protected String shadowLogPath;

    public AppenderRegisterAttachInterceptor(boolean isBusinessLogOpen, String bizShadowLogPath) {
        this.isBusinessLogOpen = isBusinessLogOpen;
        this.shadowLogPath = bizShadowLogPath;
    }

    @Override
    public void doBefore(Advice advice) throws Throwable {
        if (!isBusinessLogOpen) {
            return;
        }
        LoggerConfig config = (LoggerConfig)advice.getTarget();
        if (config == null) {
            return;
        }
        //每个LoggerConfig只执行一次
        Cache.LoggerConfigCache.makeSureConfigOnce(config, new Consumer<LoggerConfig>() {
            @Override
            public void accept(LoggerConfig loggerConfig) {
                Map<String, Appender> appenderMap = loggerConfig.getAppenders();
                for (Entry<String, Appender> entry : appenderMap.entrySet()) {
                    Appender appender = entry.getValue();
                    String appenderName = entry.getKey();
                    if (Pradar.isClusterTestPrefix(appenderName)) {
                        continue;
                    }
                    Appender ptAppender = Cache.AppenderCache.computeIfAbsent(appender, new Function<Appender, Appender>() {
                        @Override
                        public Appender apply(Appender appender) {
                            Appender result = ShadowAppenderCreatorFacade.createShadowAppenderCreator(appender, shadowLogPath);
                            result.start();
                            return result;
                        }
                    });
                    if (ptAppender != null) {
                        if (!appenderMap.containsKey(ptAppender.getName())) {
                            loggerConfig.addAppender(ptAppender, null, null);
                        }
                    }
                }
            }
        });
    }

}
