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
package com.pamirs.attach.plugin.resin.interceptor;

import com.pamirs.attach.plugin.common.web.BufferedServletRequestWrapper;
import com.pamirs.attach.plugin.common.web.RequestTracer;
import com.pamirs.attach.plugin.common.web.ServletRequestTracer;
import com.pamirs.attach.plugin.resin.ResinConstans;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/13 5:10 下午
 */
public class ServletInvocationServiceInterceptor extends AroundInterceptor {
    private final static Logger logger = LoggerFactory.getLogger(ServletInvocationServiceInterceptor.class);

    private final RequestTracer<HttpServletRequest, HttpServletResponse> requestTracer;

    public ServletInvocationServiceInterceptor() {
        Pradar.WEB_SERVER_NAME = ResinConstans.PLUGIN_NAME;
        this.requestTracer = new ServletRequestTracer();
    }

    @Override
    public void doBefore(Advice advice) throws Throwable {
        ServletRequest servletRequest = (ServletRequest) advice.getParameterArray()[0];
        ServletResponse servletResponse = (ServletResponse) advice.getParameterArray()[1];
        if (!(servletRequest instanceof HttpServletRequest)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Invalid target object, The javax.servlet.http.HttpServletRequest interface is not implemented. target={}", servletRequest);
            }
            return;
        }

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        advice.changeParameter(0, new BufferedServletRequestWrapper(request));
        requestTracer.startTrace(request, response, Pradar.WEB_SERVER_NAME);
        advice.mark(TraceInterceptorAdaptor.BEFORE_TRACE_SUCCESS);
    }

    @Override
    public void doAfter(Advice advice) throws Throwable {
        try {
            if (!advice.hasMark(TraceInterceptorAdaptor.BEFORE_TRACE_SUCCESS)) {
                return;
            }
            ServletRequest servletRequest = (ServletRequest) advice.getParameterArray()[0];
            ServletResponse servletResponse = (ServletResponse) advice.getParameterArray()[1];
            if (!(servletRequest instanceof HttpServletRequest)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Invalid target object, The javax.servlet.http.HttpServletRequest interface is not implemented. target={}", servletRequest);
                }
                return;
            }
            if (!(servletResponse instanceof HttpServletResponse)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Invalid target object, The javax.servlet.http.HttpServletResponse interface is not implemented. target={}", servletResponse);
                }
                return;
            }

            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            requestTracer.endTrace(request, response, null, "200");
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
            ServletRequest servletRequest = (ServletRequest) advice.getParameterArray()[0];
            ServletResponse servletResponse = (ServletResponse) advice.getParameterArray()[1];
            if (!(servletRequest instanceof HttpServletRequest)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Invalid target object, The javax.servlet.http.HttpServletRequest interface is not implemented. target={}", servletRequest);
                }
                return;
            }
            if (!(servletResponse instanceof HttpServletResponse)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Invalid target object, The javax.servlet.http.HttpServletResponse interface is not implemented. target={}", servletResponse);
                }
                return;
            }

            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            requestTracer.endTrace(request, response, advice.getThrowable(), "500");
        } finally {
            advice.unMark(TraceInterceptorAdaptor.BEFORE_TRACE_SUCCESS);
            Pradar.clearInvokeContext();
        }
    }
}
