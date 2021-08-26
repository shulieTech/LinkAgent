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

import javax.annotation.Resource;

import com.pamirs.attach.plugin.logback.Constants;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Auther: vernon
 * @Date: 2020/12/8 00:12
 * @Description:
 */
public class AppenderRouterInterceptor extends CutoffInterceptorAdaptor {
    private final static Logger logger = LoggerFactory.getLogger(AppenderRouterInterceptor.class);

    protected boolean isBusinessLogOpen;
    protected String bizShadowLogPath;

    @Resource
    protected DynamicFieldManager manager;

    public AppenderRouterInterceptor(boolean isBusinessLogOpen, String bizShadowLogPath) {
        this.isBusinessLogOpen = isBusinessLogOpen;
        this.bizShadowLogPath = bizShadowLogPath;
    }

    @Override
    public CutOffResult cutoff0(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (!isBusinessLogOpen) {
            return CutOffResult.PASSED;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("entering appender routing plugin..，class = {}", advice.getTarget().getClass().getName());
        }
        if (advice.getTarget().getClass().getName().equals("ch.qos.logback.classic.AsyncAppender")) {
            //AsyncAppender是影子和业务共用的，所有的都要pass
            return CutOffResult.PASSED;
        }
        if (advice.getTarget().getClass().getName().equals("ch.qos.logback.classic.sift.SiftingAppender")) {
            //SiftingAppender 直接pass
            return CutOffResult.PASSED;
        }
        //classloader 冲突，所以使用反射
        String name = Reflect.on(advice.getTarget()).call("getName").get();
        if (isClusterTest(args[0])) {
            //压测流量放行压测的appender
            if (name.startsWith(Pradar.CLUSTER_TEST_PREFIX)) {
                return CutOffResult.PASSED;
            }
            return CutOffResult.cutoff(null);
        } else {
            //业务流量放行业务的appender
            if (name.startsWith(Pradar.CLUSTER_TEST_PREFIX)) {
                return CutOffResult.cutoff(null);
            }
            return CutOffResult.PASSED;
        }
    }

    private boolean isClusterTest(Object event) {
        if (Pradar.isClusterTest()) {
            return true;
        }
        return manager.getDynamicField(event, Constants.ASYNC_CLUSTER_TEST_MARK_FIELD, false);
    }
}
