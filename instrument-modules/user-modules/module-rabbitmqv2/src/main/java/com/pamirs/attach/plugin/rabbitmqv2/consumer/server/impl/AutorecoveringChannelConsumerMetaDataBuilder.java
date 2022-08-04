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
package com.pamirs.attach.plugin.rabbitmqv2.consumer.server.impl;

import com.pamirs.attach.plugin.rabbitmqv2.consumer.model.ConsumerDetail;
import com.pamirs.attach.plugin.rabbitmqv2.consumer.model.ConsumerMetaData;
import com.pamirs.attach.plugin.rabbitmqv2.consumer.server.ConsumerMetaDataBuilder;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.impl.recovery.AutorecoveringChannel;
import com.rabbitmq.client.impl.recovery.AutorecoveringConnection;
import com.rabbitmq.client.impl.recovery.RecordedConsumer;
import com.rabbitmq.client.impl.recovery.RecoveryAwareChannelN;
import com.shulie.instrument.simulator.api.reflect.Reflect;

import java.util.Map;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/11/17 4:08 下午
 */
public class AutorecoveringChannelConsumerMetaDataBuilder implements ConsumerMetaDataBuilder {

    private final static AutorecoveringChannelConsumerMetaDataBuilder INSTANCE = new AutorecoveringChannelConsumerMetaDataBuilder();

    public static AutorecoveringChannelConsumerMetaDataBuilder getInstance() {
        return INSTANCE;
    }

    private AutorecoveringChannelConsumerMetaDataBuilder() {
    }

    @Override
    public ConsumerMetaData tryBuild(ConsumerDetail consumerDetail) {
        String consumerTag = consumerDetail.getConsumerTag();
        Consumer consumer = consumerDetail.getConsumer();
        Channel channel = unWrapChannel(consumerDetail.getChannel(), consumerTag, consumer);
        if (!(channel instanceof AutorecoveringChannel)) {
            return null;
        }
        /*
            如果一个connection有多个channel，这多个channel之间又有相同的consumer tag订阅不同的queue，那么这里就有问题。
            因为consumer_tag对于connection来说不是唯一的，对于channel才是唯一的，最早订阅的conusmer会被覆盖
            rabbitmq client的内部实现就是有bug，see : AutorecoveringConnection#recordConsumer，
            所以应该不太会出现重复consumer tag的情况，这里就先不考虑相同consumer tag在同一个connection的情况
         */
        RecordedConsumer recordedConsumer = getRecordedConsumer(channel, consumerTag);
        return new ConsumerMetaData((String) Reflect.on(recordedConsumer).get("queue"),
                consumerTag,
                consumer,
                (Boolean) Reflect.on(recordedConsumer).get("exclusive"),
                (Boolean) Reflect.on(recordedConsumer).get("autoAck"),
                (Integer) Reflect.on(channel).get("prefetchCountConsumer"), false);
    }

    private Channel unWrapChannel(Channel channel, String consumerTag, Consumer consumer) {
        if (channel instanceof RecoveryAwareChannelN) {
            if (consumer instanceof DefaultConsumer) {
                channel = Reflect.on(consumer).get("_channel");
            } else {
                Map<String, Consumer> consumers = Reflect.on(channel).get("_consumers");
                Consumer consumerFromRecovery = consumers.get(consumerTag);
                if (consumerFromRecovery.getClass().getName().contains("AutorecoveringChannel")) {
                    channel = Reflect.on(consumerFromRecovery).get("this$0");
                }
            }
        }
        return channel;
    }

    private RecordedConsumer getRecordedConsumer(Channel channel, String consumerTag) {
        AutorecoveringConnection connection = Reflect.on(channel).get("connection");
        Map<String, RecordedConsumer> consumers = Reflect.on(connection).get("consumers");
        return consumers.get(consumerTag);
    }

}
