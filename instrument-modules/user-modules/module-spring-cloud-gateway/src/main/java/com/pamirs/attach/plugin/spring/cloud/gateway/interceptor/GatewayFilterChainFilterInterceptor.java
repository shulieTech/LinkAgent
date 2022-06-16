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
package com.pamirs.attach.plugin.spring.cloud.gateway.interceptor;

import com.pamirs.attach.plugin.common.web.RequestTracer;
import com.pamirs.attach.plugin.spring.cloud.gateway.SpringCloudGatewayConstants;
import com.pamirs.attach.plugin.spring.cloud.gateway.tracer.ServerHttpRequestTracer;
import com.pamirs.pradar.InvokeContext;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.BeforeTraceInterceptorAdapter;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.NettyRoutingFilter;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

/**
 * @author liqiyu
 * @date 2021/4/6 20:39
 */
public class GatewayFilterChainFilterInterceptor extends BeforeTraceInterceptorAdapter {

    private static final RequestTracer<ServerHttpRequest, ServerHttpResponse> requestTracer = new ServerHttpRequestTracer();;
    private static final HttpHeadersFilter requestHttpHeadersFilter = new HttpHeadersFilter() {
        @Override
        public HttpHeaders filter(HttpHeaders input, ServerWebExchange exchange) {
            final InvokeContext invokeContext = Pradar.getInvokeContext();
            if(invokeContext == null || invokeContext.isEmpty()){
                return input;
            }
            exchange.getAttributes().put(SpringCloudGatewayConstants.NETTY_HTTP_CONTEXT, invokeContext);
            final HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.addAll(input);
            httpHeaders.setAll(Pradar.getInvokeContextTransformMap());
            Pradar.popInvokeContext();
            return httpHeaders;
        }

        @Override
        public boolean supports(Type type) {
            return Type.REQUEST.equals(type);
        }
    };



    private static final HttpHeadersFilter responseHttpHeadersFilter = new HttpHeadersFilter() {
        @Override
        public HttpHeaders filter(HttpHeaders input, ServerWebExchange exchange) {
            final ServerHttpRequest request = exchange.getRequest();
            // 第一次是结束 netty http
            final InvokeContext nettyHttpContext = exchange.getAttribute(SpringCloudGatewayConstants.NETTY_HTTP_CONTEXT);
            if(nettyHttpContext == null || nettyHttpContext.isEmpty()){
                return input;
            }
            exchange.getAttributes().remove(SpringCloudGatewayConstants.NETTY_HTTP_CONTEXT);
            Pradar.setInvokeContext(nettyHttpContext);
            final ServerHttpResponse response = exchange.getResponse();
            Pradar.endClientInvoke(String.valueOf(response.getStatusCode().value()),MiddlewareType.TYPE_RPC);
            final InvokeContext springCloudGatewayContext = exchange.getAttribute(SpringCloudGatewayConstants.GATEWAY_CONTEXT);
            if(springCloudGatewayContext == null || springCloudGatewayContext.isEmpty()){
                return input;
            }
            exchange.getAttributes().remove(SpringCloudGatewayConstants.GATEWAY_CONTEXT);
            // 第二次是结束 spring cloud gateway
            Pradar.setInvokeContext(springCloudGatewayContext);
            requestTracer.endTrace(request, exchange.getResponse(), null);
            return input;
        }
        @Override
        public boolean supports(Type type) {
            return Type.RESPONSE.equals(type);
        }
    };
    @Override
    public String getPluginName() {
        return "spring-cloud-gateway-forward";
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
        final NettyRoutingFilter target = (NettyRoutingFilter)advice.getTarget();
        final Object[] parameterArray = advice.getParameterArray();
        ServerWebExchange exchange = (ServerWebExchange)parameterArray[0];
        final InvokeContext context = exchange.getAttribute(SpringCloudGatewayConstants.GATEWAY_CONTEXT);
        if(context == null || context.isEmpty()){
            return;
        }
        Pradar.setInvokeContext(context);
        boolean existsMethod = Reflect.on(target).existsMethod("getHeadersFilters");
        List<HttpHeadersFilter> headersFilters;
        if(existsMethod){
            headersFilters = target.getHeadersFilters();
        }else {
            ObjectProvider<List<HttpHeadersFilter>> provider = Reflect.on(target).get("headersFilters");
            headersFilters = provider.getIfAvailable();
        }
        if(!headersFilters.contains(requestHttpHeadersFilter)){
            headersFilters.add(requestHttpHeadersFilter);
        }
        if(!headersFilters.contains(responseHttpHeadersFilter)){
            headersFilters.add(responseHttpHeadersFilter);
        }
    }

    @Override
    public SpanRecord beforeTrace(final Advice advice) {
        final Object[] parameterArray = advice.getParameterArray();
        ServerWebExchange exchange = (ServerWebExchange)parameterArray[0];
        ServerHttpRequest request = exchange.getRequest();
        URI requestUrl = exchange.getRequiredAttribute(GATEWAY_REQUEST_URL_ATTR);
        //添加压测数据到header
        SpanRecord record = new SpanRecord();
        record.setRemoteIp(requestUrl.getHost());
        record.setService(request.getPath().toString());
        record.setMethod(StringUtils.upperCase(request.getMethodValue()));
        record.setPort(requestUrl.getPort());
        final Map<String, String> stringStringMap = request.getQueryParams().toSingleValueMap();
        if (!stringStringMap.isEmpty()) {
            StringBuilder params = new StringBuilder();
            for (Entry<String, String> entry : stringStringMap.entrySet()) {
                params.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
            }
            record.setRequest(params.toString());
            record.setRequestSize(params.length());
        }else {
            record.setRequestSize(0);
        }
        return record;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        SpanRecord record = new SpanRecord();
        record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        record.setResponse(advice.getThrowable());
        record.setResponseSize(0);
        return record;
    }
}
