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

import com.pamirs.attach.plugin.spring.web.SpringWebConstants;
import com.pamirs.pradar.InvokeContext;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.springframework.web.server.ServerWebExchange;


/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/3/24 11:38 上午
 */
public class SpringWebFilterChainInterceptor extends TraceInterceptorAdaptor {

    @Override
    public String getPluginName() {
        return "spring-web-forward";
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
        final InvokeContext context = exchange.getAttribute(SpringWebConstants.WEB_CONTEXT);
        if (context == null || context.isEmpty()) {
            return;
        }
        Pradar.setInvokeContext(context);
    }
}
