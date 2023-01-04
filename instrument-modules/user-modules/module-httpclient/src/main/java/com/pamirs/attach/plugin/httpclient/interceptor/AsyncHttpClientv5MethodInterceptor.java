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
package com.pamirs.attach.plugin.httpclient.interceptor;

import java.lang.reflect.Field;
import java.net.SocketTimeoutException;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.pamirs.attach.plugin.httpclient.HttpClientConstants;
import com.pamirs.attach.plugin.httpclient.utils.BlackHostChecker;
import com.pamirs.attach.plugin.httpclient.utils.HttpClientFixJsonStrategies;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.common.HeaderMark;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.internal.config.ExecutionCall;
import com.pamirs.pradar.internal.config.MatchConfig;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.pressurement.mock.JsonMockStrategy;
import com.shulie.instrument.simulator.api.ProcessControlException;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.StringUtils;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.nio.support.BasicRequestProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by baozi on 2021/8/12.
 */
public class AsyncHttpClientv5MethodInterceptor extends AroundInterceptor {
    private final static Logger logger = LoggerFactory.getLogger(AsyncHttpClientv5MethodInterceptor.class);

    private static String getService(String schema, String host, int port, String path) {
        String url = schema + "://" + host;
        if (port != -1 && port != 80) {
            url = url + ':' + port;
        }
        return url + path;
    }

    @Override
    public void doBefore(final Advice advice) throws ProcessControlException {
        Object[] args = advice.getParameterArray();
        //HttpHost
        HttpHost httpHost = (HttpHost) args[0];
        if (httpHost == null) {
            return;
        }
        BasicRequestProducer requestBasic = (BasicRequestProducer) args[1];
        final SimpleHttpRequest request;
        try {
            Field field = requestBasic.getClass().getDeclaredField("request");
            field.setAccessible(true);
            request = (SimpleHttpRequest) field.get(requestBasic);
        } catch (Exception e) {
            logger.error("获取request参数错误", e);
            return;
        }
        if (request == null) {
            return;
        }
        String host = httpHost.getHostName();
        int port = httpHost.getPort();
        String path = httpHost.getHostName();
        String reqStr = request.toString();
        String method = StringUtils.upperCase(reqStr.substring(0, reqStr.indexOf(" ")));
        //判断是否在白名单中
        String url = getService(httpHost.getSchemeName(), host, port, path);
        boolean isBlackHost = BlackHostChecker.isBlackHost(url);
        Pradar.startClientInvoke(path, method);
        Pradar.remoteIp(host);
        Pradar.remotePort(port);
        Pradar.middlewareName(HttpClientConstants.HTTP_CLIENT_NAME_5X);
        Header[] headers = request.getHeaders("content-length");
        if (headers != null && headers.length != 0) {
            try {
                Header header = headers[0];
                Pradar.requestSize(Integer.valueOf(header.getValue()));
            } catch (NumberFormatException e) {
            }
        }
        final Map<String, String> context = Pradar.getInvokeContextMap();
        if (!isBlackHost) {
            for (Map.Entry<String, String> entry : context.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (request.getHeaders(HeaderMark.DONT_MODIFY_HEADER) == null ||
                        request.getHeaders(HeaderMark.DONT_MODIFY_HEADER).length == 0) {
                    request.setHeader(key, value);
                }
            }
        }
        //pradar启动提前为的是捕获白名单异常能trace进去的
        httpClusterTest(advice, request, url);
        Pradar.popInvokeContext();

        final Object future = args[args.length - 1];
        if (!(future instanceof FutureCallback) && future != null) {
            return;
        }

        advice.changeParameter(args.length - 1, new FutureCallback() {
            @Override
            public void completed(Object result) {
                Pradar.setInvokeContext(context);
                ((FutureCallback) future).completed(result);
                try {
                    if (result instanceof SimpleHttpResponse) {
                        afterTrace(request, (SimpleHttpResponse) result);
                    } else {
                        afterTrace(request, null);
                    }
                } catch (Throwable e) {
                    LOGGER.error("AsyncHttpClient execute future endTrace error.", e);
                    Pradar.endClientInvoke("200", HttpClientConstants.PLUGIN_TYPE);
                }
            }

            @Override
            public void failed(Exception ex) {
                Pradar.setInvokeContext(context);
                ((FutureCallback) future).failed(ex);
                try {
                    exceptionTrace(request, ex);
                } catch (Throwable e) {
                    LOGGER.error("AsyncHttpClient execute future endTrace error.", e);
                    Pradar.endClientInvoke("200", HttpClientConstants.PLUGIN_TYPE);
                }
            }

            @Override
            public void cancelled() {
                Pradar.setInvokeContext(context);
                ((FutureCallback) future).cancelled();
                try {
                    exceptionTrace(request, null);
                } catch (Throwable e) {
                    LOGGER.error("AsyncHttpClient execute future endTrace error.", e);
                    Pradar.endClientInvoke("200", HttpClientConstants.PLUGIN_TYPE);
                }
            }
        });

    }

