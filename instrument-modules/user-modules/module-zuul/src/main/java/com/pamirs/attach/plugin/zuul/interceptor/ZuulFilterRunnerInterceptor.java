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
package com.pamirs.attach.plugin.zuul.interceptor;

import java.util.Map;

import javax.annotation.Resource;

import com.netflix.zuul.message.http.HttpRequestInfo;
import com.pamirs.attach.plugin.zuul.ZuulConstants;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2021/8/30 2:34 下午
 */
public class ZuulFilterRunnerInterceptor extends AroundInterceptor {
    @Resource
    private DynamicFieldManager dynamicFieldManager;

    @Override
    public void doBefore(Advice advice) throws Throwable {
        // HttpResponseInfo对象也会走这个方法, 但我不需要关心response
        Object param = advice.getParameterArray()[0];
        if (!(param instanceof HttpRequestInfo)) {
            return;
        }

        HttpRequestInfo requestInfo = (HttpRequestInfo)advice.getParameterArray()[0];
        if (requestInfo == null) {
            return;
        }
        Map<String, String> context = dynamicFieldManager.getDynamicField(requestInfo,
            ZuulConstants.DYNAMIC_FIELD_CONTEXT);
        if (context == null) {
            return;
        }
        Pradar.setInvokeContext(context);
    }

    @Override
    public void doAfter(Advice advice) throws Throwable {
        // HttpResponseInfo对象也会走这个方法, 但我不需要关心response
        Object param = advice.getParameterArray()[0];
        if (!(param instanceof HttpRequestInfo)) {
            return;
        }
        if (!Pradar.hasInvokeContext()) {
            return;
        }

        Pradar.popInvokeContext();
    }

    @Override
    public void doException(Advice advice) throws Throwable {
        // HttpResponseInfo对象也会走这个方法, 但我不需要关心response
        Object param = advice.getParameterArray()[0];
        if (!(param instanceof HttpRequestInfo)) {
            return;
        }
        if (!Pradar.hasInvokeContext()) {
            return;
        }
        Pradar.popInvokeContext();
    }
}
