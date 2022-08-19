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

package com.pamirs.attach.plugin.alibaba.rocketmqv2.consumer;

import com.alibaba.rocketmq.client.consumer.DefaultMQPushConsumer;
import com.alibaba.rocketmq.client.consumer.listener.MessageListener;
import com.alibaba.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import com.alibaba.rocketmq.client.consumer.listener.MessageListenerOrderly;
import com.alibaba.rocketmq.common.UtilAll;
import com.alibaba.rocketmq.common.protocol.heartbeat.SubscriptionData;
import com.pamirs.attach.plugin.alibaba.rocketmqv2.consumer.config.RocketmqConsumerConfig;
import com.pamirs.attach.plugin.alibaba.rocketmqv2.consumer.server.RocketmqShadowServer;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.bean.SyncObjectData;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowConsumerExecute;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfigWithData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/7/29 11:38
 */
public class RocketmqShadowConsumerExecute implements ShadowConsumerExecute {

    private static final Logger logger = LoggerFactory.getLogger(RocketmqShadowConsumerExecute.class);

    /**
     * 重试topic前缀
     */
    private static final String RETRY_PREFIX = "%RETRY%";

    /**
     * 死信topic前缀
     */
    private static final String DLQ_PREFIX = "%DLQ%";

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

        List<ConsumerConfig> consumerList = new ArrayList<>();
        if (map != null) {
            for (String topic : map.keySet()) {
                if (topic.startsWith(RETRY_PREFIX) || topic.startsWith(DLQ_PREFIX)) {
                    continue;
                }
                RocketmqConsumerConfig consumerConfig = new RocketmqConsumerConfig(businessConsumer, topic);
                consumerList.add(consumerConfig);
            }
        }
        return consumerList;
    }

    @Override
    public ShadowServer fetchShadowServer(List<ConsumerConfigWithData> configWithDataList) {
        List<String> needRegisterTopic = configWithDataList.stream().map(item -> ((RocketmqConsumerConfig) item.getConsumerConfig()).getTopic()).collect(Collectors.toList());
        DefaultMQPushConsumer shadowConsumer = buildMQPushConsumer(((RocketmqConsumerConfig) configWithDataList.get(0).getConsumerConfig()).getBusinessConsumer(), needRegisterTopic);
        return new RocketmqShadowServer(shadowConsumer);
    }

    /**
     * 构建 DefaultMQPushConsumer
     * 如果后续支持影子 server 模式，则直接修改此方法即可
     *
     * @param businessConsumer      业务消费者
     * @param needRegisterTopicList 需要注册的topic
     * @return 返回注册的影子消费者，如果初始化失败会返回 null
     */
    private DefaultMQPushConsumer buildMQPushConsumer(DefaultMQPushConsumer businessConsumer, List<String> needRegisterTopicList) {
        ConcurrentMap<String, SubscriptionData> map;
        try {
            map = businessConsumer.getDefaultMQPushConsumerImpl().getSubscriptionInner();
        } catch (NoSuchMethodError e) {
            try {
                map = Reflect.on(businessConsumer.getDefaultMQPushConsumerImpl()).call("getSubscriptionInner").get();
            } catch (ReflectException t) {
                logger.error("buildMQPushConsumer getSubscriptionInner error.", e);
                return null;
            }
        }
        if (map == null) {
            return null;
        }

        DefaultMQPushConsumer defaultMQPushConsumer = new DefaultMQPushConsumer();
        defaultMQPushConsumer.setNamesrvAddr(businessConsumer.getNamesrvAddr());
        defaultMQPushConsumer.setConsumerGroup(Pradar.addClusterTestPrefix(businessConsumer.getConsumerGroup()));
        defaultMQPushConsumer.setConsumeFromWhere(businessConsumer.getConsumeFromWhere());
        defaultMQPushConsumer.setPullThresholdForQueue(businessConsumer.getPullThresholdForQueue());
        defaultMQPushConsumer.setPullBatchSize(businessConsumer.getPullBatchSize());
        defaultMQPushConsumer.setConsumeMessageBatchMaxSize(businessConsumer.getConsumeMessageBatchMaxSize());
        defaultMQPushConsumer.setConsumeThreadMax(businessConsumer.getConsumeThreadMax());
        defaultMQPushConsumer.setConsumeThreadMin(businessConsumer.getConsumeThreadMin());
        String instanceName = getInstanceName();
        if (!"DEFAULT".equals(instanceName)) {
            defaultMQPushConsumer.setInstanceName(Pradar.CLUSTER_TEST_PREFIX + instanceName);
        } else {
            defaultMQPushConsumer.setInstanceName(
                    Pradar.addClusterTestPrefix(businessConsumer.getConsumerGroup() + instanceName));
        }
        try {
            defaultMQPushConsumer.setAdjustThreadPoolNumsThreshold(businessConsumer.getAdjustThreadPoolNumsThreshold());
        } catch (Throwable ignored) {
        }
        try {
            defaultMQPushConsumer.setAllocateMessageQueueStrategy(businessConsumer.getAllocateMessageQueueStrategy());
        } catch (Throwable ignored) {
        }
        defaultMQPushConsumer.setConsumeConcurrentlyMaxSpan(businessConsumer.getConsumeConcurrentlyMaxSpan());
        defaultMQPushConsumer.setConsumeTimestamp(businessConsumer.getConsumeTimestamp());
        defaultMQPushConsumer.setMessageModel(businessConsumer.getMessageModel());
        defaultMQPushConsumer.setMessageListener(businessConsumer.getMessageListener());
        defaultMQPushConsumer.setPostSubscriptionWhenPull(businessConsumer.isPostSubscriptionWhenPull());
        defaultMQPushConsumer.setPullInterval(businessConsumer.getPullInterval());
        defaultMQPushConsumer.setSubscription(businessConsumer.getSubscription());
        defaultMQPushConsumer.setUnitMode(businessConsumer.isUnitMode());
        try {
            defaultMQPushConsumer.setClientCallbackExecutorThreads(businessConsumer.getClientCallbackExecutorThreads());
        } catch (Throwable ignored) {
        }
        try {
            defaultMQPushConsumer.setClientIP(businessConsumer.getClientIP());
        } catch (Throwable ignored) {
        }
        try {
            defaultMQPushConsumer.setHeartbeatBrokerInterval(businessConsumer.getHeartbeatBrokerInterval());
        } catch (Throwable ignored) {
        }
        try {
            defaultMQPushConsumer.setPersistConsumerOffsetInterval(businessConsumer.getPersistConsumerOffsetInterval());
        } catch (Throwable ignored) {
        }
        try {
            defaultMQPushConsumer.setPollNameServerInteval(businessConsumer.getPollNameServerInteval());
        } catch (Throwable ignored) {
        }
        try {
            defaultMQPushConsumer.setUnitName(businessConsumer.getUnitName());
        } catch (Throwable ignored) {
        }
        try {
            /**
             * 高版本才有，低版本无此方法，所以需要做一下此方法的兼容
             */
            defaultMQPushConsumer.setVipChannelEnabled(businessConsumer.isVipChannelEnabled());
        } catch (Throwable ignored) {
        }

        MessageListener messageListener = businessConsumer.getMessageListener();
        if (messageListener != null) {
            /**
             * 低版本只有registerMessageListener(com.alibaba.rocketmq.client.consumer.listener.MessageListener)
             */
            if (messageListener instanceof MessageListenerConcurrently) {
                try {
                    defaultMQPushConsumer.registerMessageListener((MessageListenerConcurrently) messageListener);
                } catch (NoSuchMethodError e) {
                    defaultMQPushConsumer.registerMessageListener(messageListener);
                }
            } else if (messageListener instanceof MessageListenerOrderly) {
                try {
                    defaultMQPushConsumer.registerMessageListener((MessageListenerOrderly) messageListener);
                } catch (NoSuchMethodError e) {
                    defaultMQPushConsumer.registerMessageListener(messageListener);
                }
            }
        }

        for (String topic : needRegisterTopicList) {
            SubscriptionData subscriptionData = map.get(topic);
            if (subscriptionData == null) {
                continue;
            }
            String subString = subscriptionData.getSubString();
            String filterClassSource = null;
            try {
                /**
                 * 高版本才有这个方法
                 */
                filterClassSource = subscriptionData.getFilterClassSource();
            } catch (Throwable ignored) {
            }
            if (filterClassSource != null) {
                try {
                    defaultMQPushConsumer.subscribe(Pradar.addClusterTestPrefix(topic), subString, filterClassSource);
                    logger.info(
                            "Alibaba-RocketMQ shadow consumer subscribe topic : {} subString : {} filterClassSource : {}",
                            Pradar.addClusterTestPrefix(topic), subString, filterClassSource);
                } catch (Throwable e) {
                    ErrorReporter.buildError()
                            .setErrorType(ErrorTypeEnum.MQ)
                            .setErrorCode("MQ-0001")
                            .setMessage("Alibaba-RocketMQ消费端subscribe失败！")
                            .setDetail(
                                    "topic:" + topic + " fullClassName:" + subString + " filterClassSource:" + filterClassSource
                                            + "||" + e.getMessage())
                            .report();
                    logger.error(
                            "Alibaba-RocketMQ: subscribe shadow DefaultMQPushConsumer err! topic:{} fullClassName:{} "
                                    + "filterClassSource:{}",
                            topic, subString, filterClassSource, e);
                    return null;
                }
            } else {
                try {
                    defaultMQPushConsumer.subscribe(Pradar.addClusterTestPrefix(topic), subString);
                    logger.info("Alibaba-RocketMQ shadow consumer subscribe topic : {} subString : {}",
                            Pradar.addClusterTestPrefix(topic), subString);
                } catch (Throwable e) {
                    ErrorReporter.buildError()
                            .setErrorType(ErrorTypeEnum.MQ)
                            .setErrorCode("MQ-0001")
                            .setMessage("Alibaba-RocketMQ消费端subscribe失败！")
                            .setDetail("topic:" + topic + " subExpression:" + subString + "||" + e.getMessage())
                            .report();
                    logger.error(
                            "Alibaba-RocketMQ: subscribe shadow DefaultMQPushConsumer err! topic:{} subExpression:{}",
                            topic, subString, e);
                    return null;
                }
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
