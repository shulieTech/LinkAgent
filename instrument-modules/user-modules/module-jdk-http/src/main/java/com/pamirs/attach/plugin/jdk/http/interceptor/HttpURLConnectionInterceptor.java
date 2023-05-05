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
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.ContextTransfer;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.internal.adapter.ExecutionStrategy;
import com.pamirs.pradar.internal.config.MatchConfig;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.pressurement.mock.JsonMockStrategy;
import com.pamirs.pradar.pressurement.mock.MockStrategy;
import com.pamirs.pradar.utils.InnerWhiteListCheckUtil;
import com.shulie.instrument.simulator.api.ProcessControlException;
import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.StringUtils;

import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

@ListenerBehavior(isFilterBusinessData = true)
public class HttpURLConnectionInterceptor extends TraceInterceptorAdaptor {

    public static ThreadLocal<SpanRecord> traceLocalCache = new ThreadLocal<SpanRecord>();

    protected static String getService(String schema, String host, int port, String path) {
        String url = schema + "://" + host;
        if (port != -1 && port != 80) {
            url = url + ':' + port;
        }
        return url + path;
    }

    @Override
    public String getPluginName() {
        return JdkHttpConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return JdkHttpConstants.PLUGIN_TYPE;
    }

    @Override
    public void beforeLast(Advice advice) throws ProcessControlException {
        if (!Pradar.isClusterTest()) {
            return;
        }
        Object target = advice.getTarget();
        final HttpURLConnection request = (HttpURLConnection) target;
        final URL url = request.getURL();
        String fullPath = getService(url.getProtocol(),
                url.getHost(),
                url.getPort(),
                url.getPath());
        String whiteList = request.getRequestProperty(PradarService.PRADAR_WHITE_LIST_CHECK);

        MatchConfig config = ClusterTestUtils.httpClusterTest(fullPath);
        ExecutionStrategy strategy = config.getStrategy();
        // mock不在connect里执行
        if (strategy instanceof JsonMockStrategy || strategy instanceof MockStrategy) {
            return;
        }
        config.addArgs(PradarService.PRADAR_WHITE_LIST_CHECK, whiteList);
        // 白名单需要的信息
        config.addArgs("url", fullPath);
        // mock转发需要信息
        config.addArgs("request", request);
        config.addArgs("method", "url");
        config.addArgs("isInterface", Boolean.FALSE);
        config.getStrategy().processBlock(advice.getBehavior().getReturnType(), advice.getClassLoader(), config);
    }

    @Override
    protected ContextTransfer getContextTransfer(Advice advice) {
        Object target = advice.getTarget();
        final HttpURLConnection request = (HttpURLConnection) target;
        return new ContextTransfer() {
            @Override
            public void transfer(String key, String value) {
                request.setRequestProperty(key, value);
            }
        };
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        InnerWhiteListCheckUtil.check();
        final HttpURLConnection request = (HttpURLConnection) advice.getTarget();
        // post和put请求
        String method = request.getRequestMethod();
        SpanRecord record = new SpanRecord();
        String host = request.getURL().getHost();
        int port = request.getURL().getPort();
        String path = request.getURL().getPath();
        record.setService(path);
        record.setMethod(StringUtils.upperCase(method));
        record.setRemoteIp(host);
        record.setPort(port);

        // post和put请求需要在sun.net.www.http.HttpClient.writeRequests(sun.net.www.MessageHeader, sun.net.www.http.PosterOutputStream)方法打印trace
        if("post".equalsIgnoreCase(method) || "put".equalsIgnoreCase(method)){
            traceLocalCache.set(record);
            return null;
        }

        boolean connected = ReflectionUtils.get(request, JdkHttpConstants.DYNAMIC_FIELD_CONNECTED);
        boolean connecting = ReflectionUtils.get(request, JdkHttpConstants.DYNAMIC_FIELD_CONNECTING);
        if(connected || connecting){
            advice.mark("connect");
            return null;
        }
        return record;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        if(advice.hasMark("connect")){
            return null;
        }
        final HttpURLConnection request = (HttpURLConnection) advice.getTarget();
        // post和put请求
        String method = request.getRequestMethod();
        if("post".equalsIgnoreCase(method) || "put".equalsIgnoreCase(method)){
            return null;
        }
        SpanRecord record = new SpanRecord();
        record.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
        record.setMethod(StringUtils.upperCase(((HttpURLConnection) advice.getTarget()).getRequestMethod()));
        InnerWhiteListCheckUtil.check();
        return record;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        if(advice.hasMark("connect")){
            return null;
        }
        final HttpURLConnection request = (HttpURLConnection) advice.getTarget();

        String method = request.getRequestMethod();
        if("post".equalsIgnoreCase(method) || "put".equalsIgnoreCase(method)){
            return null;
        }
        SpanRecord record = new SpanRecord();
        if (advice.getThrowable() instanceof SocketTimeoutException) {
            record.setResultCode(ResultCode.INVOKE_RESULT_TIMEOUT);
        } else {
            record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        }
        record.setMethod(StringUtils.upperCase(((HttpURLConnection) advice.getTarget()).getRequestMethod()));
        record.setResponse(advice.getThrowable());
        InnerWhiteListCheckUtil.check();
        return record;

    }

}
