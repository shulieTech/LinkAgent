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
package com.pamirs.attach.plugin.apache.kafka.interceptor;

import java.lang.reflect.Field;
import java.util.Map;

import javax.annotation.Resource;

import com.pamirs.attach.plugin.apache.kafka.KafkaConstants;
import com.pamirs.attach.plugin.apache.kafka.destroy.KafkaDestroy;
import com.pamirs.attach.plugin.apache.kafka.header.HeaderProcessor;
import com.pamirs.attach.plugin.apache.kafka.header.HeaderProvider;
import com.pamirs.attach.plugin.apache.kafka.util.KafkaUtils;
import com.pamirs.attach.plugin.apache.kafka.util.ReflectUtil;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.common.BytesUtils;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

/**
 * @Author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @package: com.pamirs.attach.plugin.apache.kafka.interceptor
 * @Date 2019-08-05 19:34
 */
@Destroyable(KafkaDestroy.class)
@ListenerBehavior(isNoSilence = true)
public class ConsumerRecordEntryPointInterceptor extends TraceInterceptorAdaptor {
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

    private volatile String groupGlobal = null;

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
        ConsumerRecord consumerRecord = (ConsumerRecord)args[0];
        Object consumer = advice.getParameterArray()[2];
        if (consumer instanceof Consumer && consumer.getClass().getName().equals("brave.kafka.clients.TracingConsumer")) {
            consumer = Reflect.on(consumer).get("delegate");
        }
        long consumerCell = System.currentTimeMillis() - consumerRecord.timestamp();
        String remoteAddress = null;
        if (args.length >= 3) {
            remoteAddress = KafkaUtils.getRemoteAddress(args[2], manager);
        }
        if (remoteAddress == null) {
            Object metadata = Reflect.on(consumer).get("metadata");
            Object cluster = ReflectUtil.reflectSlience(metadata, "cluster");
            Iterable<Node> nodes = null;
            if (cluster != null) {
                nodes = Reflect.on(cluster).get("nodes");
            } else {
                Object cache = ReflectUtil.reflectSlience(metadata, "cache");
                if (cache != null) {
                    nodes = ReflectUtil.reflectSlience(cache, "nodes");
                }
            }
            StringBuilder sb = new StringBuilder();
            if (nodes != null) {
                for (Node node : nodes) {
                    sb.append(Reflect.on(node).get("host").toString()).append(":").append(Reflect.on(node).get("port")
                        .toString()).append(",");
                }
                remoteAddress = sb.toString();
            }
        }
        String group = null;
        if (groupGlobal == null) {
            if (args.length >= 3) {
                group = manager.removeField(args[2], KafkaConstants.DYNAMIC_FIELD_GROUP);
            }
            if (group == null) {
                try {
                    Field groupId = ReflectUtil.getField(consumer, "groupId");
                    group = (String)groupId.get(consumer);
                } catch (Throwable e) {
//                    Object coordinator = Reflect.on(consumer).get(KafkaConstants.REFLECT_FIELD_COORDINATOR);
//                    group = ReflectUtil.reflectSlience(coordinator, KafkaConstants.REFLECT_FIELD_GROUP_ID);
                    groupGlobal = Thread.currentThread().getName().split("-")[0];
                }
            }
        }

        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setRemoteIp(remoteAddress == null ? "127.0.0.1:9092" : remoteAddress);
        if (PradarSwitcher.isKafkaMessageHeadersEnabled()) {
            HeaderProcessor headerProcessor = HeaderProvider.getHeaderProcessor(consumerRecord);
            Map<String, String> ctx = headerProcessor.getHeaders(consumerRecord);
            spanRecord.setContext(ctx);
        }
        spanRecord.setRequest(consumerRecord);
        spanRecord.setService(consumerRecord.topic());
        spanRecord.setMethod(group == null ? groupGlobal : group);
        spanRecord.setCallbackMsg(consumerCell + "");
        return spanRecord;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        Object result = advice.getReturnObj();
        ConsumerRecord consumerRecord = (ConsumerRecord)args[0];
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setRequest(consumerRecord);
        spanRecord.setResponse(result);
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
        return spanRecord;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        Throwable throwable = advice.getThrowable();
        ConsumerRecord consumerRecord = (ConsumerRecord)args[0];
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setRequest(consumerRecord);
        spanRecord.setResponse(throwable);
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        return spanRecord;
    }

    @Override
    public void beforeLast(Advice advice) {
        Object[] args = advice.getParameterArray();
        ClusterTestUtils.validateClusterTest();
        ConsumerRecord consumerRecord = (ConsumerRecord)args[0];
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
        if (PradarService.isSilence() && isClusterTest) {
            throw new PressureMeasureError("[kafka check]silence module ! can not handle cluster test data");
        }
    }
}
