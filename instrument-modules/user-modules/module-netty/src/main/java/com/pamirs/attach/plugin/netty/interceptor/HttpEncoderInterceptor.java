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
package com.pamirs.attach.plugin.netty.interceptor;

import com.pamirs.attach.plugin.netty.NettyConstants;
import com.pamirs.pradar.InvokeContext;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.ContextTransfer;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/12/15 7:14 下午
 */
public class HttpEncoderInterceptor extends TraceInterceptorAdaptor {
    @Resource
    protected DynamicFieldManager manager;

    @Override
    public String getPluginName() {
        return NettyConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return NettyConstants.PLUGIN_TYPE;
    }

    @Override
    protected boolean isTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        final HttpMessage httpMessage = (HttpMessage) args[1];
        Map<String, String> context = manager.getDynamicField(httpMessage, NettyConstants.DYNAMIC_FIELD_ASYNC_CONTEXT);
        if (!Pradar.hasInvokeContext(context) && !Pradar.hasInvokeContext()) {
            return false;
        }

        String traceId = httpMessage.headers().get(PradarService.PRADAR_TRACE_ID_KEY);
        return traceId == null;
    }

    @Override
    protected ContextTransfer getContextTransfer(Advice advice) {
        Object[] args = advice.getParameterArray();
        final HttpMessage httpMessage = (HttpMessage) args[1];
        Map<String, String> context = manager.getDynamicField(httpMessage, NettyConstants.DYNAMIC_FIELD_ASYNC_CONTEXT);
        InvokeContext invokeContext_ = Pradar.fromMap(context);
        if (Pradar.hasInvokeContext(context) || Pradar.hasInvokeContext()) {
            return new ContextTransfer() {
                @Override
                public void transfer(String key, String value) {
                    HttpHeaders headers = httpMessage.headers();
                    if (headers != null && !headers.contains(key)) {
                        headers.set(key, value);
                    }
                }
            };
        }
        return null;
    }

    @Override
    protected boolean isClient(Advice advice) {
        Object[] args = advice.getParameterArray();
        final HttpMessage httpMessage = (HttpMessage) args[1];
        Map<String, String> context = manager.getDynamicField(httpMessage, NettyConstants.DYNAMIC_FIELD_ASYNC_CONTEXT);
        return Pradar.hasInvokeContext(context) || Pradar.hasInvokeContext();
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        final HttpMessage httpMessage = (HttpMessage) args[1];
        final ChannelHandlerContext channelHandlerContext = (ChannelHandlerContext) args[0];
        Map<String, String> context = manager.getDynamicField(httpMessage, NettyConstants.DYNAMIC_FIELD_ASYNC_CONTEXT);
        if (!Pradar.hasInvokeContext(context)) {
            context = Pradar.getInvokeContextMap();
        }

        /**
         * 如果有上下文，则说明是发送请求，作为客户端
         * 否则是作为服务端接收请求
         */
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setContext(context);
        spanRecord.setRemoteIp(getHost(channelHandlerContext));
        spanRecord.setPort(getPort(channelHandlerContext));
        spanRecord.setService(getUrl(httpMessage));
        spanRecord.setMethod(getMethod(httpMessage));
        if (!Pradar.hasInvokeContext(context)) {
            spanRecord.setClusterTest(isClusterTestRequest(httpMessage));
            Map<String, String> ctx = new HashMap<String, String>();
            for (String key : Pradar.getInvokeContextTransformKeys()) {
                String value = httpMessage.headers().get(key);
                if (value != null) {
                    ctx.put(key, value);
                }
            }
            if (!ctx.isEmpty()) {
                spanRecord.setContext(ctx);
            }
        }
        return spanRecord;

    }

    /**
     * 判断是否是压测流量
     *
     * @param httpMessage
     * @return
     */
    public static boolean isClusterTestRequest(HttpMessage httpMessage) {
        String value = httpMessage.headers().get(PradarService.PRADAR_CLUSTER_TEST_KEY);
        if (StringUtils.isBlank(value)) {
            value = httpMessage.headers().get(PradarService.PRADAR_HTTP_CLUSTER_TEST_KEY);
        }
        return ClusterTestUtils.isClusterTestRequest(value);
    }

    private String getUrl(HttpMessage message) {
        if (message instanceof HttpRequest) {
            String url = ((HttpRequest) message).getUri();
            if (url != null) {
                final int indexOfQuestion = url.indexOf('?');
                if (indexOfQuestion != -1) {
                    return url.substring(0, indexOfQuestion);
                }
            }
            return url;
        }
        return null;
    }

    private String getMethod(HttpMessage message) {
        if (message instanceof HttpRequest) {
            return ((HttpRequest) message).getMethod().name();
        }
        return null;
    }

    private String getHost(ChannelHandlerContext channelHandlerContext) {
        if (channelHandlerContext != null) {
            final Channel channel = channelHandlerContext.channel();
            if (channel != null && channel.remoteAddress() != null) {
                if (channel.remoteAddress() instanceof InetSocketAddress) {
                    InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
                    return address.getAddress().getHostAddress();
                } else {
                    return channel.remoteAddress().toString();
                }
            }
        }
        return null;
    }

    public static int getPort(ChannelHandlerContext channelHandlerContext) {
        if (channelHandlerContext != null) {
            final Channel channel = channelHandlerContext.channel();
            if (channel != null && channel.remoteAddress() != null) {
                if (channel.remoteAddress() instanceof InetSocketAddress) {
                    InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
                    return address.getPort();
                } else {
                    return 0;
                }
            }
        }
        return 0;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        final HttpMessage httpMessage = (HttpMessage) args[1];
        SpanRecord record = new SpanRecord();
        record.setResultCode(httpMessage.getDecoderResult().isFailure() ? ResultCode.INVOKE_RESULT_FAILED : ResultCode.INVOKE_RESULT_SUCCESS);
        return record;

    }

    @Override
    public void afterLast(Advice advice) {
        Object[] args = advice.getParameterArray();
        final HttpMessage httpMessage = (HttpMessage) args[1];
        manager.removeAll(httpMessage);
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        SpanRecord record = new SpanRecord();
        record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        return record;
    }

    @Override
    public void exceptionLast(Advice advice) {
        Object[] args = advice.getParameterArray();
        final HttpMessage httpMessage = (HttpMessage) args[1];
        manager.removeAll(httpMessage);
    }
}
