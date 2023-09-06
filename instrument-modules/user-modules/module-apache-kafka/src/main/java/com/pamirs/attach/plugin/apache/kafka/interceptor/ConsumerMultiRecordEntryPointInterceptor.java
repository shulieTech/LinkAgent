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
import com.pamirs.attach.plugin.apache.kafka.header.HeaderProcessor;
import com.pamirs.attach.plugin.apache.kafka.header.HeaderProvider;
import com.pamirs.attach.plugin.apache.kafka.util.KafkaUtils;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.common.BytesUtils;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @Author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @package: com.pamirs.attach.plugin.apache.kafka.interceptor
 * @Date 2019-08-05 19:35
 */
@Destroyable(KafkaDestroy.class)
public class ConsumerMultiRecordEntryPointInterceptor extends TraceInterceptorAdaptor {
    @Resource
    protected DynamicFieldManager manager;

    @Override
    public String getPluginName() {
        return KafkaConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return KafkaConstants.PLUGIN_TYPE;
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
    public SpanRecord beforeTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args == null || args.length == 0) {
            return null;
        }
        if (!(args[0] instanceof List)) {
            return null;
        }
        List<ConsumerRecord> list = (List<ConsumerRecord>) args[0];
        if (list.isEmpty()) {
            return null;
        }
        Object consumer = advice.getParameterArray()[2];
        if (consumer instanceof Consumer && consumer.getClass().getName().equals("brave.kafka.clients.TracingConsumer")) {
            consumer = ReflectionUtils.get(consumer, "delegate");
        }
        ConsumerRecord consumerRecord = list.get(0);
        String group = null;
        String remoteAddress = null;
        if (args.length >= 3) {
            if (ReflectionUtils.existsField(consumer, "groupId")) {
                Object groupIdValue = ReflectionUtils.get(consumer, "groupId");
                if (groupIdValue.getClass().getName().equals("java.util.Optional")) {
                    group = ReflectionUtils.get(groupIdValue, "value");
                } else {
                    group = (String) groupIdValue;
                }
            } else if (ReflectionUtils.existsField(consumer, KafkaConstants.DYNAMIC_FIELD_GROUP)) {
                group = ReflectionUtils.get(consumer, KafkaConstants.DYNAMIC_FIELD_GROUP);
            }
            remoteAddress = KafkaUtils.getRemoteAddress(consumer, manager);
        }
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setRemoteIp(remoteAddress);
        if (PradarSwitcher.isKafkaMessageHeadersEnabled()) {
            HeaderProcessor headerProcessor = HeaderProvider.getHeaderProcessor(consumerRecord);
            Map<String, String> ctx = headerProcessor.getHeaders(consumerRecord);
            spanRecord.setContext(ctx);
        }
        spanRecord.setRequest(consumerRecord);

        spanRecord.setService(consumerRecord.topic());
        spanRecord.setMethod(group == null ? "" : group);
        spanRecord.setRemoteIp(remoteAddress);
        spanRecord.setCallbackMsg((System.currentTimeMillis() - consumerRecord.timestamp()) + "");
        return spanRecord;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args == null || args.length == 0) {
            return null;
        }
        if (!(args[0] instanceof List)) {
            return null;
        }
        List<ConsumerRecord> list = (List<ConsumerRecord>) args[0];
        if (list.isEmpty()) {
            return null;
        }
        ConsumerRecord consumerRecord = list.get(0);

        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setRequest(consumerRecord);
        spanRecord.setResponse(advice.getReturnObj());
        return spanRecord;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args == null || args.length == 0) {
            return null;
        }
        if (!(args[0] instanceof List)) {
            return null;
        }
        List<ConsumerRecord> list = (List<ConsumerRecord>) args[0];
        if (list.isEmpty()) {
            return null;
        }
        ConsumerRecord consumerRecord = list.get(0);
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setRequest(consumerRecord);
        spanRecord.setResponse(advice.getThrowable());
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        return spanRecord;
    }

    @Override
    public void beforeLast(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args == null || args.length == 0) {
            return;
        }
        if (!(args[0] instanceof List)) {
            return;
        }
        try {
            List<ConsumerRecord> consumerRecordList = (List<ConsumerRecord>) args[0];
            if (consumerRecordList.isEmpty()) {
                return;
            }
            ConsumerRecord consumerRecord = consumerRecordList.get(0);
            String topic = consumerRecord.topic();
            boolean isClusterTest = Pradar.isClusterTestPrefix(topic);
            if (PradarSwitcher.isKafkaMessageHeadersEnabled()) {
                Headers headers = consumerRecord.headers();
                Header header = headers.lastHeader(PradarService.PRADAR_CLUSTER_TEST_KEY);
                if (header != null) {
                    isClusterTest = isClusterTest || ClusterTestUtils.isClusterTestRequest(BytesUtils.toString(header.value()));
                }
            }
            if (isClusterTest) {
                Pradar.setClusterTest(true);
            }
        } catch (Throwable e) {
        }
    }

}
