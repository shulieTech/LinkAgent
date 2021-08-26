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
package com.pamirs.attach.plugin.rabbitmq.common;

import java.util.Collections;
import java.util.Map;

import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.impl.recovery.RecordedConsumer;
import com.shulie.instrument.simulator.api.reflect.Reflect;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/05/19 5:54 下午
 */
public class ConsumerMetaData {

    private final String queue;
    private final String consumerTag;
    private final Consumer consumer;
    private final boolean exclusive;
    private final boolean autoAck;
    private final Map<String, Object> arguments;

    public ConsumerMetaData(RecordedConsumer recordedConsumer, Consumer consumer) {
        this.queue = recordedConsumer.getQueue();
        this.consumerTag = recordedConsumer.getConsumerTag();
        this.consumer = consumer;
        this.exclusive = Reflect.on(recordedConsumer).get("exclusive");
        this.autoAck = Reflect.on(recordedConsumer).get("autoAck");
        this.arguments = Reflect.on(recordedConsumer).get("arguments");
    }

    public ConsumerMetaData(String queue, String consumerTag, Consumer consumer) {
        this.queue = queue;
        this.consumerTag = consumerTag;
        this.consumer = consumer;
        this.exclusive = false;
        this.autoAck = false;
        this.arguments = Collections.emptyMap();
    }

    public String getQueue() {
        return queue;
    }

    public String getConsumerTag() {
        return consumerTag;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public boolean isAutoAck() {
        return autoAck;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }
}
