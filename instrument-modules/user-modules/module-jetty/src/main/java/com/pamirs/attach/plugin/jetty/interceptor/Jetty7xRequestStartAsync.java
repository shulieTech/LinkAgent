package com.pamirs.attach.plugin.jetty.interceptor;


import com.pamirs.attach.plugin.common.web.RequestTracer;
import com.pamirs.attach.plugin.jetty.Jetty7xAsyncListener;
import com.pamirs.pradar.Pradar;
import org.eclipse.jetty.continuation.ContinuationListener;
import org.eclipse.jetty.server.AsyncContext;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/12/25 2:02 下午
 */
public class Jetty7xRequestStartAsync implements JettyRequestStartAsync {
    private final AsyncContext asyncContext;
    private final ServletRequest servletRequest;
    private final ServletResponse servletResponse;
    private final RequestTracer<HttpServletRequest, HttpServletResponse> requestTracer;

    public Jetty7xRequestStartAsync(Object asyncContext, final ServletRequest servletRequest, final ServletResponse servletResponse, final RequestTracer<HttpServletRequest, HttpServletResponse> requestTracer) {
        this.asyncContext = (AsyncContext) asyncContext;
        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;
        this.requestTracer = requestTracer;
    }

    @Override
    public void startAsync() {
        final ContinuationListener asyncListener = new Jetty7xAsyncListener(asyncContext, Pradar.popInvokeContextMap(), (HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, requestTracer);
        asyncContext.addContinuationListener(asyncListener);
    }
}
