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

import com.pamirs.attach.plugin.rabbitmq.RabbitmqConstants;
import com.pamirs.attach.plugin.rabbitmq.common.ChannelHolder;
import com.pamirs.attach.plugin.rabbitmq.common.ConfigCache;
import com.pamirs.attach.plugin.rabbitmq.destroy.RabbitmqDestroy;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * 给spring 的 Internal
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/1/7 2:58 下午
 */
@Destroyable(RabbitmqDestroy.class)
public class DefaultConsumerHandleDeliveryInterceptor extends AroundInterceptor {
    private final static Logger LOGGER = LoggerFactory.getLogger(DefaultConsumerHandleDeliveryInterceptor.class);

    private Consumer getConsumer(Object target) {
        if (target == null) {
            return null;
        }
        if (!"org.springframework.amqp.rabbit.listener.BlockingQueueConsumer$ConsumerDecorator".equals(target.getClass().getName())) {
            return (Consumer) target;
        }
        try {
            return getConsumer(Reflect.on(target).get(RabbitmqConstants.DYNAMIC_FIELD_DELEGATE));
        } catch (ReflectException e) {
            return null;
        }
    }

    private String getQueue(Consumer target) {
        final int key = System.identityHashCode(target);
        if (ConfigCache.containsQueue(key)) {
            return ConfigCache.getQueue(key);
        }
        String queue = null;
        try {
            queue = Reflect.on(target).get(RabbitmqConstants.DYNAMIC_FIELD_QUEUE);
        } catch (ReflectException e) {
            try {
                queue = Reflect.on(target).get(RabbitmqConstants.DYNAMIC_FIELD_QUEUE_NAME);
            } catch (ReflectException re) {
            }
        }
        ConfigCache.putQueue(key, queue == null ? "" : queue);
        return queue;
    }

    @Override
    public void doBefore(Advice advice) throws Exception {
        Object[] args = advice.getParameterArray();
        if (ArrayUtils.isEmpty(args)) {
            return;
        }
        String consumerTag = (String) args[0];
        /**
         * 如果已经存在该消费者，则直接忽略即可
         */
        if (ChannelHolder.existsConsumer(consumerTag)) {
            return;
        }

        DefaultConsumer consumer = (DefaultConsumer) advice.getTarget();

        List<String> queueNames = new ArrayList<String>();
        try {
            queueNames.add((String) Reflect.on(consumer).get(RabbitmqConstants.DYNAMIC_FIELD_QUEUE_NAME));
        } catch (ReflectException e) {
            try {
                queueNames.add((String) Reflect.on(consumer).get(RabbitmqConstants.DYNAMIC_FIELD_QUEUE));
            } catch (ReflectException re) {
            }
        }
        if (queueNames.isEmpty()) {
            try {
                String[] queues = Reflect.on(Reflect.on(consumer).get("this$0")).get(RabbitmqConstants.DYNAMIC_FIELD_QUEUES);
                if (queues != null && queues.length != 0) {
                    queueNames.addAll(Arrays.asList(queues));
                }
            } catch (Throwable e) {
            }
        }

        if (queueNames.isEmpty()) {
            LOGGER.warn("RabbitMQ: {} got a null queueName with field [{},{}]. ", consumer, RabbitmqConstants.DYNAMIC_FIELD_QUEUE_NAME, RabbitmqConstants.DYNAMIC_FIELD_QUEUE);
            return;
        }

        /**
         * 如果已经处理过则不再处理
         */
        if (ConfigCache.containsCache(System.identityHashCode(consumer))) {
            return;
        }
        ConfigCache.putCache(System.identityHashCode(consumer), ChannelHolder.NULL_OBJECT);

        for (String queue : queueNames) {
            final Channel channel = consumer.getChannel();
            String ptQueueName = Pradar.addClusterTestPrefix(queue);
            boolean exists = ChannelHolder.isQueueExists(channel, ptQueueName);
            if (!exists) {
                LOGGER.warn("Try to subscribe rabbitmq queue[{}],but it is not exists. skip it", ptQueueName);
                continue;
            }

            if (PradarSwitcher.whiteListSwitchOn() && !GlobalConfig.getInstance().getMqWhiteList().contains(queue)) {
                LOGGER.warn("SIMULATOR: rabbitmq queue is not in whitelist. ignore it :{}", queue);
                return;
            }

            String ptConsumerTag = null;
            if (StringUtils.isNotBlank(consumerTag)) {
                ptConsumerTag = Pradar.addClusterTestPrefix(consumerTag);
            }

            ptConsumerTag = channel.basicConsume(ptQueueName, false, ptConsumerTag, false, false, new HashMap<String, Object>(), consumer);
            ChannelHolder.addConsumerTag(channel, consumerTag, ptConsumerTag, ptQueueName);
        }
    }
}
