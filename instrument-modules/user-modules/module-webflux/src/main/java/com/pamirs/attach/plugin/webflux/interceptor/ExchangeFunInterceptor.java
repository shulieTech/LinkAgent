/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pamirs.attach.plugin.webflux.interceptor;

import com.pamirs.attach.plugin.webflux.common.WebFluxConstants;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.common.HeaderMark;
import com.pamirs.pradar.interceptor.ContextTransfer;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.internal.config.ExecutionCall;
import com.pamirs.pradar.internal.config.MatchConfig;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.utils.InnerWhiteListCheckUtil;
import com.shulie.instrument.simulator.api.ProcessControlException;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientRequest;

import java.lang.reflect.Field;
import java.net.SocketTimeoutException;

public class ExchangeFunInterceptor extends TraceInterceptorAdaptor {

    final Logger logger = LoggerFactory.getLogger(ExchangeFunInterceptor.class);

    private static Field headers;

    @Override
    public String getPluginName() {
        return WebFluxConstants.MODULE_NAME;
    }

    @Override
    public int getPluginType() {
        return WebFluxConstants.PLUGIN_TYPE;
    }

    @Override
    public void beforeLast(Advice advice) throws ProcessControlException {
        if (!Pradar.isClusterTest() && !ClusterTestUtils.enableBizRequestMock()) {
            return;
        }
        Object[] args = advice.getParameterArray();
        final ClientRequest request = (ClientRequest) args[0];
        if (request == null) {
            return;
        }

        String host = request.url().getHost();
        int port = request.url().getPort();
        String path = request.url().getPath();

        //判断是否在白名单中
        String url = getService(request.url().getScheme(), host, port, path);

        MatchConfig config = ClusterTestUtils.httpClusterTest(url);

        config.addArgs("url", url);
        config.addArgs("request", request);
        config.addArgs("method", "url");
        config.addArgs("isInterface", Boolean.FALSE);
        config.getStrategy().processBlock(advice.getBehavior().getReturnType(), advice.getClassLoader(), config,
                new ExecutionCall() {
                    @Override
                    public Object call(Object param) {
                        return param;
                    }
                });
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        InnerWhiteListCheckUtil.check();
        ClientRequest request = (ClientRequest) advice.getParameterArray()[0];

        SpanRecord record = new SpanRecord();
        record.setRemoteIp(request.url().getHost());
        record.setService(request.url().getPath());
        record.setMethod(StringUtils.upperCase(request.method().name()));
        record.setRemoteIp(request.url().getHost());
        record.setPort(request.url().getPort());
        record.setRequest(request.url().getQuery());
        return record;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        SpanRecord record = new SpanRecord();
        if (advice.getThrowable() instanceof SocketTimeoutException) {
            record.setResultCode(ResultCode.INVOKE_RESULT_TIMEOUT);
        } else {
            record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        }
        record.setResponse(advice.getThrowable());
        record.setResponseSize(0);
        InnerWhiteListCheckUtil.check();
        return record;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        SpanRecord record = new SpanRecord();
        InnerWhiteListCheckUtil.check();
        record.setResponseSize(0);
        return record;
    }

    @Override
    protected ContextTransfer getContextTransfer(Advice advice) {
        Object[] args = advice.getParameterArray();
        final ClientRequest request = (ClientRequest) args[0];
        initHeaders(request);

        return new ContextTransfer() {
            @Override
            public void transfer(String key, String value) {

                MultiValueMap<String, String> headersMap;
                try {
                    headersMap = (MultiValueMap<String, String>) headers.get(request);
                    if (headersMap.get(HeaderMark.DONT_MODIFY_HEADER) == null) {
                        headersMap.add(key, value);
                    }
                } catch (IllegalAccessException e) {
                    logger.error("ExchangeFunInterceptor set header error", e);
                }
            }
        };
    }

    private void initHeaders(Object target) {
        if (headers != null) {
            return;
        }
        try {
            headers = target.getClass().getDeclaredField("headers");
            headers.setAccessible(true);
        } catch (Throwable e) {
            logger.error("ExchangeFunInterceptor get headers field error", e);
        }
    }

    private static String getService(String schema, String host, int port, String path) {
        String url = schema + "://" + host;
        if (port != -1 && port != 80) {
            url = url + ':' + port;
        }
        return url + path;
    }
}
