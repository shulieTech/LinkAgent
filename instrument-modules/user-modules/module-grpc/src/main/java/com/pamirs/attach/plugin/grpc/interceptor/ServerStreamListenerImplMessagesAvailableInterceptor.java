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
package com.pamirs.attach.plugin.grpc.interceptor;

import com.pamirs.attach.plugin.grpc.GrpcConstants;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author angju
 * @date 2021/6/21 16:44
 */
public class ServerStreamListenerImplMessagesAvailableInterceptor extends TraceInterceptorAdaptor {
    @Resource
    protected DynamicFieldManager manager;

    @Override
    public String getPluginName() {
        return GrpcConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return GrpcConstants.PLUGIN_TYPE;
    }

    /**
     * 是否是调用端
     *
     * @return
     */
    @Override
    public boolean isClient(Advice advice) {
        return false;
    }

    @Override
    public void beforeFirst(Advice advice) {

    }

    @Override
    public void beforeLast(Advice advice) {

    }


    @Override
    public SpanRecord beforeTrace(Advice advice) {
        final String fullMethodName = manager.getDynamicField(advice.getTarget(), GrpcConstants.DYNAMIC_FIELD_FULL_METHOD_NAME);
        final Map<String, String> invokeContext = manager.getDynamicField(advice.getTarget(), GrpcConstants.DYNAMIC_FIELD_INVOKE_CONTEXT);
        SpanRecord record = new SpanRecord();
        String rpcId = invokeContext.get(Pradar.getInvokeIdKey());
        String[] rpcIds = rpcId.split("\\.");
        String lastRpcId = Integer.valueOf(rpcIds[rpcIds.length -1]) + 1 + "";
        StringBuilder newRpcId = new StringBuilder();
        for (int i = 0; i < rpcIds.length - 1 ; i ++ ){
            newRpcId.append(rpcIds[i]).append(".");
        }
        newRpcId.append(lastRpcId);
        invokeContext.put(Pradar.getInvokeIdKey(), newRpcId.toString());
        record.setContext(invokeContext);

        String method = fullMethodName;
        String service = "";
        if (fullMethodName.indexOf("/") != -1) {
            service = fullMethodName.substring(0, fullMethodName.lastIndexOf("/"));
            method = fullMethodName.substring(fullMethodName.lastIndexOf("/") + 1);
        }
        record.setMethod(method);
        record.setService(service);
        return record;
    }

    @Override
    public void afterFirst(Advice advice) {

    }

    @Override
    public void afterLast(Advice advice) {

    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        SpanRecord record = new SpanRecord();
        record.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
        return record;
    }

    @Override
    public void exceptionFirst(Advice advice) {

    }

    @Override
    public void exceptionLast(Advice advice) {

    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        SpanRecord record = new SpanRecord();
        record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        return record;
    }
}
