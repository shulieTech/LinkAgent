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

package com.pamirs.attach.plugin.apache.rocketmqv2.consumer.config;

import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/7/29 15:49
 */
public class RocketmqConsumerConfig extends ConsumerConfig {
    private final DefaultMQPushConsumer businessConsumer;
    private final String topic;

    public RocketmqConsumerConfig(DefaultMQPushConsumer businessConsumer, String topic) {
        this.businessConsumer = businessConsumer;
        this.topic = topic;
    }

    @Override
    public String keyOfConfig() {
        return topic + "#" + businessConsumer.getConsumerGroup();
    }

    @Override
    public String keyOfServer() {
        return businessConsumer.getNamesrvAddr();
    }

    public DefaultMQPushConsumer getBusinessConsumer() {
        return businessConsumer;
    }

    public String getTopic() {
        return topic;
    }
}
