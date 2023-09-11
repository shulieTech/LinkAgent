package com.pamirs.attach.plugin.spring.cloud.gateway.filter;

import com.pamirs.attach.plugin.common.web.RequestTracer;
import com.pamirs.attach.plugin.spring.cloud.gateway.SpringCloudGatewayConstants;
import com.pamirs.attach.plugin.spring.cloud.gateway.tracer.ServerHttpRequestTracer;
import com.pamirs.pradar.BizClassLoaderService;
import com.pamirs.pradar.InvokeContext;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

/**
 * @author guann1n9
 * @date 2022/6/19 10:26 PM
 */
public class ResponseHttpHeadersFilter implements HttpHeadersFilter {

    private static final ResponseHttpHeadersFilter instace = new ResponseHttpHeadersFilter();

    private static final RequestTracer<ServerHttpRequest, ServerHttpResponse> requestTracer = new ServerHttpRequestTracer();


    @Override
    public HttpHeaders filter(HttpHeaders input, ServerWebExchange exchange) {
        BizClassLoaderService.setBizClassLoader(exchange.getClass().getClassLoader());
        try {
            final ServerHttpRequest request = exchange.getRequest();
            // 第一次是结束 netty http
            final InvokeContext nettyHttpContext = exchange.getAttribute(SpringCloudGatewayConstants.NETTY_HTTP_CONTEXT);
            if (nettyHttpContext == null || nettyHttpContext.isEmpty()) {
                return input;
            }
            exchange.getAttributes().remove(SpringCloudGatewayConstants.NETTY_HTTP_CONTEXT);
            Pradar.setInvokeContext(nettyHttpContext);
            final ServerHttpResponse response = exchange.getResponse();
            int code = response.getStatusCode() == null ? 200 : response.getStatusCode().value();
            Pradar.endClientInvoke(String.valueOf(code), MiddlewareType.TYPE_RPC);
            final InvokeContext springCloudGatewayContext = exchange.getAttribute(SpringCloudGatewayConstants.GATEWAY_CONTEXT);
            if (springCloudGatewayContext == null || springCloudGatewayContext.isEmpty()) {
                return input;
            }
            exchange.getAttributes().remove(SpringCloudGatewayConstants.GATEWAY_CONTEXT);
            // 第二次是结束 spring cloud gateway
            Pradar.setInvokeContext(springCloudGatewayContext);
            requestTracer.endTrace(request, exchange.getResponse(), null);
        } finally {
            BizClassLoaderService.clearBizClassLoader();
        }
        return input;
    }

    @Override
    public boolean supports(Type type) {
        BizClassLoaderService.setBizClassLoader(type.getClass().getClassLoader());
        boolean result = Type.RESPONSE.equals(type);
        BizClassLoaderService.clearBizClassLoader();
        return result;
    }


    public static ResponseHttpHeadersFilter getInstace() {
        return instace;
    }


    private ResponseHttpHeadersFilter() {
    }
}
