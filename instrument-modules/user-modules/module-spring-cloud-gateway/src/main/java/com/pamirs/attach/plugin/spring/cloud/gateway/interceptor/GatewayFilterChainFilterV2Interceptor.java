/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.spring.cloud.gateway.interceptor;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.attach.plugin.spring.cloud.gateway.filter.RequestHttpHeadersFilter;
import com.pamirs.attach.plugin.spring.cloud.gateway.filter.ResponseHttpHeadersFilter;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.BeforeTraceInterceptorAdapter;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.gateway.filter.NettyRoutingFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

/**
 * @author liqiyu
 * @date 2021/4/6 20:39
 */
public class GatewayFilterChainFilterV2Interceptor extends BeforeTraceInterceptorAdapter {

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
        final NettyRoutingFilter target = (NettyRoutingFilter) advice.getTarget();
        boolean existsMethod = existsMethod(target,"getHeadersFilters");
        if(!existsMethod){
            return;
        }
        RequestHttpHeadersFilter requestHttpHeadersFilter = RequestHttpHeadersFilter.getInstance();
        ResponseHttpHeadersFilter responseHttpHeadersFilter = ResponseHttpHeadersFilter.getInstace();
        if (!target.getHeadersFilters().contains(requestHttpHeadersFilter)) {
            target.getHeadersFilters().add(requestHttpHeadersFilter);
        }
        if (!target.getHeadersFilters().contains(responseHttpHeadersFilter)) {
            target.getHeadersFilters().add(responseHttpHeadersFilter);
        }
    }


    private static final Map<String, Boolean> methodCheckCache = new ConcurrentHashMap<String, Boolean>();

    private Boolean existsMethod(Object target,String method) {
        Boolean result = methodCheckCache.get(target.getClass().toString()+"_"+method);
        if(result != null){
            return result;
        }
        result = ReflectionUtils.existsMethod(target.getClass(), method);
        methodCheckCache.put(target.getClass().toString()+"_"+method,result);
        return result;
    }


    private static final Map<Class, Field> fieldMap = new ConcurrentHashMap<Class, Field>();


    private Field getHeadersFilters(Object target) {
        Field field = fieldMap.get(target.getClass());
        if(field != null){
            return field;
        }
        field = ReflectionUtils.findField(target.getClass(), "headersFilters");
        fieldMap.put(target.getClass(),field);
        return field;
    }

    @Override
    public SpanRecord beforeTrace(final Advice advice) {
        final Object[] parameterArray = advice.getParameterArray();
        ServerWebExchange exchange = (ServerWebExchange) parameterArray[0];
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
        } else {
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
