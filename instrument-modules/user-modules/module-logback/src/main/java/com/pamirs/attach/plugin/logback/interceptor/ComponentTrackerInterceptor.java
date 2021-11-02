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
package com.pamirs.attach.plugin.logback.interceptor;

import com.pamirs.attach.plugin.logback.utils.AppenderHolder;
import com.pamirs.attach.plugin.logback.utils.ClusterTestMarker;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.ResultInterceptorAdaptor;
import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/06/22 10:21 上午
 */
@ListenerBehavior(isFilterClusterTest = true)
public class ComponentTrackerInterceptor extends ResultInterceptorAdaptor {

    protected boolean isBusinessLogOpen;
    protected String bizShadowLogPath;

    private final Logger log = LoggerFactory.getLogger(ComponentTrackerInterceptor.class);

    public ComponentTrackerInterceptor(boolean isBusinessLogOpen, String bizShadowLogPath) {
        this.isBusinessLogOpen = isBusinessLogOpen;
        this.bizShadowLogPath = bizShadowLogPath;
    }

    @Override
    public Object getResult0(Advice advice) {
        if (!Pradar.isClusterTest()) {
            return advice.getReturnObj();
        }
        if (!this.isBusinessLogOpen) {
            return advice.getReturnObj();
        }
        Object appender = advice.getReturnObj();
        try {
            Object ptAppender = AppenderHolder.getOrCreatePtAppender(appender.getClass().getClassLoader(),
                appender, bizShadowLogPath);
            if (ClusterTestMarker.isClusterTestThenClear()) {
                return ptAppender == null ? appender : ptAppender;
            }
        } catch (Exception e) {
            log.error("get pt appender fail!", e);
        }
        return appender;
    }
}
