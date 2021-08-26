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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pamirs.attach.plugin.logback.utils.AppenderHolder;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Auther: vernon
 * @Date: 2020/12/7 23:37
 * @Description:
 */
public class AppenderRegisterInterceptor extends AroundInterceptor {

    protected boolean isBusinessLogOpen;
    protected String bizShadowLogPath;

    private final Logger log = LoggerFactory.getLogger(ComponentTrackerInterceptor.class);

    private Map<Object, Long> lastCheckTimes = new HashMap<Object, Long>();

    public AppenderRegisterInterceptor(boolean isBusinessLogOpen, String bizShadowLogPath) {
        this.isBusinessLogOpen = isBusinessLogOpen;
        this.bizShadowLogPath = bizShadowLogPath;
    }

    @Override
    public void doBefore(Advice advice) {
        if (!isBusinessLogOpen) {
            return;
        }
        Object appenderAttachable = advice.getTarget();
        if (!isNeedScan(appenderAttachable)) { //因为下面都是反射调用，并且不需要每次都扫描一遍，间隔扫描提升性能
            return;
        }
        List appenderList = Reflect.on(appenderAttachable).get("appenderList");
        if (appenderList == null) {
            return;
        }
        ClassLoader bizClassLoader = appenderAttachable.getClass().getClassLoader();
        for (Object appender : appenderList) {
            String appenderName = Reflect.on(appender).call("getName").get();
            if (appenderName.startsWith(Pradar.CLUSTER_TEST_PREFIX)) {
                continue;
            }
            try {
                Object ptAppender = AppenderHolder.getOrCreatePtAppender(bizClassLoader, appender, bizShadowLogPath);
                if (ptAppender != null) {
                    Reflect.on(appenderAttachable).call("addAppender", ptAppender).get();
                }
            } catch (Exception e) {
                log.error("get pt appender fail!", e);
            }
        }
        lastCheckTimes.put(appenderAttachable, System.currentTimeMillis());
    }

    private boolean isNeedScan(Object appenderAttachable) {
        Long lastCheckTime = lastCheckTimes.get(appenderAttachable);
        if (lastCheckTime == null) {
            lastCheckTime = -1L;
        }
        return (System.currentTimeMillis() - lastCheckTime) > 30000;
    }
}
