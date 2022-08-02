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

package com.pamirs.attach.plugin.apache.rocketmqv2.consumer;

import com.pamirs.attach.plugin.apache.rocketmqv2.consumer.config.RocketmqConsumerConfig;
import com.pamirs.attach.plugin.apache.rocketmqv2.consumer.server.RocketmqShadowServer;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.bean.SyncObjectData;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowConsumerExecute;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.MessageListener;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.common.UtilAll;
import org.apache.rocketmq.common.protocol.heartbeat.SubscriptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/7/29 15:48
 */
public class RocketmqShadowConsumerExecute implements ShadowConsumerExecute {

    private static final Logger logger = LoggerFactory.getLogger(RocketmqShadowConsumerExecute.class);

    @Override
    public List<ConsumerConfig> prepareConfig(SyncObjectData syncObjectData) {
        DefaultMQPushConsumer businessConsumer = (DefaultMQPushConsumer) syncObjectData.getTarget();
        ConcurrentMap<String, SubscriptionData> map = null;
        try {
            map = businessConsumer.getDefaultMQPushConsumerImpl().getSubscriptionInner();
        } catch (NoSuchMethodError e) {
            try {
                map = Reflect.on(businessConsumer.getDefaultMQPushConsumerImpl()).call("getSubscriptionInner").get();
            } catch (ReflectException t) {
                logger.error("buildMQPushConsumer getSubscriptionInner error.", e);
            }
        }

        List<ConsumerConfig> consumerList = new ArrayList<ConsumerConfig>();
        if (map != null) {
            for (String topic : map.keySet()) {
                RocketmqConsumerConfig consumerConfig = new RocketmqConsumerConfig(businessConsumer, topic);
                consumerList.add(consumerConfig);
            }
        }

        return consumerList;
    }

    @Override
    public ShadowServer fetchShadowServer(ConsumerConfig config, String shadowConfig) {
        RocketmqConsumerConfig consumerConfig = (RocketmqConsumerConfig) config;

        DefaultMQPushConsumer shadowConsumer = buildMQPushConsumer(consumerConfig.getBusinessConsumer(), consumerConfig.getTopic());
        return new RocketmqShadowServer(shadowConsumer);
    }


