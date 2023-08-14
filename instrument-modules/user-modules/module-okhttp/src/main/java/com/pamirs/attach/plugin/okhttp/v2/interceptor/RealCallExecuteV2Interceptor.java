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
package com.pamirs.attach.plugin.okhttp.v2.interceptor;

import com.pamirs.attach.plugin.okhttp.OKHttpConstants;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.gson.GsonFactory;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.internal.adapter.ExecutionForwardCall;
import com.pamirs.pradar.internal.config.MatchConfig;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.utils.InnerWhiteListCheckUtil;
import com.shulie.instrument.simulator.api.ProcessControlException;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.internal.http.RealResponseBody;
import okhttp3.HttpUrl;
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
public class RealCallExecuteV2Interceptor extends TraceInterceptorAdaptor {
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
        Object target = advice.getTarget();
        Request request = null;
        try {
            request = Reflect.on(target).get(OKHttpConstants.DYNAMIC_FIELD_REQUEST);
        } catch (ReflectException e) {
            request = Reflect.on(target).get(OKHttpConstants.DYNAMIC_FIELD_ORIGINAL_REQUEST);
        }

        String url = request.urlString();
        final MatchConfig config = ClusterTestUtils.httpClusterTest(url);
        String check = request.header(OKHttpConstants.DYNAMIC_FIELD_HEADER);
        config.addArgs(PradarService.PRADAR_WHITE_LIST_CHECK, check);
        config.addArgs("url", url);
        config.addArgs("isInterface", Boolean.FALSE);
        final Request finalRequest = request;
        config.getStrategy().processBlock(advice.getBehavior().getReturnType(), advice.getClassLoader(), config, new ExecutionForwardCall() {
            @Override
            public Object forward(Object param) throws ProcessControlException {
                HttpUrl httpUrl = HttpUrl.parse(config.getForwarding());
                Reflect.on(finalRequest).set("url", httpUrl);
                return null;
            }

            @Override
            public Object call(Object param) {
                Headers header = Headers.of(new String[0]);
                Buffer buffer = new Buffer();

                try {
                    byte[] bytes = null;
                    if (param instanceof String) {
                        buffer.write(String.valueOf(param).getBytes("UTF-8"));
                    } else {
                        buffer.write(GsonFactory.getGson().toJson(param).getBytes());
                    }
                } catch (IOException e) {
                }

                return new Response.Builder().code(200)
                        .body(new RealResponseBody(header,buffer))
                        .request(finalRequest)
                        .protocol(Protocol.HTTP_1_0)
                        .message("OK")
                        .build();
            }
        });
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        InnerWhiteListCheckUtil.check();
        Object target = advice.getTarget();
        Request request = null;
        try {
            request = Reflect.on(target).get(OKHttpConstants.DYNAMIC_FIELD_REQUEST);
        } catch (ReflectException e) {
            request = Reflect.on(target).get(OKHttpConstants.DYNAMIC_FIELD_ORIGINAL_REQUEST);
        }
        SpanRecord record = new SpanRecord();
        record.setRemoteIp(request.url().getHost());
        record.setService(request.url().getPath());
        record.setMethod(StringUtils.upperCase(request.method()));
        record.setRemoteIp(request.url().getHost());
        record.setPort(request.url().getPort());
        record.setRequest(request.url().getQuery());

        String header = request.header("content-length");
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
        InnerWhiteListCheckUtil.check();
        long length = 0;
        try {
            length = response.body().contentLength();
        } catch (IOException e) {
            LOGGER.warn("error {}", e);
        }
        record.setResponseSize(length < 0 ? 0 : length);
        return record;
    }
}
