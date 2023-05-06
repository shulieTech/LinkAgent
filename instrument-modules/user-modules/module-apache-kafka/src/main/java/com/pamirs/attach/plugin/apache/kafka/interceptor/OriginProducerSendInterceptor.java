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
package com.pamirs.attach.plugin.apache.kafka.interceptor;

import com.pamirs.attach.plugin.apache.kafka.KafkaConstants;
import com.pamirs.attach.plugin.apache.kafka.destroy.KafkaDestroy;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import java.util.List;

/**
 * send方法增强类
 *
 * @Author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @package: com.pamirs.attach.plugin.apache.kafka.interceptor
 * @Date 2019-08-05 19:25
 */
@Destroyable(KafkaDestroy.class)
@ListenerBehavior(isFilterBusinessData = true)
public class OriginProducerSendInterceptor extends TraceInterceptorAdaptor {

    @Override
    public String getPluginName() {
        return "origin-kafka";
    }

    @Override
    public int getPluginType() {
        return KafkaConstants.PLUGIN_TYPE;
    }

    private String getRemoteAddress(Object remoteAddressFieldAccessor) {
        try {
            Object obj = ReflectionUtils.get(remoteAddressFieldAccessor,"underlying");
            ProducerConfig producerConfig = ReflectionUtils.get(obj,"config");
            String value = ReflectionUtils.get(producerConfig,"brokerList");
            if (value == null) {
                return KafkaConstants.UNKNOWN;
            }
            return value;
        } catch (Throwable e) {
            return KafkaConstants.UNKNOWN;
        }
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        Object target = advice.getTarget();
        String remoteAddress = getRemoteAddress(target);
        final KeyedMessage keyedMessage = getKeyedMessage(advice);
        SpanRecord spanRecord = new SpanRecord();
        if (keyedMessage != null) {
            spanRecord.setService(keyedMessage.topic());
        }
        spanRecord.setMethod("MQSend");
        spanRecord.setRemoteIp(remoteAddress);
        return spanRecord;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        Object result = advice.getReturnObj();
        final KeyedMessage keyedMessage = getKeyedMessage(advice);
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setRequest(keyedMessage);
        spanRecord.setResponse(result);
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
        return spanRecord;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        Throwable throwable = advice.getThrowable();
        final KeyedMessage keyedMessage = getKeyedMessage(advice);
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setRequest(keyedMessage);
        spanRecord.setResponse(throwable);
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        return spanRecord;
    }

    private KeyedMessage getKeyedMessage(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args.length < 1) {
            return null;
        }
        KeyedMessage keyedMessage = null;
        if (args[0] instanceof KeyedMessage) {
            keyedMessage = (KeyedMessage) args[0];
        }

        if (args[0] instanceof List && !((List<?>) args[0]).isEmpty()) {
            keyedMessage = (KeyedMessage) ((List<?>) args[0]).get(0);
        }
        return keyedMessage;
    }
}
