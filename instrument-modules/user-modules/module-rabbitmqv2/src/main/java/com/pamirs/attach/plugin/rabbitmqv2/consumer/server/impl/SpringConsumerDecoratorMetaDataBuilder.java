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
package com.pamirs.attach.plugin.rabbitmqv2.consumer.server.impl;

import com.pamirs.attach.plugin.rabbitmqv2.consumer.model.ConsumerDetail;
import com.pamirs.attach.plugin.rabbitmqv2.consumer.model.ConsumerMetaData;
import com.pamirs.attach.plugin.rabbitmqv2.consumer.server.ConsumerMetaDataBuilder;
import com.pamirs.pradar.exception.PradarException;
import com.rabbitmq.client.Consumer;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.listener.BlockingQueueConsumer;

import java.util.Map;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/11/25 1:43 下午
 */
public class SpringConsumerDecoratorMetaDataBuilder implements ConsumerMetaDataBuilder {

    private final static SpringConsumerDecoratorMetaDataBuilder INSTANCE = new SpringConsumerDecoratorMetaDataBuilder();

    public static SpringConsumerDecoratorMetaDataBuilder getInstance() {
        return INSTANCE;
    }

    private final Logger logger = LoggerFactory.getLogger(SpringConsumerDecoratorMetaDataBuilder.class);

    private SpringConsumerDecoratorMetaDataBuilder() {}

    @Override
    public ConsumerMetaData tryBuild(ConsumerDetail consumerDetail) {
        Consumer consumer = consumerDetail.getConsumer();
        String consumerTag = consumerDetail.getConsumerTag();
        if (!consumer.getClass().getName().equals(
            "org.springframework.amqp.rabbit.listener.BlockingQueueConsumer$ConsumerDecorator")) {
            return null;
        }
        try {
            return highVersion(consumer, consumerTag);
        } catch (Throwable e) {
            throw new PradarException("spring rabbitmq 版本不支持！", e);
        }
    }

    private ConsumerMetaData highVersion(Consumer consumer, String consumerTag) {
        BlockingQueueConsumer blockingQueueConsumer = Reflect.on(Reflect.on(consumer).get("delegate")).get("this$0");
        Map<String, String> consumerTags = null;
        try{
            consumerTags = Reflect.on(blockingQueueConsumer).get("consumerTags");
        } catch (Throwable e){
            logger.warn("[RabbitMQ] BlockingQueueConsumer not find consumerTags field");
        }
        final ConsumerMetaData consumerMetaData = new ConsumerMetaData(
            Reflect.on(consumer).<String>get("queue"),
            consumerTag,
            consumer,
            Reflect.on(blockingQueueConsumer).<Boolean>get("exclusive"),
            Reflect.on(blockingQueueConsumer).<AcknowledgeMode>get("acknowledgeMode").isAutoAck(),
            Reflect.on(blockingQueueConsumer).<Integer>get("prefetchCount"),
            true, true);
        if(consumerTags != null){
            consumerTags.put(consumerMetaData.getPtConsumerTag(), consumerMetaData.getPtQueue());
        }
        return consumerMetaData;
    }

    private ConsumerMetaData lowVersion(Consumer consumer, String consumerTag) {
        BlockingQueueConsumer blockingQueueConsumer = Reflect.on(consumer).get("this$0");
        Map<String, String> consumerTags = Reflect.on(blockingQueueConsumer).get("consumerTags");
        String queue = consumerTags.get(consumerTag);
        if (queue == null) {
            throw new RuntimeException("this should never happened!");
        }
        return new ConsumerMetaData(
            queue,
            consumerTag,
            consumer,
            Reflect.on(blockingQueueConsumer).<Boolean>get("exclusive"),
            Reflect.on(blockingQueueConsumer).<AcknowledgeMode>get("acknowledgeMode").isAutoAck(),
            Reflect.on(blockingQueueConsumer).<Integer>get("prefetchCount"),
            true);
    }
}
