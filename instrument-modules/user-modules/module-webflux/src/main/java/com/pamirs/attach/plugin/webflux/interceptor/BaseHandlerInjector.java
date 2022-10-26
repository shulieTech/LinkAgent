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
package com.pamirs.attach.plugin.webflux.interceptor;


import com.pamirs.attach.plugin.webflux.common.StaticFileFilter;
import com.pamirs.pradar.InvokeContext;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.util.CollectionUtils;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;


/**
 * @Auther: vernon
 * @Date: 2020/12/28 19:27
 * @Description:
 */
public class BaseHandlerInjector extends AroundInterceptor {

    public static final String WEB_CONTEXT = "p-gateway-context";

    protected void doBefore(ServerWebExchange exchange) {
        if (isClusterTest(exchange)) {
            final InvokeContext context = exchange.getAttribute(WEB_CONTEXT);
            if (context != null && !context.isEmpty()) {
                Pradar.setInvokeContext(context);
            }
            Pradar.setClusterTest(true);
        }
    }

    protected void doAfter(ServerWebExchange exchange, ServerHttpResponse response, Throwable throwable) {
//        Pradar.setClusterTest(false);
    }

    private boolean isClusterTest(ServerWebExchange exchange) {
        List<String> ua = exchange.getRequest().getHeaders().get(PradarService.PRADAR_HTTP_CLUSTER_TEST_KEY);
        if (CollectionUtils.isNotEmpty(ua) && ua.contains(PradarService.PRADAR_CLUSTER_TEST_HTTP_USER_AGENT_SUFFIX)) {
            return true;
        }

        List<String> clusterTest = exchange.getRequest().getHeaders().get(PradarService.PRADAR_CLUSTER_TEST_KEY);
        if (CollectionUtils.isNotEmpty(clusterTest) && clusterTest.contains("1")) {
            return true;
        }
        return false;
    }

    private boolean isNeedFilter(ServerWebExchange exchange) {

        ServerWebExchange ex = null;
        if (exchange instanceof ServerWebExchange) {
            ex = (ServerWebExchange) exchange;
        }
        String checkTest = ex.getRequest().getURI().toString();

        return StaticFileFilter.needFilter(checkTest) || ex.getAttribute(StaticFileFilter.PRADAR_FILTER) != null;
    }

    private boolean getPradarSet(ServerWebExchange request) {
        Object value = request.getAttribute("Pradar-Set");
        if (value != null && (value instanceof Boolean)) {
            return (Boolean) value;
        }
        return false;
    }

}
