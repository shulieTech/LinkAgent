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

package com.pamirs.attach.plugin.apache.kafkav2.consumer.config;

import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/2 14:34
 */
public class KafkaShadowConsumerConfig extends ConsumerConfig {

    private final String topic;

    private final String groupId;

    private final String bootstrapServers;

    private final KafkaConsumer kafkaConsumer;

    public KafkaShadowConsumerConfig(String topic, String groupId, String bootstrapServers, KafkaConsumer kafkaConsumer) {
        this.topic = topic;
        this.groupId = groupId;
        this.bootstrapServers = bootstrapServers;
        this.kafkaConsumer = kafkaConsumer;
    }

    @Override
    public String keyOfConfig() {
        return this.topic + "#" + this.groupId;
    }

    @Override
    public String keyOfServer() {
        return "";
    }

    public String getTopic() {
        return topic;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public KafkaConsumer getKafkaConsumer() {
        return kafkaConsumer;
    }
}
