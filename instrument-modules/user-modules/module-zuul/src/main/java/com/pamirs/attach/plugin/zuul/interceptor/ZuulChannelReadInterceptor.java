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
import com.netflix.zuul.message.http.HttpRequestMessage;
import com.netflix.zuul.message.http.HttpResponseInfo;
import com.pamirs.attach.plugin.common.web.RequestTracer;
import com.pamirs.attach.plugin.zuul.ZuulConstants;
import com.pamirs.attach.plugin.zuul.tracer.ZuulMessageTracer;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import io.netty.channel.ChannelHandlerContext;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2021/8/30 2:34 下午
 */
public class ZuulChannelReadInterceptor extends AroundInterceptor {

    @Resource
    private DynamicFieldManager dynamicFieldManager;

    private final RequestTracer<HttpRequestInfo, HttpResponseInfo> requestTracer;

    public ZuulChannelReadInterceptor() {
        this.requestTracer = new ZuulMessageTracer();
    }

    @Override
    public void doBefore(Advice advice) throws Throwable {
        ChannelHandlerContext context = (ChannelHandlerContext)advice.getParameterArray()[0];
        Object msg = advice.getParameterArray()[1];
        if (context == null || msg == null) {
            return;
        }

        if (msg instanceof HttpRequestMessage) {
            HttpRequestMessage message = (HttpRequestMessage)msg;

            dynamicFieldManager.setDynamicField(context, ZuulConstants.TRIGGER_CONTEXT_REQUEST, message);

            requestTracer.startTrace(message, null, ZuulConstants.MODULE_NAME);

            Map<String, String> pradarContext = Pradar.getInvokeContextMap();
            dynamicFieldManager.setDynamicField(context, ZuulConstants.DYNAMIC_FIELD_CONTEXT, pradarContext);
            dynamicFieldManager.setDynamicField(msg, ZuulConstants.DYNAMIC_FIELD_CONTEXT, pradarContext);
        }
    }

    @Override
    public void doAfter(Advice advice) throws Throwable {
        if (!Pradar.hasInvokeContext()) {
            return;
        }
        Pradar.popInvokeContext();
    }

    @Override
    public void doException(Advice advice) throws Throwable {
        if (!Pradar.hasInvokeContext()) {
            return;
        }
        Pradar.popInvokeContext();
    }
}
