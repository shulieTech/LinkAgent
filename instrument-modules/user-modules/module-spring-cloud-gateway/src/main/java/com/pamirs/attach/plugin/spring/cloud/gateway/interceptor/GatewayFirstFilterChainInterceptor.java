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

package com.pamirs.attach.plugin.spring.cloud.gateway.interceptor;

import com.pamirs.attach.plugin.spring.cloud.gateway.SpringCloudGatewayConstants;
import com.pamirs.pradar.InvokeContext;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.BeforeTraceInterceptorAdapter;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ServerWebExchange;


public class GatewayFirstFilterChainInterceptor extends BeforeTraceInterceptorAdapter {

    final Logger logger = LoggerFactory.getLogger(GatewayFirstFilterChainInterceptor.class);

    @Override
    public String getPluginName() {
        return "spring-cloud-filter";
    }

    @Override
    protected boolean isClient(Advice advice) {
        return true;
    }

    @Override
    public int getPluginType() {
        return MiddlewareType.TYPE_RPC;
    }

    @Override
    public void beforeFirst(Advice advice) {
        final Object[] parameterArray = advice.getParameterArray();
        ServerWebExchange exchange = (ServerWebExchange) parameterArray[0];
        final InvokeContext context = exchange.getAttribute(SpringCloudGatewayConstants.GATEWAY_CONTEXT);
        if (context == null || context.isEmpty()) {
            Pradar.clearInvokeContext();
            return;
        }
        Pradar.setInvokeContext(context);
    }

    @Override
    public SpanRecord beforeTrace(final Advice advice) {
        return null;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        return null;
    }

}
