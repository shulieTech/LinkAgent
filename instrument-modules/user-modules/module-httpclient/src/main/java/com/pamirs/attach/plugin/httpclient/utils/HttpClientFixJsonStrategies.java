package com.pamirs.attach.plugin.httpclient.utils;

import com.alibaba.fastjson.JSONObject;
import com.pamirs.pradar.internal.adapter.ExecutionStrategy;
import com.pamirs.pradar.internal.config.MatchConfig;
import com.pamirs.pradar.pressurement.mock.JsonMockStrategy;
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

public class HttpClientFixJsonStrategies {

    public static final ExecutionStrategy HTTPCLIENT4_FIX_JSON_STRATEGY = new JsonMockStrategy() {
        @Override
        public Object processBlock(Class returnType, ClassLoader classLoader, Object params) throws ProcessControlException {

            MatchConfig config = (MatchConfig) params;
            FutureCallback<HttpResponse> futureCallback = (FutureCallback<HttpResponse>) config.getArgs().get("futureCallback");
            StatusLine statusline = new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "");
            try {
                HttpEntity entity = null;
                entity = new StringEntity(config.getScriptContent());

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
            }
            return null;
        }
    };

    public static final ExecutionStrategy HTTPCLIENT5_FIX_JSON_STRATEGY = new JsonMockStrategy() {
        @Override
        public Object processBlock(Class returnType, ClassLoader classLoader, Object params) throws ProcessControlException {
            MatchConfig config = (MatchConfig) params;
            org.apache.hc.core5.concurrent.FutureCallback<SimpleHttpResponse> futureCallback = (org.apache.hc.core5.concurrent.FutureCallback<SimpleHttpResponse>) config.getArgs().get("futureCallback");
            try {
                SimpleHttpResponse response = SimpleHttpResponse.create(200, config.getScriptContent());
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

}
