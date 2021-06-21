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
package com.pamirs.attach.plugin.hbase.interceptor;


import com.pamirs.attach.plugin.hbase.HbaseConstants;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.hbase.async.*;

/**
 * @Auther: vernon
 * @Date: 2020/7/26 12:53
 * @Description:trace
 */
public class AsyncHbaseMethodInterceptor extends TraceInterceptorAdaptor {

    private boolean sended;

    @Override
    public String getPluginName() {
        return HbaseConstants.PLUGIN_NAME
                ;
    }

    @Override
    public int getPluginType() {
        return HbaseConstants.PLUGIN_TYPE;
    }

    @Override
    public void beforeFirst(Advice advice) {
        ClusterTestUtils.validateClusterTest();
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        HBaseRpc request = (HBaseRpc) advice.getParameterArray()[0];


        SpanRecord spanRecord = new SpanRecord();
        getRequest(request, spanRecord);
        spanRecord.setRequest(request.toString());

        return spanRecord;

    }

    private void getRequest(HBaseRpc request, SpanRecord record) {
        if (request instanceof GetRequest) {
            record.setMethod("get");
            GetRequest getRequest = (GetRequest) request;
            record.setService(new String(getRequest.table()));
        } else if (request instanceof AppendRequest) {
            record.setMethod("append");
            AppendRequest appendRequest = (AppendRequest) request;
            record.setService(new String(appendRequest.table()));
        } else if (request instanceof DeleteRequest) {
            record.setMethod("delete");
            DeleteRequest deleteRequest = (DeleteRequest) request;
            record.setService(new String(deleteRequest.table()));
        } else if (request instanceof PutRequest) {
            PutRequest putRequest = (PutRequest) request;
            record.setMethod("put");
            record.setService(new String(putRequest.table()));
        }

    }

    @Override
    public SpanRecord afterTrace(Advice advice) {

        HBaseRpc request = (HBaseRpc) advice.getParameterArray()[0];
        SpanRecord record = new SpanRecord();
        getRequest(request, record);
        record.setRequest(request.toString());
        if (advice.getReturnObj() != null){
            record.setResponse(advice.getReturnObj().toString());
        }
        record.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
        return new SpanRecord();
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        HBaseRpc request = (HBaseRpc) advice.getParameterArray()[0];
        SpanRecord record = new SpanRecord();
        getRequest(request, record);
        record.setRequest(request.toString());
        record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        record.setResponse(advice.getThrowable());
        return record;
    }

}
