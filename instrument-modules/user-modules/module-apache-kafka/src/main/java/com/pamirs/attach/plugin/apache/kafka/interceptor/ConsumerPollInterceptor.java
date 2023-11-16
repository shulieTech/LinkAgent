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
import com.pamirs.attach.plugin.apache.kafka.origin.ConsumerHolder;
import com.pamirs.attach.plugin.apache.kafka.origin.ConsumerMetaData;
import com.pamirs.attach.plugin.apache.kafka.util.KafkaUtils;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.*;
import com.pamirs.pradar.common.BytesUtils;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @Author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @package: com.pamirs.attach.plugin.apache.kafka.interceptor
 * @Date 2019-08-05 19:32
 */
@Destroyable(KafkaDestroy.class)
@SuppressWarnings("rawtypes")
public class ConsumerPollInterceptor extends AroundInterceptor {

    private final static Logger LOGGER = LoggerFactory.getLogger(ConsumerPollInterceptor.class.getName());

    private static long LAST_REPORT_TIME = System.currentTimeMillis();

    @Resource
    protected DynamicFieldManager manager;


    /**
     * poll 开始前 提交未结束的trace 并清空上下文
     * @param advice
     * @throws Throwable
     */
    @Override
    public void doBefore(Advice advice) throws Throwable {
        if(Pradar.getInvokeContext() != null){
            Pradar.endServerInvoke(ResultCode.INVOKE_RESULT_SUCCESS, KafkaConstants.PLUGIN_TYPE);
        }
        Pradar.clearInvokeContext();
    }


    @Override
    public void doException(Advice advice) throws Throwable {
        if(Pradar.getInvokeContext() != null){
            Pradar.endTrace(ResultCode.INVOKE_RESULT_FAILED, KafkaConstants.PLUGIN_TYPE);
        }
        Pradar.clearInvokeContext();
    }


    /**
     * 对spring-kafka 生成server端trace
     * @param advice
     * @throws Throwable
     */
    @Override
    public void doAfter(Advice advice) throws Throwable {
        if (!PradarSwitcher.isClusterTestEnabled()) {
            return;
        }
        KafkaConsumer consumer = (KafkaConsumer) advice.getTarget();
        // poll方法需要定制化适配上层调用逻辑，
        // 适配完以后需要调用 com.pamirs.attach.plugin.apache.kafka.origin.ConsumerHolder.addWorkWithOther
        // 把业务和影子consumer都加进来
        if (ConsumerHolder.isWorkWithOtherFramework(consumer)) {
            doWithOtherFrameworkIntercept(advice);
        } else {
            reportInfo(consumer);
        }
    }

    /**
     * 由于不会切 kafkaConsumer的poll方法，需要定制适配上层调用的代码，所以这里需要主动将不适配信息上报出去
     *
     * @param consumer kafkaConsumer对象
     */
    public void reportInfo(KafkaConsumer consumer) {
        ConsumerMetaData consumerMetaData = null;
        try {
            consumerMetaData = ConsumerHolder.getConsumerMetaData(consumer);
        } catch (Error e) {
            LOGGER.error("【Kafka】 getConsumerMetaData error", e);
        }
        if (consumerMetaData == null) {
            return;
        }
        List<String> needReportTopic = new ArrayList<String>();
        // 判断是否配置了白名单
        for (String topic : consumerMetaData.getTopics()) {
            if (!Pradar.isClusterTestPrefix(topic) && GlobalConfig.getInstance().getMqWhiteList().contains(topic + "#" + consumerMetaData.getGroupId())) {
                needReportTopic.add(topic);
            }
        }

        if (needReport() && !needReportTopic.isEmpty()) {
            StringBuilder reportInfo = new StringBuilder("【Apache-Kafka】 method 【poll】 Not support. ");
            reportInfo.append("【topics】: ").append(needReportTopic);
            reportInfo.append(" 【group】: ").append(consumerMetaData.getGroupId());

            ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.MQ)
                    .setErrorCode("Kafka-0001")
                    .setMessage("【Apache-Kafka】 method 【poll】 Not support")
                    .setDetail(reportInfo.toString())
                    .report();
            LOGGER.error(reportInfo.toString());
            LAST_REPORT_TIME = System.currentTimeMillis();
        }
    }

    /**
     * 5分钟上报一次
     *
     * @return true or false
     */
    private boolean needReport() {
        return System.currentTimeMillis() - LAST_REPORT_TIME > 5 * 60 * 1000
                && !GlobalConfig.getInstance().getSimulatorDynamicConfig().closeKafkaPollReport();
    }

    public void doWithOtherFrameworkIntercept(Advice advice) {
        try {
            if (advice.getReturnObj() == null) {
                return;
            }
            ConsumerRecords consumerRecords = (ConsumerRecords) advice.getReturnObj();
            if (consumerRecords.count() <= 0) {
                return;
            }
            Iterator iterator = consumerRecords.iterator();
            Object next = iterator.next();
            if (!(next instanceof ConsumerRecord)) {
                return;
            }
            KafkaConsumer consumer = (KafkaConsumer) advice.getTarget();
            ConsumerRecord record = (ConsumerRecord) next;
            String topic = record.topic();

            String group = null;
            Map<String, String> ctx = null;
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
            String remoteAddress = KafkaUtils.getRemoteAddress(consumer, manager);

            if (PradarSwitcher.isKafkaMessageHeadersEnabled()) {
                HeaderProcessor headerProcessor = HeaderProvider.getHeaderProcessor(record);
                ctx = headerProcessor.getHeaders(record);

            }
            Pradar.startServerInvoke(topic, group, null, ctx);

            InvokeContext invokeContext = readCurrentInvokeContext(advice);
            if (invokeContext == null) {
                return;
            }
            advice.setInvokeContext(invokeContext);

            if (StringUtils.isNotBlank(remoteAddress)) {
                invokeContext.setRemoteIp(remoteAddress);
            }
            invokeContext.setCallBackMsg((System.currentTimeMillis() - record.timestamp()) + "");
            invokeContext.setMiddlewareName(KafkaConstants.PLUGIN_NAME);
            if (Pradar.isResponseOn()) {
                invokeContext.setResponse(advice.getReturnObj());
            }
            //压测标
            boolean isClusterTest = Pradar.isClusterTestPrefix(topic);
            if (PradarSwitcher.isKafkaMessageHeadersEnabled()) {
                Headers headers = record.headers();
                Header header = headers.lastHeader(PradarService.PRADAR_CLUSTER_TEST_KEY);
                if (header != null) {
                    isClusterTest = isClusterTest || ClusterTestUtils.isClusterTestRequest(
                            BytesUtils.toString(header.value()));
                }
            }
            invokeContext.setClusterTest(isClusterTest);
        } catch (Throwable e) {
            LOGGER.error("", e);
            if (Pradar.isClusterTest()) {
                throw new PressureMeasureError(e);
            }
        }
    }



    private InvokeContext readCurrentInvokeContext(Advice advice) {
        if (advice.getInvokeContext() == null) {
            return Pradar.getInvokeContext();
        } else {
            return (InvokeContext) advice.getInvokeContext();
        }
    }

}
