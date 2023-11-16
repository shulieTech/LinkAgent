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

import com.pamirs.attach.plugin.apache.kafka.KafkaConstants;
import com.pamirs.attach.plugin.apache.kafka.destroy.KafkaDestroy;
import com.pamirs.attach.plugin.apache.kafka.header.HeaderProcessor;
import com.pamirs.attach.plugin.apache.kafka.header.HeaderProvider;
import com.pamirs.attach.plugin.apache.kafka.util.KafkaUtils;
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
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @Auther: vernon
 * @Date: 2021/10/15 11:21
 * @Description:kafka-spring 139
 */


@Destroyable(KafkaDestroy.class)
@ListenerBehavior(isNoSilence = true)
public class ConsumerRecordEntryPointInterceptor2 extends TraceInterceptorAdaptor {
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
        return true;
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        ConsumerRecord consumerRecord = (ConsumerRecord) args[0];
        String group = null;
        long consumerCell = System.currentTimeMillis() - consumerRecord.timestamp();
        String remoteAddress = null;
        if (args.length >= 3) {
            remoteAddress = KafkaUtils.getRemoteAddress(args[2], manager);
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
        ConsumerRecord consumerRecord = (ConsumerRecord) args[0];
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
        ConsumerRecord consumerRecord = (ConsumerRecord) args[0];
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
        ConsumerRecord consumerRecord = (ConsumerRecord) args[0];
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
