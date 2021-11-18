package com.pamirs.attach.plugin.rabbitmq.common;

import java.util.Map;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.shulie.instrument.simulator.api.reflect.Reflect;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/11/17 4:13 下午
 */
public class DeliverDetail {

    private final String consumerTag;

    private final String exchange;

    private final String routingKey;

    private final Channel channel;

    private final Consumer consumer;

    public DeliverDetail(String consumerTag, String exchange, String routingKey, Channel channel) {
        this.consumerTag = consumerTag;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.channel = channel;
        Map<String, Consumer> _consumers = Reflect.on(channel).get("_consumers");
        this.consumer = _consumers.get(consumerTag);
    }

    public String getConsumerTag() {
        return consumerTag;
    }

    public String getExchange() {
        return exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public Channel getChannel() {
        return channel;
    }

    public Consumer getConsumer() {
        return consumer;
    }
}
