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
package com.pamirs.attach.plugin.async.http.client.plugin.interceptor;

import com.pamirs.attach.plugin.async.http.client.plugin.AsyncHttpClientConstants;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.BeforeTraceInterceptorAdapter;
import com.pamirs.pradar.interceptor.ContextInject;
import com.pamirs.pradar.interceptor.ContextTransfer;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.util.CollectionUtils;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaders;
import org.apache.commons.lang.StringUtils;
import org.asynchttpclient.*;
import org.asynchttpclient.netty.request.NettyRequest;

import javax.net.ssl.SSLSession;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

/**
 * @author angju
 * @date 2021/4/6 20:39
 */
public class NettyRequestSenderSendRequestInterceptor extends BeforeTraceInterceptorAdapter {
    @Override
    public String getPluginName() {
        return AsyncHttpClientConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return AsyncHttpClientConstants.PLUGIN_TYPE;
    }

    @Override
    public void beforeFirst(Advice advice) {
        if (!Pradar.isClusterTest()) {
            return;
        }
        ClusterTestUtils.validateClusterTest();
        Request request = (Request) advice.getParameterArray()[0];
        //白名单判断
        ClusterTestUtils.validateHttpClusterTest(request.getUrl());

    }

    @Override
    protected ContextTransfer getContextTransfer(Advice advice) {
        Request request = (Request) advice.getParameterArray()[0];
        //添加压测数据到header
        final HttpHeaders httpHeaders = request.getHeaders();
        return new ContextTransfer() {
            @Override
            public void transfer(String key, String value) {
                httpHeaders.add(key, value);
            }
        };
    }

