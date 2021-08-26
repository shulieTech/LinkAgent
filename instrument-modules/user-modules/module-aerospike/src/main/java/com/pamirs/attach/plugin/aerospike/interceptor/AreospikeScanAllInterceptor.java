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
package com.pamirs.attach.plugin.aerospike.interceptor;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.async.AsyncClient;
import com.aerospike.client.async.AsyncCluster;
import com.aerospike.client.cluster.Cluster;
import com.aerospike.client.cluster.Node;
import com.pamirs.attach.plugin.aerospike.AerospikeConstants;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.apache.commons.lang.StringUtils;

/**
 * @author xiaobin.zfb
 * @since 2020/8/14 11:16 下午
 */
public class AreospikeScanAllInterceptor extends TraceInterceptorAdaptor {

    @Override
    public String getPluginName() {
        return AerospikeConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return AerospikeConstants.PLUGIN_TYPE;
    }

    private String getMethod(Object[] args) {
        return (String) args[2];
    }

    private String getService(String method, Object[] args) {
        String namespace = (String) args[1];
        return StringUtils.isBlank(namespace) ? method : namespace + ':' + method;
    }

    private String getRemoteIp(Object target) {
        if (target instanceof AerospikeClient) {
            Cluster cluster = Reflect.on(target).get("cluster");
            Node[] nodes = cluster.getNodes();
            if (nodes == null || nodes.length == 0) {
                return "";
            }
            StringBuilder builder = new StringBuilder();
            for (Node node : nodes) {
                builder.append(node.getHost().name).append(':').append(node.getHost().port).append(',');
            }
            if (builder.length() > 0) {
                builder.deleteCharAt(builder.length() - 1);
            }
            return builder.toString();
        }

        if (target instanceof AsyncClient) {
            AsyncCluster cluster = Reflect.on(target).get("cluster");
            Node[] nodes = cluster.getNodes();
            if (nodes == null || nodes.length == 0) {
                return "";
            }
            StringBuilder builder = new StringBuilder();
            for (Node node : nodes) {
                builder.append(node.getHost().name).append(':').append(node.getHost().port).append(',');
            }
            if (builder.length() > 0) {
                builder.deleteCharAt(builder.length() - 1);
            }
            return builder.toString();
        }
        return null;
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setService(getService(advice.getBehavior().getName(), advice.getParameterArray()));
        spanRecord.setMethod(getMethod(advice.getParameterArray()));
        spanRecord.setRemoteIp(getRemoteIp(advice.getTarget()));
        spanRecord.setRequest(advice.getParameterArray());
        return spanRecord;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setRequest(advice.getParameterArray());
        spanRecord.setResponse(advice.getReturnObj());
        return spanRecord;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setRequest(advice.getParameterArray());
        spanRecord.setResponse(advice.getThrowable());
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        return spanRecord;
    }
}
