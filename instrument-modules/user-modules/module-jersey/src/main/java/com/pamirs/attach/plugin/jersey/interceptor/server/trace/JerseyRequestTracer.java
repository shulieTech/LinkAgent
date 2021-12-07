package com.pamirs.attach.plugin.jersey.interceptor.server.trace;

import com.pamirs.attach.plugin.common.web.RequestTracer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/12/02 4:31 下午
 */
public class JerseyRequestTracer extends RequestTracer<Request, Response> {
    @Override
    public String getHeader(Request request, String key) {
        return request.getHeader(key);
    }

    @Override
    public String getRemoteAddr(Request request) {
        return request.getRemoteAddr();
    }

    @Override
    public String getRemotePort(Request request) {
        return request.getRemotePort() + "";
    }

    @Override
    public String getRequestURI(Request request) {
        return request.getRequestURI();
    }

    @Override
    public String getMethod(Request request) {
        return request.getMethod().getMethodString();
    }

    @Override
    public void setAttribute(Request request, String key, Object value) {
        request.setAttribute(key, value);
    }

    @Override
    public Object getAttribute(Request request, String key) {
        return request.getAttribute(key);
    }

    @Override
    public long getContentLength(Request request) {
        return request.getContentLength();
    }

    @Override
    public String getParams(Request request) {
        return request.getParameters().paramsAsString();
    }

    @Override
    public String getResponse(Response response) {
        return null;
    }

    @Override
    public String getStatusCode(Response response, Throwable throwable) {
        return response.getStatus() + "";
    }

    @Override
    public void setResponseHeader(Response response, String key, Object value) {
        if (value == null) {
            return;
        }
        response.setHeader(key, value.toString());
    }
}
