/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pamirs.attach.plugin.spring.web.interceptor;

import com.pamirs.attach.plugin.common.web.RequestTracer;
import com.pamirs.attach.plugin.spring.web.SpringWebConstants;
import com.pamirs.attach.plugin.spring.web.trace.ServerHttpRequestTracer;
import com.pamirs.pradar.InvokeContext;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/3/24 11:21 上午
 */
public class WebHandlerDecoratorInterceptor extends TraceInterceptorAdaptor {
    private final RequestTracer<ServerHttpRequest, ServerHttpResponse> requestTracer;

    public WebHandlerDecoratorInterceptor() {
        requestTracer = new ServerHttpRequestTracer();
    }

    @Override
    public String getPluginName() {
        return SpringWebConstants.MODULE_NAME;
    }

    @Override
    public int getPluginType() {
        return SpringWebConstants.PLUGIN_TYPE;
    }

    @Override
    protected boolean isClient(Advice advice) {
        return true;
    }

    @Override
    public void beforeFirst(Advice advice) {
        final Object[] args = advice.getParameterArray();
        ServerWebExchange arg = (ServerWebExchange) args[0];
        if (arg.getAttributes().get(SpringWebConstants.WEB_CONTEXT) != null) {
            return;
        }
        ServerHttpRequest serverHttpRequest = arg.getRequest();
        requestTracer.startTrace(serverHttpRequest, null, SpringWebConstants.MODULE_NAME);
        final InvokeContext invokeContext = Pradar.getInvokeContext();
        if (invokeContext == null) {
            return;
        }
        arg.getAttributes().put(SpringWebConstants.WEB_CONTEXT, invokeContext);
    }


    @Override
    public void afterLast(Advice advice) {
        if (Pradar.hasInvokeContext()) {
            Pradar.popInvokeContext();
        }
    }

    @Override
    public void exceptionLast(Advice advice) {
        if (Pradar.hasInvokeContext()) {
            Pradar.popInvokeContext();
        }
    }

}