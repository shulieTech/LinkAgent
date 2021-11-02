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
package com.pamirs.attach.plugin.rabbitmq.interceptor;

import java.util.Map;

import javax.annotation.Resource;

import com.pamirs.attach.plugin.rabbitmq.RabbitmqConstants;
import com.pamirs.attach.plugin.rabbitmq.common.ChannelHolder;
import com.pamirs.attach.plugin.rabbitmq.common.ConsumeResult;
import com.pamirs.attach.plugin.rabbitmq.common.ExceptionSilenceConsumer;
import com.pamirs.attach.plugin.rabbitmq.destroy.RabbitmqDestroy;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.Throwables;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.impl.ChannelN;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: mubai<chengjiacai @ shulie.io>
 * @Date: 2020-03-09 17:44
 * @Description:
 */
@Destroyable(RabbitmqDestroy.class)
public class ChannelNBasicConsumeInterceptor extends AroundInterceptor {
    private static Logger logger = LoggerFactory.getLogger(ChannelNBasicConsumeInterceptor.class.getName());

    @Resource
    private DynamicFieldManager dynamicFieldManager;

    @Override
    public void doAfter(Advice advice) {
        ChannelN channel = (ChannelN) advice.getTarget();
        Object[] args = advice.getParameterArray();
        String actualConsumerTag = (String) advice.getReturnObj();
        String consumerTag = String.valueOf(args[2]);
        if(StringUtils.isBlank(consumerTag)){
            consumerTag = actualConsumerTag;
        }
        String queue = String.valueOf(args[0]);
        /**
         * 过滤掉影子消费者
         */
        if(logger.isDebugEnabled()) {
            logger.debug("RabbitMQ basicConsume---queue:{},consumerTag:{},actualConsumerTag:{},\nstack:{}", queue,
                consumerTag, actualConsumerTag,
                Throwables.getStackTraceAsString(new RuntimeException()));
        }
        if (Pradar.isClusterTestPrefix(queue)
                || ChannelHolder.existsConsumer(actualConsumerTag) || Pradar.isClusterTestPrefix(actualConsumerTag)) {
            return;
        }
        try {
            String ptQueue = Pradar.addClusterTestPrefix(queue);
            boolean autoAck = (Boolean) args[1];

            if (StringUtils.isNotBlank(consumerTag)) {
                consumerTag = Pradar.addClusterTestPrefix(consumerTag);
            }

            boolean noLocal = (Boolean) args[3];
            boolean exclusive = (Boolean) args[4];
            Map<String, Object> arguments = null;
            if (args[5] != null) {
                arguments = (Map<String, Object>) args[5];
            }
            Consumer ptConsumer = (Consumer) args[6];

            boolean exists = ChannelHolder.isQueueExists(channel, ptQueue);
            if (!exists) {
                logger.warn("Try to subscribe rabbitmq queue[{}],but it is not exists. skip it", ptQueue);
                return;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("RabbitMQ basicConsume(ptQueue:{},autoAck:{},consumerTag:{},noLocal:{},exclusive:{},arguments:{},ptConsumer:{})", ptQueue, autoAck, consumerTag, noLocal, exclusive, arguments, ptConsumer);
            }
            ConsumeResult consumeResult = ChannelHolder.consumeShadowQueue(channel, ptQueue, autoAck, consumerTag, noLocal, exclusive, arguments, new ExceptionSilenceConsumer(ptConsumer));
            String cTag = consumeResult.getTag();
            dynamicFieldManager.setDynamicField(consumeResult.getShadowChannel(), RabbitmqConstants.IS_AUTO_ACK_FIELD, autoAck);
            if (cTag != null) {
                ChannelHolder.addConsumerTag(channel, ObjectUtils.toString(advice.getReturnObj()), cTag, ptQueue);
                if(logger.isDebugEnabled()) {
                    logger.debug("RabbitMQ basicConsume-consumerTag:{},--actualConsumerTag:{},ptQueue:{},cTag:{}", consumerTag,
                        ObjectUtils.toString(advice.getReturnObj()), ptQueue, cTag);
                }
            } else {
                ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.MQ)
                        .setErrorCode("MQ-0001")
                        .setMessage("RabbitMQ消费端订阅队列失败！")
                        .setDetail("RabbitMqPushConsumerInterceptor:queue:[" + queue + "], get shadow channel is null or closed.")
                        .report();
                logger.error("RabbitMQ PT Consumer Inject failed, cause by get shadow channel is null or closed. queue:[{}] consumerTag:{}", queue, consumerTag);
                return;
            }
        } catch (Throwable e) {
            ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.MQ)
                    .setErrorCode("MQ-0001")
                    .setMessage("RabbitMQ消费端订阅队列失败！")
                    .setDetail("RabbitMqPushConsumerInterceptor:queue:[" + queue + "]," + e.getMessage())
                    .report();
            logger.error("RabbitMQ PT Consumer Inject failed queue:[{}] consumerTag:{}", queue, consumerTag, e);
        }
    }

    @Override
    public void doException(Advice advice) {
        logger.error("rabbitmq execute {} occur error: {}", advice.getBehaviorName(), advice.getParameterArray(), advice.getThrowable());
        if (!PradarSwitcher.isClusterTestEnabled()) {
            logger.warn("PradarSwitcher isClusterTestEnabled false, {} to start shadow {} skip it", advice.getBehaviorName(), advice.getBehaviorName());
            return;
        }

        ChannelN channel = (ChannelN) advice.getTarget();
        Object[] args = advice.getParameterArray();
        String consumerTag = String.valueOf(args[2]);
        String queue = String.valueOf(args[0]);
        if (!StringUtils.startsWith(queue, Pradar.CLUSTER_TEST_PREFIX)) {
            if (PradarSwitcher.whiteListSwitchOn() && !GlobalConfig.getInstance().getMqWhiteList().contains(queue)) {
                logger.error("{} is not in white list, not allow to invoke consumer...", queue);
                return;
            }

            try {
                String ptQueue = Pradar.addClusterTestPrefix(queue);
                boolean autoAck = (Boolean) args[1];
                if (StringUtils.isNotBlank(consumerTag)) {
                    consumerTag = Pradar.addClusterTestPrefix(consumerTag);
                }
                boolean noLocal = (Boolean) args[3];
                boolean exclusive = (Boolean) args[4];
                Map<String, Object> arguments = null;
                if (args[5] != null) {
                    arguments = (Map<String, Object>) args[5];
                }
                Consumer ptConsumer = (Consumer) args[6];

                boolean exists = ChannelHolder.isQueueExists(channel, ptQueue);
                if (!exists) {
                    logger.warn("Try to subscribe rabbitmq queue[{}],but it is not exists. skip it", ptQueue);
                    return;
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("RabbitMQ basicConsume(ptQueue:{},autoAck:{},consumerTag:{},noLocal:{},exclusive:{},arguments:{},ptConsumer:{})", ptQueue, autoAck, consumerTag, noLocal, exclusive, arguments, ptConsumer);
                }

                ConsumeResult consumeResult = ChannelHolder.consumeShadowQueue(channel, ptQueue, autoAck, consumerTag, noLocal, exclusive, arguments, ptConsumer);
                String cTag = consumeResult.getTag();
                dynamicFieldManager.setDynamicField(consumeResult.getShadowChannel(), RabbitmqConstants.IS_AUTO_ACK_FIELD, autoAck);
                if (cTag != null) {
                    ChannelHolder.addConsumerTag(channel, ObjectUtils.toString(advice.getReturnObj()), cTag, ptQueue);
                } else {
                    ErrorReporter.buildError()
                            .setErrorType(ErrorTypeEnum.MQ)
                            .setErrorCode("MQ-0001")
                            .setMessage("RabbitMQ消费端订阅队列失败！")
                            .setDetail("RabbitMqPushConsumerInterceptor:queue:[" + queue + "], get shadow channel is null or closed.")
                            .report();
                    logger.error("RabbitMQ PT Consumer Inject failed, cause by get shadow channel is null or closed. queue:[{}] consumerTag:{}", queue, consumerTag);
                    return;
                }
            } catch (Throwable e) {
                ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.MQ)
                        .setErrorCode("MQ-0001")
                        .setMessage("RabbitMQ消费端订阅队列失败！")
                        .setDetail("RabbitMqPushConsumerInterceptor:queue:[" + queue + "]," + e.getMessage())
                        .report();
                logger.error("RabbitMQ PT Consumer Inject failed queue:[{}] consumerTag:{}", queue, consumerTag, e);
            }
        }

    }
}
