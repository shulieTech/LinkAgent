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

import com.pamirs.attach.plugin.jdk.http.JdkHttpConstants;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.ContextTransfer;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.internal.config.MatchConfig;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.utils.InnerWhiteListCheckUtil;
import com.shulie.instrument.simulator.api.ProcessControlException;
import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

@ListenerBehavior(isFilterBusinessData = true)
public class HttpURLConnectionInterceptor extends TraceInterceptorAdaptor {
    private final static String MARK_CONNECTING = "connecting";
    private final static String MARK_CONNECTED = "connected";

    private static volatile Field CONNECTED_FiELD = null;
    private static volatile Field CONNECTING_FiELD = null;

    private static String getService(String schema, String host, int port, String path) {
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

        /**
         * 保持trace一致
         */
        boolean connecting = advice.hasMark(MARK_CONNECTING);
        boolean connected = advice.hasMark(MARK_CONNECTED);
        if (connected || connecting) {
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
        boolean connecting = advice.hasMark(MARK_CONNECTING);
        boolean connected = advice.hasMark(MARK_CONNECTED);
        if (connected || connecting) {
            return null;
        }
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
        Object target = advice.getTarget();
        final HttpURLConnection request = (HttpURLConnection) target;

        boolean connected = false;
        try {
            initConnectedField(target);
            connected = Reflect.on(target).get(CONNECTED_FiELD);
        } catch (ReflectException e) {
            LOGGER.warn("{} has not field {}", target.getClass().getName(), JdkHttpConstants.DYNAMIC_FIELD_CONNECTED);
        }

        boolean connecting = false;
        try {
            initConnectingField(target);
            connecting = Reflect.on(target).get(CONNECTING_FiELD);
        } catch (ReflectException e) {
            LOGGER.warn("{} has not field {}", target.getClass().getName(), JdkHttpConstants.DYNAMIC_FIELD_CONNECTING);
        }
        if (connected) {
            advice.mark(MARK_CONNECTED);
        }
        if (connecting) {
            advice.mark(MARK_CONNECTING);
        }
        if (connected || connecting) {
            return null;
        }

        SpanRecord record = new SpanRecord();
        String host = request.getURL().getHost();
        int port = request.getURL().getPort();
        String path = request.getURL().getPath();
        record.setService(path);
        record.setMethod(StringUtils.upperCase(request.getRequestMethod()));
        record.setRemoteIp(host);
        record.setPort(port);
        return record;
    }

    private void initConnectedField(Object target) {
        if (CONNECTED_FiELD == null) {
            synchronized (HttpURLConnectionInterceptor.class) {
                if (CONNECTED_FiELD == null) {
                    CONNECTED_FiELD = Reflect.on(target).field0(JdkHttpConstants.DYNAMIC_FIELD_CONNECTED);
                }
            }
        }
    }

    private void initConnectingField(Object target) {
        if (CONNECTING_FiELD == null) {
            synchronized (HttpURLConnectionInterceptor.class) {
                if (CONNECTING_FiELD == null) {
                    CONNECTING_FiELD = Reflect.on(target).field0(JdkHttpConstants.DYNAMIC_FIELD_CONNECTING);
                }
            }
        }
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        try {
            boolean connecting = advice.hasMark(MARK_CONNECTING);
            boolean connected = advice.hasMark(MARK_CONNECTED);
            if (connected || connecting) {
                return null;
            }
            SpanRecord record = new SpanRecord();
            record.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
            InnerWhiteListCheckUtil.check();
            return record;
        } finally {
            advice.unMark(MARK_CONNECTING);
            advice.unMark(MARK_CONNECTED);
        }
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        try {
            boolean connecting = advice.hasMark(MARK_CONNECTING);
            boolean connected = advice.hasMark(MARK_CONNECTED);
            if (connected || connecting) {
                return null;
            }
            SpanRecord record = new SpanRecord();
            if (advice.getThrowable() instanceof SocketTimeoutException) {
                record.setResultCode(ResultCode.INVOKE_RESULT_TIMEOUT);
            } else {
                record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
            }
            record.setResponse(advice.getThrowable());
            InnerWhiteListCheckUtil.check();

            return record;
        } finally {
            advice.unMark(MARK_CONNECTING);
            advice.unMark(MARK_CONNECTED);
        }
    }

}
