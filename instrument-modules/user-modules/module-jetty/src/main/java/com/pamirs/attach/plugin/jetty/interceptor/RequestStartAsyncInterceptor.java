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
package com.pamirs.attach.plugin.jetty.interceptor;


import com.pamirs.attach.plugin.common.web.RequestTracer;
import com.pamirs.attach.plugin.common.web.ServletRequestTracer;
import com.pamirs.attach.plugin.jetty.JettyConstans;
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

public class RequestStartAsyncInterceptor extends AroundInterceptor {
    private final static Logger logger = LoggerFactory.getLogger(RequestStartAsyncInterceptor.class);
    private final RequestTracer<HttpServletRequest, HttpServletResponse> requestTracer;

    public RequestStartAsyncInterceptor() {
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
        }

        if (!(servletResponse instanceof HttpServletResponse)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Invalid target object, The javax.servlet.http.HttpServletResponse interface is not implemented. target={}", servletRequest);
            }
        }

        requestTracer.startTrace((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, JettyConstans.PLUGIN_NAME);
        advice.mark(TraceInterceptorAdaptor.BEFORE_TRACE_SUCCESS);
    }

    @Override
    public void doAfter(Advice advice) throws Throwable {
        try {
            if (!advice.hasMark(TraceInterceptorAdaptor.BEFORE_TRACE_SUCCESS)) {
                return;
            }

            final ServletRequest servletRequest = (ServletRequest) advice.getParameterArray()[0];
            final ServletResponse servletResponse = (ServletResponse) advice.getParameterArray()[1];
            if (!(servletRequest instanceof HttpServletRequest)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Invalid target object, The javax.servlet.http.HttpServletRequest interface is not implemented. target={}", servletRequest);
                }
            }

            if (!(servletResponse instanceof HttpServletResponse)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Invalid target object, The javax.servlet.http.HttpServletResponse interface is not implemented. target={}", servletRequest);
                }
            }

            if (!validate(advice.getTarget(), advice.getReturnObj())) {
                return;
            }
            if (isOldJetty()) {
                JettyRequestStartAsync startAsync = new Jetty7xRequestStartAsync(advice.getReturnObj(), servletRequest, servletResponse, requestTracer);
                startAsync.startAsync();
            } else {
                JettyRequestStartAsync startAsync = new Jetty9xRequestStartAsync(advice.getReturnObj(), servletRequest, servletResponse, requestTracer);
                startAsync.startAsync();
            }

        } finally {
            advice.unMark(TraceInterceptorAdaptor.BEFORE_TRACE_SUCCESS);
            Pradar.clearInvokeContext();
        }
    }

    private ClassLoader getClassLoader() {
        ClassLoader classLoader = RequestStartAsyncInterceptor.class.getClassLoader();
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        return classLoader;
    }

    private boolean isOldJetty() {
        try {
            Class clazz = getClassLoader().loadClass("org.eclipse.jetty.server.AsyncContext");
            return true;
        } catch (Throwable throwable) {
        }
        return false;
    }

    @Override
    public void doException(Advice advice) throws Throwable {
        try {
            if (!advice.hasMark(TraceInterceptorAdaptor.BEFORE_TRACE_SUCCESS)) {
                return;
            }
            final ServletRequest servletRequest = (ServletRequest) advice.getParameterArray()[0];
            final ServletResponse servletResponse = (ServletResponse) advice.getParameterArray()[1];
            if (!(servletRequest instanceof HttpServletRequest)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Invalid target object, The javax.servlet.http.HttpServletRequest interface is not implemented. target={}", servletRequest);
                }
            }

            if (!(servletResponse instanceof HttpServletResponse)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Invalid target object, The javax.servlet.http.HttpServletResponse interface is not implemented. target={}", servletRequest);
                }
            }
            requestTracer.endTrace((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, advice.getThrowable(), "500");
        } finally {
            advice.unMark(TraceInterceptorAdaptor.BEFORE_TRACE_SUCCESS);
            Pradar.clearInvokeContext();
        }

    }

    private boolean validate(final Object target, final Object result) {
        if (result == null) {
            return false;
        }

        if (!(target instanceof HttpServletRequest)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Invalid target object, The javax.servlet.http.HttpServletRequest interface is not implemented. target={}", target);
            }
            return false;
        }
        return true;
    }
}