    private void httpClusterTest(Advice advice, final SimpleHttpRequest request, String url) throws ProcessControlException {
        MatchConfig config = ClusterTestUtils.httpClusterTest(url);
        Header[] wHeaders = request.getHeaders(PradarService.PRADAR_WHITE_LIST_CHECK);
        if (wHeaders != null && wHeaders.length > 0) {
            config.addArgs(PradarService.PRADAR_WHITE_LIST_CHECK, wHeaders[0].getValue());
        }
        config.addArgs("url", url);

        config.addArgs("request", request);
        config.addArgs("method", "uri");
        config.addArgs("isInterface", Boolean.FALSE);

        if (config.getStrategy() instanceof JsonMockStrategy) {
            Object[] args = advice.getParameterArray();
            config.addArgs("futureCallback", args[args.length - 1]);
            HttpClientFixJsonStrategies.HTTPCLIENT5_FIX_JSON_STRATEGY.processBlock(advice.getBehavior().getReturnType(), advice.getClassLoader(), config);
        }

        try {
            config.getStrategy().processBlock(advice.getBehavior().getReturnType(), advice.getClassLoader(), config,
                    new ExecutionCall() {
                        @Override
                        public Object call(Object param) {
                            //现在先暂时注释掉因为只有jdk8以上才能用
                            java.util.concurrent.CompletableFuture<SimpleHttpResponse> future = new java.util.concurrent.CompletableFuture<SimpleHttpResponse>();

                            SimpleHttpResponse response = null;
                            if (param instanceof String) {
                                response = SimpleHttpResponse.create(200, (String) param);
                            } else {
                                response = SimpleHttpResponse.create(200, JSONObject.toJSONBytes(param));
                            }
                            future.complete(response);
                            return future;
                        }
                    });
        } catch (PressureMeasureError e) {
            Pradar.response(e);
            Pradar.endClientInvoke(ResultCode.INVOKE_RESULT_FAILED, HttpClientConstants.PLUGIN_TYPE);
            throw e;
        }
    }

    public void afterTrace(SimpleHttpRequest request, SimpleHttpResponse response) {
        try {
            Pradar.responseSize(response == null ? 0 : response.getBodyText().length());
        } catch (Throwable e) {
            Pradar.responseSize(0);
        }
        Pradar.request(request.getBody());
        int code = response == null ? 200 : response.getCode();
        Pradar.endClientInvoke(code + "", HttpClientConstants.PLUGIN_TYPE);

    }

    public void exceptionTrace(SimpleHttpRequest request, Throwable throwable) {
        Pradar.request(request.getBody());
        Pradar.response(throwable);
        if (throwable != null && (throwable instanceof SocketTimeoutException)) {
            Pradar.endClientInvoke(ResultCode.INVOKE_RESULT_TIMEOUT, HttpClientConstants.PLUGIN_TYPE);
        } else {
            Pradar.endClientInvoke(ResultCode.INVOKE_RESULT_FAILED, HttpClientConstants.PLUGIN_TYPE);
        }
    }

}
