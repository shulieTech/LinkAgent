package com.pamirs.attach.plugin.httpclient.utils;

import com.alibaba.fastjson.JSON;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.internal.adapter.ExecutionStrategy;
import com.pamirs.pradar.internal.config.MatchConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.mock.MockStrategy;
import com.pamirs.pradar.script.ScriptEvaluator;
import com.pamirs.pradar.script.ScriptManager;
import com.shulie.instrument.simulator.api.ProcessControlException;
import com.shulie.instrument.simulator.api.ProcessController;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientAsyncMockStrategies {

    private final static Logger LOGGER = LoggerFactory.getLogger(MockStrategy.class.getName());

    public static final ExecutionStrategy HTTPCLIENT4_MOCK_STRATEGY = new MockStrategy() {
        @Override
        public Object processBlock(Class returnType, ClassLoader classLoader, Object params) throws ProcessControlException {
            try {
                MatchConfig config = (MatchConfig) params;
                String scriptContent = config.getScriptContent();
                ScriptEvaluator evaluator = ScriptManager.getInstance().getScriptEvaluator("bsh");
                Object result = evaluator.evaluate(scriptContent, config.getArgs());

                FutureCallback<HttpResponse> futureCallback = (FutureCallback<HttpResponse>) config.getArgs().get("futureCallback");
                StatusLine statusline = new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "");

                HttpEntity entity = null;
                entity = new StringEntity(result instanceof String ? (String) result : JSON.toJSONString(result));

                BasicHttpResponse response = new BasicHttpResponse(statusline);
                response.setEntity(entity);

                java.util.concurrent.CompletableFuture future = new java.util.concurrent.CompletableFuture();
                future.complete(response);

                if (futureCallback != null) {
                    futureCallback.completed(response);
                }
                ProcessController.returnImmediately(returnType, future);
            } catch (ProcessControlException pe) {
                throw pe;
            } catch (Exception e) {
                LOGGER.error("mock处理异常 {}", e);
                ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.mock)
                        .setErrorCode("mock-0003")
                        .setMessage("mock处理异常！" + e.getMessage())
                        .setDetail("脚本内容" + ((MatchConfig) params).getScriptContent())
                        .report();
            }
            return null;
        }
    };

    public static final ExecutionStrategy HTTPCLIENT5_MOCK_STRATEGY = new MockStrategy() {
        @Override
        public Object processBlock(Class returnType, ClassLoader classLoader, Object params) throws ProcessControlException {

            try {
                MatchConfig config = (MatchConfig) params;
                String scriptContent = config.getScriptContent();
                ScriptEvaluator evaluator = ScriptManager.getInstance().getScriptEvaluator("bsh");
                Object result = evaluator.evaluate(scriptContent, config.getArgs());

                org.apache.hc.core5.concurrent.FutureCallback<SimpleHttpResponse> futureCallback = (org.apache.hc.core5.concurrent.FutureCallback<SimpleHttpResponse>) config.getArgs().get("futureCallback");

                String ret = result instanceof String ? (String) result : JSON.toJSONString(result);
                SimpleHttpResponse response = SimpleHttpResponse.create(200, ret);
                java.util.concurrent.CompletableFuture future = new java.util.concurrent.CompletableFuture();
                future.complete(response);

                futureCallback.completed(response);
                ProcessController.returnImmediately(returnType, future);
            } catch (ProcessControlException pe) {
                throw pe;
            } catch (Exception e) {
                LOGGER.error("mock处理异常 {}", e);
                ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.mock)
                        .setErrorCode("mock-0003")
                        .setMessage("mock处理异常！" + e.getMessage())
                        .setDetail("脚本内容" + ((MatchConfig) params).getScriptContent())
                        .report();
            }
            return null;
        }
    };

}