    /**
     * 构建 DefaultMQPushConsumer
     * 如果后续支持影子 server 模式，则直接修改此方法即可
     *
     * @param businessConsumer 业务消费者
     * @param topic            需要注册的topic
     * @return 返回注册的影子消费者，如果初始化失败会返回 null
     */
    private DefaultMQPushConsumer buildMQPushConsumer(DefaultMQPushConsumer businessConsumer, String topic) {
        ConcurrentMap<String, SubscriptionData> map = businessConsumer.getDefaultMQPushConsumerImpl().getSubscriptionInner();
        if (map == null || !map.containsKey(topic)) {
            return null;
        }

        DefaultMQPushConsumer defaultMQPushConsumer = new DefaultMQPushConsumer();

        String instanceName = getInstanceName();
        if (instanceName != null && !"DEFAULT".equals(instanceName)) {
            defaultMQPushConsumer.setInstanceName(Pradar.CLUSTER_TEST_PREFIX + instanceName);
        } else {
            defaultMQPushConsumer.setInstanceName(
                    Pradar.addClusterTestPrefix(businessConsumer.getConsumerGroup() + instanceName));
        }

        defaultMQPushConsumer.setNamesrvAddr(businessConsumer.getNamesrvAddr());
        defaultMQPushConsumer.setConsumerGroup(Pradar.addClusterTestPrefix(businessConsumer.getConsumerGroup()));
        defaultMQPushConsumer.setConsumeFromWhere(businessConsumer.getConsumeFromWhere());
        defaultMQPushConsumer.setPullThresholdForQueue(businessConsumer.getPullThresholdForQueue());
        final List<String> missFields = new ArrayList<String>();
        try {
            defaultMQPushConsumer.setPullThresholdSizeForTopic(businessConsumer.getPullThresholdSizeForTopic());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setPullThresholdSizeForQueue(businessConsumer.getPullThresholdSizeForQueue());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setPullBatchSize(businessConsumer.getPullBatchSize());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setConsumeMessageBatchMaxSize(businessConsumer.getConsumeMessageBatchMaxSize());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setConsumeThreadMax(businessConsumer.getConsumeThreadMax());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setConsumeThreadMin(businessConsumer.getConsumeThreadMin());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setInstanceName(Pradar.addClusterTestPrefix(businessConsumer.getInstanceName()));
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setAdjustThreadPoolNumsThreshold(businessConsumer.getAdjustThreadPoolNumsThreshold());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setAllocateMessageQueueStrategy(businessConsumer.getAllocateMessageQueueStrategy());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setConsumeConcurrentlyMaxSpan(businessConsumer.getConsumeConcurrentlyMaxSpan());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setConsumeTimestamp(businessConsumer.getConsumeTimestamp());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setMessageModel(businessConsumer.getMessageModel());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setMessageListener(businessConsumer.getMessageListener());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setOffsetStore(businessConsumer.getOffsetStore());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setPullInterval(businessConsumer.getPullInterval());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setSubscription(businessConsumer.getSubscription());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setUnitMode(businessConsumer.isUnitMode());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setClientCallbackExecutorThreads(businessConsumer.getClientCallbackExecutorThreads());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setClientIP(businessConsumer.getClientIP());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setHeartbeatBrokerInterval(businessConsumer.getHeartbeatBrokerInterval());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setPersistConsumerOffsetInterval(businessConsumer.getPersistConsumerOffsetInterval());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setPostSubscriptionWhenPull(businessConsumer.isPostSubscriptionWhenPull());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setUnitName(businessConsumer.getUnitName());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setUnitMode(businessConsumer.isUnitMode());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setMaxReconsumeTimes(businessConsumer.getMaxReconsumeTimes());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setSuspendCurrentQueueTimeMillis(businessConsumer.getSuspendCurrentQueueTimeMillis());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setConsumeTimeout(businessConsumer.getConsumeTimeout());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setUseTLS(businessConsumer.isUseTLS());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setLanguage(businessConsumer.getLanguage());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setVipChannelEnabled(businessConsumer.isVipChannelEnabled());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }

        MessageListener messageListener = businessConsumer.getMessageListener();
        if (messageListener != null) {
            if (messageListener instanceof MessageListenerConcurrently) {
                defaultMQPushConsumer.registerMessageListener((MessageListenerConcurrently) messageListener);
            } else if (messageListener instanceof MessageListenerOrderly) {
                defaultMQPushConsumer.registerMessageListener((MessageListenerOrderly) messageListener);
            }
        }

        if (!missFields.isEmpty()) {
            logger.warn("[RocketMQ] miss some fields: {}", Arrays.toString(missFields.toArray()));
        }

        SubscriptionData subscriptionData = map.get(topic);
        String subString = subscriptionData.getSubString();
        String filterClassSource = subscriptionData.getFilterClassSource();
        if (filterClassSource != null) {
            try {
                defaultMQPushConsumer.subscribe(Pradar.addClusterTestPrefix(topic), subString, filterClassSource);
            } catch (Throwable e) {
                ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.MQ)
                        .setErrorCode("MQ-0001")
                        .setMessage("Apache-RocketMQ消费端subscribe失败！")
                        .setDetail(
                                "topic:" + topic + " fullClassName:" + subString + " filterClassSource:" + filterClassSource
                                        + "||" + e.getMessage())
                        .report();
                logger.error(
                        "Apache-RocketMQ: subscribe shadow DefaultMQPushConsumer err! topic:{} fullClassName:{} "
                                + "filterClassSource:{}",
                        topic, subString, filterClassSource, e);
                return null;
            }
        } else {
            try {
                defaultMQPushConsumer.subscribe(Pradar.addClusterTestPrefix(topic), subString);
            } catch (Throwable e) {
                ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.MQ)
                        .setErrorCode("MQ-0001")
                        .setMessage("Apache-RocketMQ消费端subscribe失败！")
                        .setDetail("topic:" + topic + " subExpression:" + subString + "||" + e.getMessage())
                        .report();
                logger.error(
                        "Apache-RocketMQ: subscribe shadow DefaultMQPushConsumer err! topic:{} subExpression:{}",
                        topic, subString, e);
                return null;
            }
        }

        return defaultMQPushConsumer;
    }

    private static String getInstanceName() {
        String instanceName = System.getProperty("rocketmq.client.name", "DEFAULT");
        if ("DEFAULT".equals(instanceName)) {
            instanceName = String.valueOf(UtilAll.getPid());
        }
        return instanceName;
    }
}
