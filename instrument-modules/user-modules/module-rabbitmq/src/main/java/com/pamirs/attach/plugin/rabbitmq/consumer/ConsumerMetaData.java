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
package com.pamirs.attach.plugin.rabbitmq.consumer;

import java.util.Collections;
import java.util.Map;

import com.pamirs.pradar.Pradar;
import com.rabbitmq.client.Consumer;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/05/19 5:54 下午
 */
public class ConsumerMetaData {

    private final String queue;
    private final String ptQueue;
    private final String consumerTag;
    private final String ptConsumerTag;
    private final Consumer consumer;
    private final boolean exclusive;
    private final boolean autoAck;
    private final int prefetchCount;
    private final Map<String, Object> arguments;
    private final boolean useOriginChannel;

    public ConsumerMetaData(String queue, String consumerTag, Consumer consumer, boolean exclusive, boolean autoAck,
        int prefetchCount, boolean useOriginChannel) {
        this.queue = queue;
        this.consumerTag = consumerTag;
        this.consumer = consumer;
        this.exclusive = exclusive;
        this.autoAck = autoAck;
        this.prefetchCount = prefetchCount;
        this.useOriginChannel = useOriginChannel;
        this.arguments = Collections.emptyMap();
        this.ptQueue = Pradar.addClusterTestPrefix(queue);
        this.ptConsumerTag = Pradar.addClusterTestPrefix(consumerTag);
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

    public int getPrefetchCount() {
        return prefetchCount;
    }

    public String getPtQueue() {
        return ptQueue;
    }

    public String getPtConsumerTag() {
        return ptConsumerTag;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public boolean isUseOriginChannel() {
        return useOriginChannel;
    }
}
