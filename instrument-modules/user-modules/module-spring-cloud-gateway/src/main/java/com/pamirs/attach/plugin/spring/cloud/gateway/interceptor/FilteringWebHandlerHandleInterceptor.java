package com.pamirs.attach.plugin.spring.cloud.gateway.interceptor;

import java.util.Map;
import java.util.Map.Entry;

import com.pamirs.attach.plugin.common.web.RequestTracer;
import com.pamirs.attach.plugin.spring.cloud.gateway.SpringCloudGatewayConstants;
import com.pamirs.attach.plugin.spring.cloud.gateway.tracer.ServerHttpRequestTracer;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

/**
 * @ClassName: FilteringWebHandlerHandleInterceptor
 * @author: wangjian
 * @Date: 2020/12/10 17:51
 * @Description:
 */
public class FilteringWebHandlerHandleInterceptor extends TraceInterceptorAdaptor {
    private final RequestTracer<ServerHttpRequest, ServerHttpResponse> requestTracer;

    public FilteringWebHandlerHandleInterceptor() {
        requestTracer = new ServerHttpRequestTracer();
    }

    @Override
    public String getPluginName() {
        return SpringCloudGatewayConstants.MODULE_NAME;
    }

    @Override
    public int getPluginType() {
        return SpringCloudGatewayConstants.PLUGIN_TYPE;
    }

    @Override
    protected boolean isClient(Advice advice) {
        return true;
    }

    @Override
    public void beforeFirst(Advice advice) {
        final Object[] args = advice.getParameterArray();
        ServerWebExchange arg = (ServerWebExchange)args[0];
        ServerHttpRequest serverHttpRequest = arg.getRequest();
        requestTracer.startTrace(serverHttpRequest, null, SpringCloudGatewayConstants.MODULE_NAME);
        final Map<String, String> invokeContextTransformMap = Pradar.getInvokeContextTransformMap();
        for (Entry<String, String> entry : invokeContextTransformMap.entrySet()) {
            arg.getAttributes().put(entry.getKey(),entry.getValue());
        }
    }


    @Override
    public void afterLast(Advice advice) {
        final Object[] args = advice.getParameterArray();
        ServerWebExchange arg = (ServerWebExchange)args[0];
        ServerHttpRequest request = arg.getRequest();
        requestTracer.endTrace(request, arg.getResponse(), null);
    }

    @Override
    public void exceptionLast(Advice advice) {
        final Object[] args = advice.getParameterArray();
        ServerWebExchange arg = (ServerWebExchange)args[0];
        ServerHttpRequest request = arg.getRequest();
        requestTracer.endTrace(request, arg.getResponse(), advice.getThrowable());
    }

}
