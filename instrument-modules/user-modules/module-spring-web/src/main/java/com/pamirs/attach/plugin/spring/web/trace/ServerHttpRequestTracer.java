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
package com.pamirs.attach.plugin.spring.web.trace;

import com.pamirs.attach.plugin.common.web.RequestTracer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2021/8/30 3:05 下午
 */
public class ServerHttpRequestTracer extends RequestTracer<ServerHttpRequest, ServerHttpResponse> {

    @Override
    public String getHeader(ServerHttpRequest request, String key) {
        return request.getHeaders().getFirst(key);
    }

    @Override
    public String getRemoteAddr(ServerHttpRequest request) {
        return request.getRemoteAddress().getAddress().getHostAddress();
    }

    @Override
    public String getRemotePort(ServerHttpRequest request) {
        return String.valueOf(request.getRemoteAddress().getPort());
    }

    @Override
    public String getRequestURI(ServerHttpRequest request) {
        return request.getPath().value();
    }

    @Override
    public String getMethod(ServerHttpRequest request) {
        return request.getMethod().name();
    }

    @Override
    public void setAttribute(ServerHttpRequest request, String key, Object value) {
        request.getHeaders().add(key, value.toString());
    }

    @Override
    public Object getAttribute(ServerHttpRequest request, String key) {
        return request.getHeaders().getFirst(key);
    }

    @Override
    public long getContentLength(ServerHttpRequest request) {
        return 0;
    }

    @Override
    public String getParams(ServerHttpRequest request) {
        return null;
    }

    @Override
    public String getResponse(ServerHttpResponse response) {
        return null;
    }

    @Override
    public String getStatusCode(ServerHttpResponse response, Throwable throwable) {
        if (throwable != null) {
            return "500";
        }
        if (response == null) {
            return "200";
        }
        return String.valueOf(response.getStatusCode().value());
    }

    @Override
    public void setResponseHeader(ServerHttpResponse httpResponseInfo, String key, Object value) {
        if (httpResponseInfo == null) {
            return;
        }
        httpResponseInfo.getHeaders().add(key, String.valueOf(value));
    }

}
