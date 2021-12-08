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
package com.pamirs.attach.plugin.jersey.interceptor.server;

import com.pamirs.attach.plugin.common.web.RequestTracer;
import com.pamirs.attach.plugin.jersey.JerseyConstants;
import com.pamirs.attach.plugin.jersey.interceptor.server.trace.JerseyRequestTracer;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/13 3:03 下午
 */
public class ServerInterceptor extends AroundInterceptor {
    private final static Logger logger = LoggerFactory.getLogger(ServerInterceptor.class);

    private final RequestTracer<Request, Response> requestTracer;

    public ServerInterceptor() {
        this.requestTracer = new JerseyRequestTracer();
    }

    @Override
    public void doBefore(Advice advice) throws Throwable {
        Request request = (Request)advice.getParameterArray()[0];
        Response response = (Response)advice.getParameterArray()[1];
        boolean isStartTrace = false;
        try {
            isStartTrace = requestTracer.startTrace(request, response, JerseyConstants.PLUGIN_NAME);
        } finally {
            if (isStartTrace) {
                advice.mark(TraceInterceptorAdaptor.BEFORE_TRACE_SUCCESS);
            }
        }
    }

    @Override
    public void doAfter(Advice advice) throws Throwable {
        try {
            if (!advice.hasMark(TraceInterceptorAdaptor.BEFORE_TRACE_SUCCESS)) {
                return;
            }
            Response response = (Response)advice.getParameterArray()[1];
            Request request = (Request)advice.getParameterArray()[0];
            requestTracer.endTrace(request, response, null, response.getStatus() + "");
        } finally {
            advice.unMark(TraceInterceptorAdaptor.BEFORE_TRACE_SUCCESS);
            Pradar.clearInvokeContext();
        }
    }

    @Override
    public void doException(Advice advice) throws Throwable {
        try {
            if (!advice.hasMark(TraceInterceptorAdaptor.BEFORE_TRACE_SUCCESS)) {
                return;
            }
            Response response = (Response)advice.getParameterArray()[1];
            Request request = (Request)advice.getParameterArray()[0];
            requestTracer.endTrace(request, response, advice.getThrowable(),
                response.getStatus() + "");
        } finally {
            advice.unMark(TraceInterceptorAdaptor.BEFORE_TRACE_SUCCESS);
            Pradar.clearInvokeContext();
        }
    }
}
