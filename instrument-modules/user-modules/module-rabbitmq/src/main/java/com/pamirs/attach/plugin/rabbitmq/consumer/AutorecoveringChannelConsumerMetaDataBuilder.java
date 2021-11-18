package com.pamirs.attach.plugin.rabbitmq.consumer;

import java.util.Map;

import com.pamirs.attach.plugin.rabbitmq.common.DeliverDetail;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.impl.recovery.AutorecoveringChannel;
import com.rabbitmq.client.impl.recovery.AutorecoveringConnection;
import com.rabbitmq.client.impl.recovery.RecordedConsumer;
import com.rabbitmq.client.impl.recovery.RecoveryAwareChannelN;
import com.shulie.instrument.simulator.api.reflect.Reflect;

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
    public ConsumerMetaData tryBuild(DeliverDetail deliverDetail) {
        String consumerTag = deliverDetail.getConsumerTag();
        Consumer consumer = deliverDetail.getConsumer();
        Channel channel = unWrapChannel(deliverDetail.getChannel(), consumerTag, consumer);
        if (!(channel instanceof AutorecoveringChannel)) {
            return null;
        }
        RecordedConsumer recordedConsumer = getRecordedConsumer(channel, consumerTag);
        return new ConsumerMetaData((String)Reflect.on(recordedConsumer).get("queue"),
            consumerTag,
            consumer,
            (Boolean)Reflect.on(recordedConsumer).get("exclusive"),
            (Boolean)Reflect.on(recordedConsumer).get("autoAck"),
            (Integer)Reflect.on(channel).get("prefetchCountConsumer"));
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
