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

import com.pamirs.attach.plugin.spring.cloud.gateway.SpringCloudGatewayConstants;
import com.pamirs.attach.plugin.spring.cloud.gateway.filter.RequestHttpHeadersFilter;
import com.pamirs.attach.plugin.spring.cloud.gateway.filter.ResponseHttpHeadersFilter;
import com.pamirs.pradar.InvokeContext;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.BeforeTraceInterceptorAdapter;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.gateway.filter.NettyRoutingFilter;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
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
        List<HttpHeadersFilter> headersFilters = target.getHeadersFilters();
        RequestHttpHeadersFilter requestHttpHeadersFilter = RequestHttpHeadersFilter.getInstance();
        ResponseHttpHeadersFilter responseHttpHeadersFilter = ResponseHttpHeadersFilter.getInstace();
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
