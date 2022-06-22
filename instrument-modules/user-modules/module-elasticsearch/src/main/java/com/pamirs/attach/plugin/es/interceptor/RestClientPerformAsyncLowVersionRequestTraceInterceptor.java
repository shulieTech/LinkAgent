/*
 * *
 *  * Copyright 2021 Shulie Technology, Co.Ltd
 *  * Email: shulie@shulie.io
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.pamirs.attach.plugin.es.interceptor;

import com.pamirs.attach.plugin.es.common.RestClientHighLowFlag;
import com.pamirs.attach.plugin.es.utils.AsyncLowTraceUtils;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.http.HttpHost;
import org.apache.http.client.AuthCache;
import org.apache.http.client.methods.HttpRequestBase;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * @author angju
 * @date 2022/4/12 09:33
 */
public class RestClientPerformAsyncLowVersionRequestTraceInterceptor extends TraceInterceptorAdaptor {

    @Override
    public String getPluginName() {
        return "es";
    }

    @Override
    public int getPluginType() {
        return MiddlewareType.TYPE_SEARCH;
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        //高版本的trace已经有了
        if (RestClientHighLowFlag.isHigh){
            return null;
        }
        SpanRecord spanRecord = new SpanRecord();
        Object hostTuple = advice.getParameterArray()[1];
        HttpRequestBase requestBase = (HttpRequestBase) advice.getParameterArray()[2];
        spanRecord.setService(getService(hostTuple, advice.getTarget().hashCode()));
        spanRecord.setMethod(requestBase.getURI().getPath());
        return spanRecord;
    }

    private String getService(Object hostTuple, int hashCode){
        if (AsyncLowTraceUtils.map.get(hashCode) != null){
            return AsyncLowTraceUtils.map.get(hashCode);
        }
        synchronized (AsyncLowTraceUtils.map){
            if (AsyncLowTraceUtils.map.get(hashCode) != null){
                return AsyncLowTraceUtils.map.get(hashCode);
            }
            StringBuilder stringBuilder = new StringBuilder();
            Field authCacheField = null;
            boolean authCacheFieldAccessible = false;
            Field mapField = null;
            boolean mapFieldAccessible = false;
            try {
                authCacheField = hostTuple.getClass().getDeclaredField("authCache");
                authCacheFieldAccessible = authCacheField.isAccessible();
                authCacheField.setAccessible(true);
                AuthCache authCache = (AuthCache) authCacheField.get(hostTuple);
                mapField = authCache.getClass().getDeclaredField("map");
                mapFieldAccessible = mapField.isAccessible();
                mapField.setAccessible(true);
                Map<HttpHost, byte[]> map = (Map<HttpHost, byte[]>) mapField.get(authCache);
                for (HttpHost httpHost : map.keySet()){
                    stringBuilder.append(httpHost.toString()).append(";");
                }
            }catch (Throwable t){
                LOGGER.error("RestClientPerformAsyncLowVersionRequestTraceInterceptor getService error {}", t);
            } finally {
                if (authCacheField != null){
                    authCacheField.setAccessible(authCacheFieldAccessible);
                }
                if (mapField != null){
                    mapField.setAccessible(mapFieldAccessible);
                }
            }
            AsyncLowTraceUtils.map.put(hashCode, stringBuilder.toString());
            return stringBuilder.toString();
        }
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        if (RestClientHighLowFlag.isHigh){
            return null;
        }
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
        return spanRecord;
    }


    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        if (RestClientHighLowFlag.isHigh){
            return null;
        }
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        spanRecord.setResponse(advice.getThrowable());
        return spanRecord;
    }
}
