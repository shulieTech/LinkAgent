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

import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;

import com.pamirs.attach.plugin.httpclient.HttpClientConstants;
import com.pamirs.attach.plugin.httpclient.utils.BlackHostChecker;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.common.HeaderMark;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.internal.adapter.ExecutionForwardCall;
import com.pamirs.pradar.internal.adapter.ExecutionStrategy;
import com.pamirs.pradar.internal.config.MatchConfig;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.pressurement.mock.JsonMockStrategy;
import com.pamirs.pradar.utils.InnerWhiteListCheckUtil;
import com.shulie.instrument.simulator.api.ProcessControlException;
import com.shulie.instrument.simulator.api.ProcessController;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

/**
 * Created by xiaobin on 2016/12/15.
 */
public class AsyncHttpClientv4MethodInterceptor extends AroundInterceptor {

    private static String getService(String schema, String host, int port, String path) {
        String url = schema + "://" + host;
        if (port != -1 && port != 80) {
            url = url + ':' + port;
        }
        return url + path;
    }

    private static ExecutionStrategy fixJsonStrategy = new JsonMockStrategy() {
        @Override
        public Object processBlock(Class returnType, ClassLoader classLoader, Object params) throws ProcessControlException {

            MatchConfig config = (MatchConfig)params;
            if (config.getScriptContent().contains("return")) {
                return null;
            }
            if (null == config.getArgs().get("futureCallback")) {
                return null;
            }
            //现在先暂时注释掉因为只有jdk8以上才能用
            FutureCallback<HttpResponse> futureCallback = (FutureCallback<HttpResponse>)config.getArgs().get(
                "futureCallback");
            StatusLine statusline = new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "");
            try {
                HttpEntity entity = null;
                entity = new StringEntity(config.getScriptContent());

                BasicHttpResponse response = new BasicHttpResponse(statusline);
                response.setEntity(entity);
                java.util.concurrent.CompletableFuture future = new java.util.concurrent.CompletableFuture();
                future.complete(response);
                futureCallback.completed(response);
                ProcessController.returnImmediately(returnType, future);
            } catch (ProcessControlException pe) {
                throw pe;
            } catch (Exception e) {
            }
            return null;
        }
    };

    @Override
    public void doBefore(final Advice advice) throws ProcessControlException {
        final Object[] args = advice.getParameterArray();
        HttpHost httpHost = (HttpHost)args[0];
        final HttpRequest request = (HttpRequest)args[1];
        if (httpHost == null) {
            return;
        }
        InnerWhiteListCheckUtil.check();
        String host = httpHost.getHostName();
        int port = httpHost.getPort();
        String path = httpHost.getHostName();
        String reqStr = request.toString();
        String method = StringUtils.upperCase(reqStr.substring(0, reqStr.indexOf(" ")));
        if (request instanceof HttpUriRequest) {
            path = ((HttpUriRequest)request).getURI().getPath();
            method = ((HttpUriRequest)request).getMethod();
        }
        //判断是否在白名单中
        String url = getService(httpHost.getSchemeName(), host, port, path);
        boolean isBlackHost = BlackHostChecker.isBlackHost(url);
        if (!isBlackHost) {
            httpClusterTest(advice, args, request, url);
        }
        Pradar.startClientInvoke(path, method);
        Pradar.remoteIp(host);
        Pradar.remotePort(port);
        Pradar.middlewareName(HttpClientConstants.HTTP_CLIENT_NAME_4X);
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
                if (request.getHeaders(HeaderMark.DONT_MODIFY_HEADER) == null || request.getHeaders(
                    HeaderMark.DONT_MODIFY_HEADER).length == 0) {
                    request.setHeader(key, value);
                }
            }
        }
        Pradar.popInvokeContext();

        final Object future = args[args.length - 1];
        if (!(future instanceof FutureCallback)) {
            return;
        }
        advice.changeParameter(args.length - 1, new FutureCallback() {
            @Override
            public void completed(Object result) {
                Pradar.setInvokeContext(context);
                ((FutureCallback)future).completed(result);
                try {
                    if (result instanceof HttpResponse) {
                        afterTrace(request, (HttpResponse)result);
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
                ((FutureCallback)future).failed(ex);
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
                ((FutureCallback)future).cancelled();
                try {
                    exceptionTrace(request, null);
                } catch (Throwable e) {
                    LOGGER.error("AsyncHttpClient execute future endTrace error.", e);
                    Pradar.endClientInvoke("200", HttpClientConstants.PLUGIN_TYPE);
                }
            }
        });

    }

    private void httpClusterTest(Advice advice, final Object[] args, final HttpRequest request, String url)
        throws ProcessControlException {
        final MatchConfig config = ClusterTestUtils.httpClusterTest(url);
        Header[] wHeaders = request.getHeaders(PradarService.PRADAR_WHITE_LIST_CHECK);
        if (wHeaders != null && wHeaders.length > 0) {
            config.addArgs(PradarService.PRADAR_WHITE_LIST_CHECK, wHeaders[0].getValue());
        }
        config.addArgs("url", url);

        config.addArgs("request", request);
        config.addArgs("method", "uri");
        config.addArgs("isInterface", Boolean.FALSE);
        if (args.length == 3) {
            config.addArgs("futureCallback", args[2]);
        } else if (args.length == 4) {
            config.addArgs("futureCallback", args[3]);
        }
        if (config.getStrategy() instanceof JsonMockStrategy) {
            fixJsonStrategy.processBlock(advice.getBehavior().getReturnType(), advice.getClassLoader(), config);
        }
        config.getStrategy().processBlock(advice.getBehavior().getReturnType(), advice.getClassLoader(), config,
            new ExecutionForwardCall() {
                @Override
                public Object forward(Object param) throws ProcessControlException {
                    if (Pradar.isClusterTest()) {
                        String forwarding = config.getForwarding();
                        try {
                            if (null != forwarding && null != request) {
                                URI uri = new URI(forwarding);
                                HttpHost httpHost1 = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
                                args[0] = httpHost1;

                                Object uriField = Reflect.on(request).get("uri");
                                if (uriField instanceof String) {
                                    Reflect.on(request).set("uri", uri.toASCIIString());
                                } else {
                                    try {
                                        Reflect.on(request).set("uri", uri);
                                    } catch (Exception e) {
                                        URL url = new URL(forwarding);
                                        Reflect.on(request).set("uri", url);
                                    }
                                }
                            }
                        } catch (Throwable t) {
                            throw new PressureMeasureError("not support forward type. params: " + param);
                        }
                    }
                    return null;
                }

                @Override
                public Object call(Object param) {
                    if (null == config.getArgs().get("futureCallback")) {
                        return null;
                    }
                    //现在先暂时注释掉因为只有jdk8以上才能用
                    FutureCallback<HttpResponse> futureCallback = (FutureCallback<HttpResponse>)config.getArgs().get(
                        "futureCallback");
                    StatusLine statusline = new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "");
                    try {
                        HttpEntity entity = null;
                        if (param instanceof String) {
                            entity = new StringEntity(String.valueOf(param));
                        } else {
                            entity = new ByteArrayEntity(JSONObject.toJSONBytes(param));
                        }
                        BasicHttpResponse response = new BasicHttpResponse(statusline);
                        response.setEntity(entity);
                        java.util.concurrent.CompletableFuture future = new java.util.concurrent.CompletableFuture();
                        future.complete(response);
                        futureCallback.completed(response);
                        return future;
                    } catch (Exception e) {
                    }
                    return null;
                }
            });
    }

    public void afterTrace(HttpRequest request, HttpResponse response) {
        try {
            Pradar.responseSize(response == null ? 0 : response.getEntity().getContentLength());
        } catch (Throwable e) {
            Pradar.responseSize(0);
        }
        Pradar.request(request.getParams());
        InnerWhiteListCheckUtil.check();
        int code = response == null ? 200 : response.getStatusLine().getStatusCode();
        Pradar.endClientInvoke(code + "", HttpClientConstants.PLUGIN_TYPE);

    }

    public void exceptionTrace(HttpRequest request, Throwable throwable) {
        Pradar.request(request.getParams());
        Pradar.response(throwable);
        InnerWhiteListCheckUtil.check();
        if (throwable != null && (throwable instanceof SocketTimeoutException)) {
            Pradar.endClientInvoke(ResultCode.INVOKE_RESULT_TIMEOUT, HttpClientConstants.PLUGIN_TYPE);
        } else {
            Pradar.endClientInvoke(ResultCode.INVOKE_RESULT_FAILED, HttpClientConstants.PLUGIN_TYPE);
        }
    }

}
