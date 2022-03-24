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
 * @Date 2022/3/24 1:56 下午
 */
public class HttpWebHandlerAdapterInterceptor extends TraceInterceptorAdaptor {

    private static final RequestTracer<ServerHttpRequest, ServerHttpResponse> requestTracer = new ServerHttpRequestTracer();


    @Override
    public String getPluginName() {
        return SpringWebConstants.MODULE_NAME;
    }

    @Override
    public int getPluginType() {
        return SpringWebConstants.PLUGIN_TYPE;
    }

    @Override
    public void afterLast(Advice advice) {
        final Object[] args = advice.getParameterArray();
        ServerWebExchange exchange = (ServerWebExchange) args[0];
        final InvokeContext context = exchange.getAttribute(SpringWebConstants.WEB_CONTEXT);
        if (context == null || context.isEmpty()) {
            return;
        }
        Pradar.setInvokeContext(context);
        requestTracer.endTrace(exchange.getRequest(), exchange.getResponse(), null);
    }
}
