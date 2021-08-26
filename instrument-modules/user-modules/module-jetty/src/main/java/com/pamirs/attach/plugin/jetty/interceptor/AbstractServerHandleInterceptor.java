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
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author fabing.zhaofb
 */
public abstract class AbstractServerHandleInterceptor extends AroundInterceptor {
    protected final static Logger LOGGER = LoggerFactory.getLogger(AbstractServerHandleInterceptor.class.getName());

    protected final RequestTracer<HttpServletRequest, HttpServletResponse> requestTracer;

    public AbstractServerHandleInterceptor() {
        Pradar.WEB_SERVER_NAME = JettyConstans.PLUGIN_NAME;
        this.requestTracer = new ServletRequestTracer();
    }

    /**
     * 获取 HttpServletRequest
     *
     * @param args
     * @return
     */
    abstract HttpServletRequest toHttpServletRequest(Object[] args);

    /**
     * 获取 HttpServletResponse
     *
     * @param args
     * @return
     */
    abstract HttpServletResponse toHttpServletResponse(Object[] args);

    /**
     * 包装 request 请求
     *
     * @param advice
     */
    abstract void doWrapRequest(Advice advice);

    @Override
    public void doBefore(Advice advice) throws Throwable {
        HttpServletRequest request = toHttpServletRequest(advice.getParameterArray());
        HttpServletResponse response = toHttpServletResponse(advice.getParameterArray());
        if (request == null) {
            return;
        }
        try {
            if (request.getDispatcherType() == DispatcherType.ASYNC || request.getDispatcherType() == DispatcherType.ERROR) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Skip async servlet request event. isAsyncStarted={}, dispatcherType={}", request.isAsyncStarted(), request.getDispatcherType());
                }
                return;
            }
        } catch (Throwable e) {
        }

        doWrapRequest(advice);
        requestTracer.startTrace(request, response, JettyConstans.PLUGIN_NAME);
        advice.mark(TraceInterceptorAdaptor.BEFORE_TRACE_SUCCESS);
    }

    @Override
    public void doAfter(Advice advice) throws Throwable {
        if (!advice.hasMark(TraceInterceptorAdaptor.BEFORE_TRACE_SUCCESS)) {
            return;
        }
        try {
            HttpServletRequest request = toHttpServletRequest(advice.getParameterArray());
            if (request == null) {
                return;
            }
            HttpServletResponse response = toHttpServletResponse(advice.getParameterArray());
            try {
                if (request.getDispatcherType() == DispatcherType.ASYNC || request.getDispatcherType() == DispatcherType.ERROR) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Skip async servlet request event. isAsyncStarted={}, dispatcherType={}", request.isAsyncStarted(), request.getDispatcherType());
                    }
                    return;
                }
            } catch (Throwable e) {
            }
            requestTracer.endTrace(request, response, null);
        } catch (Throwable e) {
            Pradar.endTrace(ResultCode.INVOKE_RESULT_UNKNOWN, MiddlewareType.TYPE_WEB_SERVER);
            LOGGER.warn("jetty afterThrowing execute err!", e);
            throw e;
        } finally {
            advice.unMark(TraceInterceptorAdaptor.BEFORE_TRACE_SUCCESS);
            Pradar.clearInvokeContext();
        }
    }

    @Override
    public void doException(Advice advice) throws Throwable {
        if (!advice.hasMark(TraceInterceptorAdaptor.BEFORE_TRACE_SUCCESS)) {
            return;
        }
        try {
            HttpServletRequest request = toHttpServletRequest(advice.getParameterArray());
            HttpServletResponse response = toHttpServletResponse(advice.getParameterArray());
            try {
                if (request.getDispatcherType() == DispatcherType.ASYNC || request.getDispatcherType() == DispatcherType.ERROR) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Skip async servlet request event. isAsyncStarted={}, dispatcherType={}", request.isAsyncStarted(), request.getDispatcherType());
                    }
                    return;
                }
            } catch (Throwable e) {
            }
            requestTracer.endTrace(request, response, advice.getThrowable());
        } catch (Throwable e) {
            Pradar.endTrace(ResultCode.INVOKE_RESULT_UNKNOWN, MiddlewareType.TYPE_WEB_SERVER);
            LOGGER.warn("jetty afterThrowing execute err!", e);
            throw e;
        } finally {
            advice.unMark(TraceInterceptorAdaptor.BEFORE_TRACE_SUCCESS);
            Pradar.clearInvokeContext();
        }
    }
}
