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
package com.pamirs.attach.plugin.async.http.client.plugin.interceptor;


import com.pamirs.attach.plugin.async.http.client.plugin.AsyncHttpClientConstants;
import com.pamirs.attach.plugin.async.http.client.plugin.utils.DefaultListenableFuture;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.ContextTransfer;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.internal.adapter.ExecutionStrategy;
import com.pamirs.pradar.internal.config.MatchConfig;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.pressurement.mock.JsonMockStrategy;
import com.pamirs.pradar.pressurement.mock.MockStrategy;
import com.pamirs.pradar.script.ScriptEvaluator;
import com.pamirs.pradar.script.ScriptManager;
import com.pamirs.pradar.utils.InnerWhiteListCheckUtil;
import com.shulie.instrument.simulator.api.ProcessControlException;
import com.shulie.instrument.simulator.api.ProcessController;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.util.CollectionUtils;
import io.netty.handler.codec.http.HttpHeaders;
import org.apache.commons.lang.StringUtils;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.Param;
import org.asynchttpclient.Request;

/**
 * @author angju
 * @date 2021/4/6 20:39
 */
public class NettyRequestSenderSendRequestInterceptor extends TraceInterceptorAdaptor {

    @Override
    public String getPluginName() {
        return AsyncHttpClientConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return AsyncHttpClientConstants.PLUGIN_TYPE;
    }

    private JsonMockStrategy fixJsonStrategy = new JsonMockStrategy() {
        @Override
        public Object processBlock(Class returnType, ClassLoader classLoader, Object params) throws ProcessControlException {
            MatchConfig config = (MatchConfig) params;
            Pradar.mockResponse(config.getScriptContent());
            Object[] parameters = (Object[]) config.getArgs().get("parameters");
            AsyncCompletionHandler handler = null;
            if (parameters.length > 0 && parameters[1] instanceof AsyncCompletionHandler) {
                handler = (AsyncCompletionHandler) parameters[1];
            }
            ProcessController.returnImmediately(new DefaultListenableFuture(config.getScriptContent(), handler));
            return null;
        }
    };

    private MockStrategy mockStrategy = new MockStrategy() {
        @Override
        public Object processBlock(Class returnType, ClassLoader classLoader, Object params) throws ProcessControlException {
            MatchConfig config = (MatchConfig) params;
            ScriptEvaluator evaluator = ScriptManager.getInstance().getScriptEvaluator("bsh");
            Object result = evaluator.evaluate(config.getScriptContent(), config.getArgs());

            Object[] parameters = (Object[]) config.getArgs().get("parameters");
            AsyncCompletionHandler handler = null;
            if (parameters.length > 0 && parameters[1] instanceof AsyncCompletionHandler) {
                handler = (AsyncCompletionHandler) parameters[1];
            }
            Pradar.mockResponse(result);
            ProcessController.returnImmediately(new DefaultListenableFuture(result, handler));
            return null;
        }
    };

    @Override
    public void beforeLast(Advice advice) throws ProcessControlException {
        if (!ClusterTestUtils.enableMock()) {
            return;
        }
        Request request = (Request) advice.getParameterArray()[0];
        String url = request.getUri().getPath();
        final MatchConfig config = ClusterTestUtils.httpClusterTest(url);
        config.addArgs("url", url);
        config.addArgs("request", request);
        config.addArgs("method", "uri");
        config.addArgs("isInterface", Boolean.FALSE);
        config.addArgs("advice", advice);
        config.addArgs("parameters", advice.getParameterArray());

        ExecutionStrategy strategy = config.getStrategy();
        if (strategy instanceof JsonMockStrategy) {
            fixJsonStrategy.processBlock(advice.getBehavior().getReturnType(), advice.getClassLoader(), config);
        }
        if (strategy instanceof MockStrategy) {
            mockStrategy.processBlock(advice.getBehavior().getReturnType(), advice.getClassLoader(), config);
        }
        strategy.processBlock(advice.getBehavior().getReturnType(), advice.getClassLoader(), config);
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
       /* String url = request.getUrl();
        record.setService(url);*/
        String uri = request.getUri().getPath();
        record.setService(uri);
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
        return record;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        SpanRecord record = new SpanRecord();
        record.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
        record.setResponse(advice.getReturnObj());
        InnerWhiteListCheckUtil.check();
        record.setResponseSize(0);
        return record;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        SpanRecord record = new SpanRecord();
        record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        record.setResponse(advice.getThrowable());
        InnerWhiteListCheckUtil.check();
        record.setResponseSize(0);
        return record;
    }
}
