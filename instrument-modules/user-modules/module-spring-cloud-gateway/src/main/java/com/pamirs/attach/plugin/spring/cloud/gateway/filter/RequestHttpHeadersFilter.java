package com.pamirs.attach.plugin.spring.cloud.gateway.filter;

import com.pamirs.attach.plugin.spring.cloud.gateway.SpringCloudGatewayConstants;
import com.pamirs.pradar.BizClassLoaderService;
import com.pamirs.pradar.InvokeContext;
import com.pamirs.pradar.Pradar;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

/**
 * @author guann1n9
 * @date 2022/6/19 10:25 PM
 */
public class RequestHttpHeadersFilter implements HttpHeadersFilter {


    private static final RequestHttpHeadersFilter instance = new RequestHttpHeadersFilter();

    @Override
    public HttpHeaders filter(HttpHeaders input, ServerWebExchange exchange) {
        BizClassLoaderService.setBizClassLoader(exchange.getClass().getClassLoader());
        final HttpHeaders httpHeaders = new HttpHeaders();
        try {
            final InvokeContext invokeContext = Pradar.getInvokeContext();
            if (invokeContext == null || invokeContext.isEmpty()) {
                return input;
            }
            exchange.getAttributes().put(SpringCloudGatewayConstants.NETTY_HTTP_CONTEXT, invokeContext);
            httpHeaders.addAll(input);
            httpHeaders.setAll(Pradar.getInvokeContextTransformMap());
            Pradar.popInvokeContext();
        } finally {
            BizClassLoaderService.clearBizClassLoader();
        }
        return httpHeaders;
    }

    @Override
    public boolean supports(Type type) {
        BizClassLoaderService.setBizClassLoader(type.getClass().getClassLoader());
        boolean result = Type.REQUEST.equals(type);
        BizClassLoaderService.clearBizClassLoader();
        return result;
    }


    private RequestHttpHeadersFilter() {
    }

    public static RequestHttpHeadersFilter getInstance() {
        return instance;
    }

}
