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
import com.netflix.zuul.message.http.HttpResponseInfo;
import com.pamirs.attach.plugin.common.web.RequestTracer;
import com.pamirs.attach.plugin.zuul.ZuulConstants;
import com.pamirs.attach.plugin.zuul.tracer.ZuulMessageTracer;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2021/8/30 2:34 下午
 */
public class ZuulInterceptor extends AroundInterceptor {
    @Resource
    private DynamicFieldManager dynamicFieldManager;

    private final static Logger logger = LoggerFactory.getLogger(ZuulInterceptor.class);

    private final RequestTracer<HttpRequestInfo, HttpResponseInfo> requestTracer;

    public ZuulInterceptor() {
        this.requestTracer = new ZuulMessageTracer();
    }

    @Override
    public void doBefore(Advice advice) throws Throwable {
        // HttpResponseInfo对象也会走这个方法, 但我不需要关心response
        Object param = advice.getParameterArray()[0];
        if (!(param instanceof HttpRequestInfo)) {
            return;
        }

        HttpRequestInfo requestInfo = (HttpRequestInfo)advice.getParameterArray()[0];
        if (requestInfo == null) {
            logger.warn("Invalid target object, The com.netflix.zuul.message.http.HttpRequestInfo is null");
            return;
        }
        Map<String, String> context = dynamicFieldManager.removeField(requestInfo,
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

        try {
            HttpRequestInfo requestInfo = (HttpRequestInfo)advice.getParameterArray()[0];
            if (requestInfo == null) {
                logger.warn("Invalid target object, The com.netflix.zuul.message.http.HttpRequestInfo is null");
                return;
            }

            requestTracer.endTrace(requestInfo, null, null);
        } finally {
            Pradar.clearInvokeContext();
        }
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
        try {
            HttpRequestInfo requestInfo = (HttpRequestInfo)advice.getParameterArray()[0];
            if (requestInfo == null) {
                logger.warn("Invalid target object, The com.netflix.zuul.message.http.HttpRequestInfo is null");
                return;
            }

            requestTracer.endTrace(requestInfo, null, advice.getThrowable());
        } finally {
            advice.unMark(TraceInterceptorAdaptor.BEFORE_TRACE_SUCCESS);
            Pradar.clearInvokeContext();
        }
    }
}
