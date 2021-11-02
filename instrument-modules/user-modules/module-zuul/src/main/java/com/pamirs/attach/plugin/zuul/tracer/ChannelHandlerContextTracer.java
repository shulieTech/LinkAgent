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

import com.pamirs.attach.plugin.common.web.RequestTracer;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import org.apache.commons.lang3.StringUtils;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2021/9/14 11:33 上午
 */
public class ChannelHandlerContextTracer extends RequestTracer<HttpRequest, HttpResponse> {

    @Override
    public String getHeader(HttpRequest request, String key) {
        return request.headers().get(key);
    }

    @Override
    public String getRemoteAddr(HttpRequest request) {
        //String host = request.headers().get("host");
        //if (StringUtils.isBlank(host)) {
        //    return null;
        //}
        //return host.split(":")[0];
        // 这里有时会返回域名，不返回真实的ip，所以return null
        return null;
    }

    @Override
    public String getRemotePort(HttpRequest request) {
        String host = request.headers().get("host");
        if (StringUtils.isBlank(host)) {
            return null;
        }
        String[] hostArr = host.split(":");
        if (hostArr.length < 2) {
            return "80";
        } else {
            return hostArr[1];
        }
    }

    @Override
    public String getRequestURI(HttpRequest request) {
        return request.uri();
    }

    @Override
    public String getMethod(HttpRequest request) {
        return request.method().name();
    }

    @Override
    public void setAttribute(HttpRequest request, String key, Object value) {
        request.headers().add(key, value);
    }

    @Override
    public Object getAttribute(HttpRequest request, String key) {
        return request.headers().get(key);
    }

    @Override
    public long getContentLength(HttpRequest request) {
        // request中获取不到contentLength，所以这里固定返回0
        return 0L;
    }

    @Override
    public String getParams(HttpRequest request) {
        return null;
    }

    @Override
    public String getResponse(HttpResponse response) {
        return null;
    }

    @Override
    public String getStatusCode(HttpResponse response, Throwable throwable) {
        if (throwable != null) {
            return "500";
        }
        if (response == null) {
            return "200";
        }
        return String.valueOf(response.status());
    }

    @Override
    public void setResponseHeader(HttpResponse httpResponse, String key, Object value) {
        if (httpResponse == null) {
            return;
        }
        httpResponse.headers().add(key, value);
    }
}
