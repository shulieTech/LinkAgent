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

import com.netflix.netty.common.HttpLifecycleChannelHandler;
import com.netflix.netty.common.HttpLifecycleChannelHandler.CompleteEvent;
import com.netflix.netty.common.HttpRequestReadTimeoutEvent;
import com.netflix.zuul.message.http.HttpRequestInfo;
import com.netflix.zuul.message.http.HttpResponseInfo;
import com.netflix.zuul.netty.RequestCancelledEvent;
import com.pamirs.attach.plugin.common.web.RequestTracer;
import com.pamirs.attach.plugin.zuul.ZuulConstants;
import com.pamirs.attach.plugin.zuul.tracer.ZuulMessageTracer;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2021/8/30 2:34 下午
 */
public class ZuulEventTriggerInterceptor extends AroundInterceptor {

    @Resource
    private DynamicFieldManager dynamicFieldManager;

    private final RequestTracer<HttpRequestInfo, HttpResponseInfo> requestTracer;

    public ZuulEventTriggerInterceptor() {
        this.requestTracer = new ZuulMessageTracer();
    }

    //@Override
    //public void doBefore(Advice advice) throws Throwable {
    //    ChannelHandlerContext context = (ChannelHandlerContext)advice.getParameterArray()[0];
    //    Object event = advice.getParameterArray()[1];
    //    //如果不是开始事件则直接return
    //    if (context == null || !(event instanceof StartEvent)) {
    //        return;
    //    }
    //    HttpRequest request = ((StartEvent)event).getRequest();
    //    if (request == null) {
    //        return;
    //    }
    //    try {
    //        dynamicFieldManager.setDynamicField(context, ZuulConstants.TRIGGER_CONTEXT_REQUEST, request);
    //
    //        requestTracer.startTrace(request, null, ZuulConstants.MODULE_NAME);
    //
    //        Map<String, String> pradarContext = Pradar.getInvokeContextMap();
    //        dynamicFieldManager.setDynamicField(context, ZuulConstants.DYNAMIC_FIELD_CONTEXT, pradarContext);
    //    } finally {
    //        advice.mark(TraceInterceptorAdaptor.BEFORE_TRACE_SUCCESS);
    //    }
    //}

    @Override
    public void doAfter(Advice advice) throws Throwable {
        ChannelHandlerContext context = (ChannelHandlerContext)advice.getParameterArray()[0];
        Object event = advice.getParameterArray()[1];

        if (context == null) {
            return;
        }

        // 如果event不是这些终止的事件则直接return
        if (!(event instanceof CompleteEvent)
            && !(event instanceof HttpRequestReadTimeoutEvent)
            && !(event instanceof IdleStateEvent)
            && !(event instanceof RequestCancelledEvent)
            && !(event instanceof HttpLifecycleChannelHandler.RejectedPipeliningEvent)) {
            return;
        }

        HttpRequestInfo request = dynamicFieldManager.removeField(context, ZuulConstants.TRIGGER_CONTEXT_REQUEST);
        if (request == null) {
            return;
        }

        Map<String, String> pradarContext = dynamicFieldManager.removeField(context,
            ZuulConstants.DYNAMIC_FIELD_CONTEXT);
        if (pradarContext == null) {
            return;
        }
        Pradar.setInvokeContext(pradarContext);

        try {
            requestTracer.endTrace(request, null, null);
        } finally {
            Pradar.clearInvokeContext();
        }
    }

    @Override
    public void doException(Advice advice) throws Throwable {
        ChannelHandlerContext context = (ChannelHandlerContext)advice.getParameterArray()[0];
        if (context == null) {
            return;
        }

        if (!Pradar.hasInvokeContext()) {
            return;
        }

        Pradar.popInvokeContext();

        HttpRequestInfo request = dynamicFieldManager.removeField(context, ZuulConstants.TRIGGER_CONTEXT_REQUEST);
        if (request == null) {
            return;
        }
        try {
            requestTracer.endTrace(request, null, advice.getThrowable());
        } finally {
            advice.unMark(TraceInterceptorAdaptor.BEFORE_TRACE_SUCCESS);
            Pradar.clearInvokeContext();
        }
    }
}
