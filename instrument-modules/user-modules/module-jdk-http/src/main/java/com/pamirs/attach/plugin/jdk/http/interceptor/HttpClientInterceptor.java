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
package com.pamirs.attach.plugin.jdk.http.interceptor;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.attach.plugin.jdk.http.JdkHttpConstants;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.ContextTransfer;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.utils.InnerWhiteListCheckUtil;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.net.www.MessageHeader;
import sun.net.www.http.HttpClient;
import sun.net.www.http.PosterOutputStream;

import java.lang.reflect.Field;
import java.net.SocketTimeoutException;
import java.util.Map;

/**
 * @Auther: vernon
 * @Date: 2020/3/24 23:33
 * @Description:
 */
public class HttpClientInterceptor extends TraceInterceptorAdaptor {
    private final static Logger LOGGER = LoggerFactory.getLogger(HttpClientInterceptor.class);

    private static volatile Field FIELD = null;

    @Override
    public String getPluginName() {
        return JdkHttpConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return JdkHttpConstants.PLUGIN_TYPE;
    }

    @Override
    public void beforeLast(Advice advice) {
        InnerWhiteListCheckUtil.check();
        ClusterTestUtils.validateClusterTest();
        HttpClient client = (HttpClient) advice.getTarget();

        MessageHeader header = ReflectionUtils.get(client, JdkHttpConstants.DYNAMIC_FIELD_REQUESTS);
        if (header == null) {
            header = new MessageHeader();
        }
        Map<String, String> ctx = Pradar.getInvokeContextTransformMap();
        for (Map.Entry<String, String> entry : ctx.entrySet()) {
            header.set(entry.getKey(), entry.getValue());
        }
        ReflectionUtils.set(client, JdkHttpConstants.DYNAMIC_FIELD_REQUESTS, header);
    }

    @Override
    protected ContextTransfer getContextTransfer(Advice advice) {
        final MessageHeader head = (MessageHeader) advice.getParameterArray()[0];
        return new ContextTransfer() {
            @Override
            public void transfer(String key, String value) {
                head.set(key, value);
            }
        };
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        InnerWhiteListCheckUtil.check();

        PosterOutputStream stream = (PosterOutputStream) advice.getParameterArray()[1];
        SpanRecord spanRecord = HttpURLConnectionInterceptor.traceLocalCache.get();
        if(spanRecord != null && stream != null){
            spanRecord.setRequest(new String(stream.toByteArray()));
        }
        return spanRecord;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        SpanRecord spanRecord = HttpURLConnectionInterceptor.traceLocalCache.get();
        if(spanRecord == null){
            return null;
        }
        Pradar.getInvokeContext().setStartTime(HttpURLConnectionInterceptor.connectTimeLocalCache.get());
        SpanRecord record = new SpanRecord();
        record.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
        InnerWhiteListCheckUtil.check();
        return record;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        SpanRecord spanRecord = HttpURLConnectionInterceptor.traceLocalCache.get();
        if(spanRecord == null){
            return null;
        }
        Pradar.getInvokeContext().setStartTime(HttpURLConnectionInterceptor.connectTimeLocalCache.get());
        SpanRecord record = new SpanRecord();
        if (advice.getThrowable() instanceof SocketTimeoutException) {
            record.setResultCode(ResultCode.INVOKE_RESULT_TIMEOUT);
        } else {
            record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        }
        record.setResponse(advice.getThrowable());
        InnerWhiteListCheckUtil.check();
        return record;
    }
}
