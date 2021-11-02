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
package com.pamirs.attach.plugin.zuul.tracer;

import com.netflix.zuul.message.http.HttpRequestInfo;
import com.netflix.zuul.message.http.HttpResponseInfo;
import com.pamirs.attach.plugin.common.web.RequestTracer;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2021/8/30 3:05 下午
 */
public class ZuulMessageTracer extends RequestTracer<HttpRequestInfo, HttpResponseInfo> {

    @Override
    public String getHeader(HttpRequestInfo request, String key) {
        return request.getHeaders().getFirst(key);
    }

    @Override
    public String getRemoteAddr(HttpRequestInfo request) {
        return request.getClientIp();
    }

    @Override
    public String getRemotePort(HttpRequestInfo request) {
        return String.valueOf(request.getPort());
    }

    @Override
    public String getRequestURI(HttpRequestInfo request) {
        return request.getPath();
    }

    @Override
    public String getMethod(HttpRequestInfo request) {
        return request.getMethod();
    }

    @Override
    public void setAttribute(HttpRequestInfo request, String key, Object value) {
        request.getHeaders().add(key, value.toString());
    }

    @Override
    public Object getAttribute(HttpRequestInfo request, String key) {
        return request.getHeaders().getFirst(key);
    }

    @Override
    public long getContentLength(HttpRequestInfo request) {
        return request.getBodyLength();
    }

    @Override
    public String getParams(HttpRequestInfo request) {
        return null;
    }

    @Override
    public String getResponse(HttpResponseInfo response) {
        return null;
    }

    @Override
    public String getStatusCode(HttpResponseInfo response, Throwable throwable) {
        if (throwable != null) {
            return "500";
        }
        if (response == null) {
            return "200";
        }
        return String.valueOf(response.getStatus());
    }

    @Override
    public void setResponseHeader(HttpResponseInfo httpResponseInfo, String key, Object value) {
        if (httpResponseInfo == null) {
            return;
        }
        httpResponseInfo.getHeaders().add(key, String.valueOf(value));
    }

}
