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

import com.pamirs.attach.plugin.apache.kafka.destroy.KafkaDestroy;
import com.pamirs.attach.plugin.apache.kafka.origin.ConsumerHolder;
import com.pamirs.attach.plugin.apache.kafka.origin.ConsumerMetaData;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.common.BytesUtils;
import com.pamirs.pradar.exception.PradarException;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

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

        if (needReport()) {
            StringBuilder reportInfo = new StringBuilder("【Apache-Kafka】 method 【poll】 Not support. ");
            if (consumerMetaData != null) {
                reportInfo.append("【topics】: ").append(consumerMetaData.getTopics());
                reportInfo.append(" 【group】: ").append(consumerMetaData.getGroupId());
            } else {
                reportInfo.append("【topics】: UNKNOWN");
                reportInfo.append(" 【group】: UNKNOWN");
            }

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
            if (!PradarSwitcher.isClusterTestEnabled()) {
                return;
            }
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
            ConsumerRecord record = (ConsumerRecord) next;
            String topic = record.topic();
            Pradar.setClusterTest(false);
            boolean isClusterTest = Pradar.isClusterTestPrefix(topic);
            if (PradarSwitcher.isKafkaMessageHeadersEnabled()) {
                Headers headers = record.headers();
                Header header = headers.lastHeader(PradarService.PRADAR_CLUSTER_TEST_KEY);
                if (header != null) {
                    isClusterTest = isClusterTest || ClusterTestUtils.isClusterTestRequest(
                            BytesUtils.toString(header.value()));
                }
            }
            if (isClusterTest) {
                Pradar.setClusterTest(true);
            }
        } catch (PradarException e) {
            LOGGER.error("", e);
            if (Pradar.isClusterTest()) {
                throw e;
            }
        } catch (PressureMeasureError e) {
            LOGGER.error("", e);
            if (Pradar.isClusterTest()) {
                throw e;
            }
        } catch (Throwable e) {
            LOGGER.error("", e);
            if (Pradar.isClusterTest()) {
                throw new PressureMeasureError(e);
            }
        }
    }

}
