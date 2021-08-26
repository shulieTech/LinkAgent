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
package com.pamirs.attach.plugin.mule.interceptor;

import com.pamirs.attach.plugin.mule.obj.RequestTracer;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.internal.config.MatchConfig;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.shulie.instrument.simulator.api.ProcessControlException;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;

/**
 * Create by xuyh at 2020/6/18 22:51.
 */
public class MuleHttpRequestDispatcherFilterInterceptor extends AroundInterceptor {

    private ThreadLocal<HttpRequestPacket> requestPacketThreadLocal = new ThreadLocal<HttpRequestPacket>();

    private HttpRequestPacket getRequest() {
        return requestPacketThreadLocal.get();
    }

    private void setRequest(HttpRequestPacket request) {
        requestPacketThreadLocal.set(request);
    }

    @Override
    public void doBefore(Advice advice) throws ProcessControlException {
        Object[] args = advice.getParameterArray();
        setRequest(null);
        if (args == null || args.length == 0) {
            return;
        }
        if (!(args[0] instanceof FilterChainContext)) {
            return;
        }
        FilterChainContext context = (FilterChainContext) args[0];
        Object t = context.getMessage();
        if (t instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) t;
            HttpRequestPacket request = (HttpRequestPacket) httpContent.getHttpHeader();
            setRequest(request);
            String requestUri = request.getRequestURI();
            // trace log
            RequestTracer.doBeforeTrace(request);


            MatchConfig config = ClusterTestUtils.httpClusterTest(requestUri);
            String check = request.getHeaders().getHeader(PradarService.PRADAR_WHITE_LIST_CHECK);
            config.addArgs(PradarService.PRADAR_WHITE_LIST_CHECK, check);

            config.addArgs("url", requestUri);
            config.addArgs("isInterface", Boolean.FALSE);
            config.getStrategy().processBlock(advice.getClassLoader(), config);
        }
    }

    @Override
    public void doAfter(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args == null || args.length == 0) {
            return;
        }
        if (!(args[0] instanceof FilterChainContext)) {
            return;
        }
        HttpRequestPacket request = getRequest();

        // trace log end
        RequestTracer.doAfterTrace(request, null, null);
        setRequest(null);
    }

    @Override
    public void doException(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args == null || args.length == 0) {
            return;
        }
        if (!(args[0] instanceof FilterChainContext)) {
            return;
        }
        HttpRequestPacket request = getRequest();

        // trace log end
        RequestTracer.doAfterTrace(request, null, advice.getThrowable());
        setRequest(null);
    }
}
