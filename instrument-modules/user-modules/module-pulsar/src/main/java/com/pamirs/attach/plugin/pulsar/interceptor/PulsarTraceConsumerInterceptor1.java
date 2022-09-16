/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pamirs.attach.plugin.pulsar.interceptor;

import com.pamirs.attach.plugin.pulsar.common.MQType;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.ReversedTraceInterceptorAdaptor;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.ObjectUtils;
import org.apache.pulsar.client.api.Message;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/9/7 11:45
 */
public class PulsarTraceConsumerInterceptor1 extends ReversedTraceInterceptorAdaptor {

    @Override
    protected boolean isClient(Advice advice) {
        return false;
    }

    @Override
    public String getPluginName() {
        return MQType.PULSAR.toString();
    }

    @Override
    public int getPluginType() {
        return MiddlewareType.TYPE_MQ;
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        Object target = advice.getTarget();
        if (!(target instanceof org.apache.pulsar.client.impl.ConsumerBase)) {
            return null;
        }
        if (args == null || args.length != 1) {
            return null;
        }
        Object messageObj = args[0];
        if (!(messageObj instanceof Message)) {
            return null;
        }
        Message<?> message = (Message<?>) messageObj;

        String topic = message.getTopicName();
        //persistent://public/default/demo-topic
        if (topic.startsWith("persistent:")) {
            topic = topic.substring(topic.lastIndexOf("/") + 1);
        }

        SpanRecord spanRecord = new SpanRecord();
        Map<String, String> properties = message.getProperties();
        if (properties != null) {
            Map<String, String> rpcContext = new HashMap<String, String>();
            for (String key : Pradar.getInvokeContextTransformKeys()) {
                String value = ObjectUtils.toString(properties.get(key));
                if (value != null) {
                    rpcContext.put(key, value);
                }
            }
            spanRecord.setContext(rpcContext);
        }

        spanRecord.setService(topic);
        boolean isClusterTest = Pradar.isClusterTestPrefix(topic);
        // 消息的properties是否包含Pradar.PRADAR_CLUSTER_TEST_KEY
        if (properties != null) {
            String clusterTestValue = ObjectUtils.toString(properties.get(PradarService.PRADAR_CLUSTER_TEST_KEY));
            isClusterTest = isClusterTest || Boolean.TRUE.toString().equals(clusterTestValue) || "1".equals(clusterTestValue);
        }
        spanRecord.setClusterTest(isClusterTest);
        spanRecord.setRequest(message.getKey());
        spanRecord.setMethod(message.getProducerName() + ":" + topic);
        return spanRecord;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setResponse("next receive");
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
        spanRecord.setMiddlewareName(MQType.PULSAR.toString());
        return spanRecord;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        Throwable throwable = advice.getThrowable();
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setRequest("receive");
        spanRecord.setResponse(throwable);
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        return spanRecord;
    }
}