    @Override
    public SpanRecord beforeTrace(final Advice advice) {
        Request request = (Request) advice.getParameterArray()[0];
        //添加压测数据到header
        SpanRecord record = new SpanRecord();
        record.setRemoteIp(request.getUri().getHost());
        String url = request.getUrl();
        record.setService(url);
        record.setMethod(StringUtils.upperCase(request.getMethod()));
        record.setPort(request.getUri().getPort());
        if (CollectionUtils.isNotEmpty(request.getQueryParams())) {
            StringBuilder params = new StringBuilder();
            for (Param param : request.getQueryParams()) {
                params.append(param.getName()).append("=").append(param.getValue()).append(",");
            }
            record.setRequest(params.toString());
            record.setRequestSize(params.length());
        }
        record.setRequestSize(0);
        record.setContextInject(new ContextInject() {
            @Override
            public void injectContext(final Map<String, String> context) {
                final AsyncHandler asyncHandler = (AsyncHandler) advice.getParameterArray()[1];
                advice.changeParameter(1, new AsyncHandler() {
                    @Override
                    public State onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
                        Pradar.setInvokeContext(context);
                        if (asyncHandler != null) {
                            return asyncHandler.onStatusReceived(responseStatus);
                        }
                        return State.CONTINUE;
                    }

                    @Override
                    public State onHeadersReceived(HttpHeaders headers) throws Exception {
                        Pradar.setInvokeContext(context);
                        if (asyncHandler != null) {
                            return asyncHandler.onHeadersReceived(headers);
                        }
                        return State.CONTINUE;
                    }

                    @Override
                    public State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
                        Pradar.setInvokeContext(context);
                        if (asyncHandler != null) {
                            return asyncHandler.onBodyPartReceived(bodyPart);
                        }
                        return State.CONTINUE;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        Pradar.setInvokeContext(context);
                        try {
                            if (asyncHandler != null) {
                                asyncHandler.onThrowable(t);
                            }
                        } finally {
                            Pradar.response(t);
                            Pradar.endClientInvoke(ResultCode.INVOKE_RESULT_FAILED, getPluginType());
                        }
                    }

                    @Override
                    public Object onCompleted() throws Exception {
                        Pradar.setInvokeContext(context);
                        Object result = null;
                        try {
                            if (asyncHandler != null) {
                                result = asyncHandler.onCompleted();
                            }
                            return result;
                        } finally {
                            Pradar.response(result);
                            Pradar.endClientInvoke(ResultCode.INVOKE_RESULT_SUCCESS, getPluginType());
                        }
                    }

                    @Override
                    public State onTrailingHeadersReceived(HttpHeaders headers) throws Exception {
                        Pradar.setInvokeContext(context);
                        if (asyncHandler != null) {
                            return asyncHandler.onTrailingHeadersReceived(headers);
                        }
                        return State.CONTINUE;
                    }

                    @Override
                    public void onHostnameResolutionAttempt(String name) {
                        Pradar.setInvokeContext(context);
                        if (asyncHandler != null) {
                            asyncHandler.onHostnameResolutionAttempt(name);
                        }
                    }

                    @Override
                    public void onHostnameResolutionSuccess(String name, List list) {
                        Pradar.setInvokeContext(context);
                        if (asyncHandler != null) {
                            asyncHandler.onHostnameResolutionSuccess(name, list);
                        }
                    }

                    @Override
                    public void onHostnameResolutionFailure(String name, Throwable cause) {
                        Pradar.setInvokeContext(context);
                        if (asyncHandler != null) {
                            asyncHandler.onHostnameResolutionFailure(name, cause);
                        }
                    }

                    @Override
                    public void onTcpConnectAttempt(InetSocketAddress remoteAddress) {
                        Pradar.setInvokeContext(context);
                        if (asyncHandler != null) {
                            asyncHandler.onTcpConnectAttempt(remoteAddress);
                        }
                    }

                    @Override
                    public void onTcpConnectSuccess(InetSocketAddress remoteAddress, Channel connection) {
                        Pradar.setInvokeContext(context);
                        if (asyncHandler != null) {
                            asyncHandler.onTcpConnectSuccess(remoteAddress, connection);
                        }
                    }

                    @Override
                    public void onTcpConnectFailure(InetSocketAddress remoteAddress, Throwable cause) {
                        Pradar.setInvokeContext(context);
                        if (asyncHandler != null) {
                            asyncHandler.onTcpConnectFailure(remoteAddress, cause);
                        }
                    }

                    @Override
                    public void onTlsHandshakeAttempt() {
                        Pradar.setInvokeContext(context);
                        if (asyncHandler != null) {
                            asyncHandler.onTlsHandshakeAttempt();
                        }
                    }

                    @Override
                    public void onTlsHandshakeSuccess(SSLSession sslSession) {
                        Pradar.setInvokeContext(context);
                        if (asyncHandler != null) {
                            asyncHandler.onTlsHandshakeSuccess(sslSession);
                        }
                    }

                    @Override
                    public void onTlsHandshakeFailure(Throwable cause) {
                        Pradar.setInvokeContext(context);
                        if (asyncHandler != null) {
                            asyncHandler.onTlsHandshakeFailure(cause);
                        }
                    }

                    @Override
                    public void onConnectionPoolAttempt() {
                        Pradar.setInvokeContext(context);
                        if (asyncHandler != null) {
                            asyncHandler.onConnectionPoolAttempt();
                        }
                    }

                    @Override
                    public void onConnectionPooled(Channel connection) {
                        Pradar.setInvokeContext(context);
                        if (asyncHandler != null) {
                            asyncHandler.onConnectionPooled(connection);
                        }
                    }

                    @Override
                    public void onConnectionOffer(Channel connection) {
                        Pradar.setInvokeContext(context);
                        if (asyncHandler != null) {
                            asyncHandler.onConnectionOffer(connection);
                        }
                    }

                    @Override
                    public void onRequestSend(NettyRequest request) {
                        Pradar.setInvokeContext(context);
                        if (asyncHandler != null) {
                            asyncHandler.onRequestSend(request);
                        }
                    }

                    @Override
                    public void onRetry() {
                        Pradar.setInvokeContext(context);
                        if (asyncHandler != null) {
                            asyncHandler.onRetry();
                        }
                    }
                });
            }
        });
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
