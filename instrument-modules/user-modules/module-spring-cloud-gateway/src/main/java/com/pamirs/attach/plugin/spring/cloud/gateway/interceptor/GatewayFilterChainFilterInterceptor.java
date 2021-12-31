package com.pamirs.attach.plugin.spring.cloud.gateway.interceptor;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.ContextTransfer;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.gateway.filter.NettyRoutingFilter;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

/**
 * @author liqiyu
 * @date 2021/4/6 20:39
 */
public class GatewayFilterChainFilterInterceptor extends TraceInterceptorAdaptor {

    private static final HttpHeadersFilter httpHeadersFilter = new HttpHeadersFilter() {
        @Override
        public HttpHeaders filter(HttpHeaders input, ServerWebExchange exchange) {
            final Map<String, String> invokeContextTransformMap = Pradar.getInvokeContextTransformMap();
            if(invokeContextTransformMap.isEmpty()){
                return input;
            }
            final HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.addAll(input);
            httpHeaders.setAll(invokeContextTransformMap);
            return httpHeaders;
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
        final Object[] parameterArray = advice.getParameterArray();
        ServerWebExchange exchange = (ServerWebExchange)parameterArray[0];
        //添加压测数据到header
        final HashMap<String, String> context = new HashMap<String, String>(Pradar.getInvokeContextTransformKeys().size());
        for (String invokeContextTransformKey : Pradar.getInvokeContextTransformKeys()) {
            final String value = exchange.getAttribute(invokeContextTransformKey);
            if(value != null) {
                context.put(invokeContextTransformKey, value);
                exchange.getAttributes().remove(invokeContextTransformKey);
            }
        }
        Pradar.setInvokeContext(context);
    }

    //@Override
    //protected ContextTransfer getContextTransfer(Advice advice) {
    //    final Object[] parameterArray = advice.getParameterArray();
    //    ServerWebExchange exchange = (ServerWebExchange)parameterArray[0];
    //    ServerHttpRequest request = exchange.getRequest();
    //    //添加压测数据到header
    //    final HttpHeaders httpHeaders = request.getHeaders();
    //    return new ContextTransfer() {
    //        @Override
    //        public void transfer(String key, String value) {
    //            httpHeaders.add(key, value);
    //        }
    //    };
    //}

    @Override
    public SpanRecord beforeTrace(final Advice advice) {
        final NettyRoutingFilter target = (NettyRoutingFilter)advice.getTarget();
        if(!target.getHeadersFilters().contains(httpHeadersFilter)){
            target.getHeadersFilters().add(httpHeadersFilter);
        }
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
