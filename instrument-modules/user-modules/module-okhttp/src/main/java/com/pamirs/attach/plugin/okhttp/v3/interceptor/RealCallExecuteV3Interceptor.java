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
package com.pamirs.attach.plugin.okhttp.v3.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.pamirs.attach.plugin.okhttp.OKHttpConstants;
import com.pamirs.attach.plugin.okhttp.utils.MockReturnUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.internal.adapter.ExecutionForwardCall;
import com.pamirs.pradar.internal.config.MatchConfig;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.pressurement.mock.JsonMockStrategy;
import com.pamirs.pradar.utils.InnerWhiteListCheckUtil;
import com.shulie.instrument.simulator.api.ProcessControlException;
import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import okhttp3.*;
import okhttp3.internal.http.RealResponseBody;
import okio.Buffer;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/6/30 10:20 上午
 */
@ListenerBehavior(isFilterBusinessData = true)
public class RealCallExecuteV3Interceptor extends TraceInterceptorAdaptor {
    @Override
    public String getPluginName() {
        return OKHttpConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return OKHttpConstants.PLUGIN_TYPE;
    }




    @Override
    public void beforeFirst(Advice advice) throws ProcessControlException {
        if (!Pradar.isClusterTest()) {
            return;
        }
        Object target = advice.getTarget();
        final Call call = (Call) target;
        final HttpUrl httpUrl = call.request().url();
        String url = OKHttpConstants.getService(httpUrl.scheme(), httpUrl.host(), httpUrl.port(), httpUrl.encodedPath());

        final MatchConfig config = ClusterTestUtils.httpClusterTest(url);
        String check = call.request().header(PradarService.PRADAR_WHITE_LIST_CHECK);
        config.addArgs(PradarService.PRADAR_WHITE_LIST_CHECK, check);
        config.addArgs("url", url);
        config.addArgs("isInterface", Boolean.FALSE);
        if (config.getStrategy() instanceof JsonMockStrategy){
            config.addArgs("call", call);
            MockReturnUtils.fixJsonStrategy.processBlock(advice.getBehavior().getReturnType(), advice.getClassLoader(), config);
        }
        config.getStrategy().processBlock(advice.getBehavior().getReturnType(), advice.getClassLoader(), config,
                new ExecutionForwardCall() {
                    @Override
                    public Object forward(Object param) throws ProcessControlException {
                        String configUrl = config.getForwarding();
                        if (httpUrl.queryParameterNames().size() > 0) {
                            StringBuilder builder = new StringBuilder(128).append(configUrl).append("?");
                            boolean first = true;
                            for (String queryParameterName : httpUrl.queryParameterNames()) {
                                if (!first) {
                                    builder.append("&");
                                }
                                builder.append(queryParameterName).append("=").append(
                                        httpUrl.queryParameter(queryParameterName));
                                first = false;
                            }
                            configUrl = builder.toString();
                        }
                        HttpUrl forwardHttpUrl = HttpUrl.parse(configUrl);
                        Reflect.on(call.request()).set("url", forwardHttpUrl);
                        return null;
                    }

                    @Override
                    public Object call(Object param) {
                        Headers header = new Headers.Builder().build();
                        Buffer buffer = new Buffer();

                        try {
                            if (param instanceof String) {
                                buffer.write(String.valueOf(param).getBytes("UTF-8"));
                            } else {
                                buffer.write(JSONObject.toJSONBytes(param));
                            }

                        } catch (IOException e) {
                        }

                        return new Response.Builder().code(200)
                                .body(new RealResponseBody(header, buffer))
                                .request(call.request())
                                .protocol(Protocol.HTTP_1_0)
                                .message("OK")
                                .build();
                    }
                });
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        Object target = advice.getTarget();
        Call call = (Call) target;

        SpanRecord record = new SpanRecord();
        HttpUrl url = call.request().url();
        record.setRemoteIp(url.host());
        record.setService(url.encodedPath());
        record.setMethod(StringUtils.upperCase(call.request().method()));
        record.setRemoteIp(url.host());
        record.setPort(url.port());
        record.setRequest(url.encodedQuery());

        String header = call.request().header("content-length");
        if (StringUtils.isNotBlank(header) && NumberUtils.isDigits(header)) {
            try {
                record.setRequestSize(Integer.valueOf(header));
            } catch (NumberFormatException e) {
            }
        }
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
        Response response = (Response) advice.getReturnObj();
        record.setResultCode(String.valueOf(response.code()));
        final long length = response.body().contentLength();
        InnerWhiteListCheckUtil.check();
        record.setResponseSize(length < 0 ? 0 : length);
        return record;
    }
}
