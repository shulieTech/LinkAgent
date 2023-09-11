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

package com.pamirs.attach.plugin.apache.kafka.streamv2.consumer.config;

import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;

import java.util.Properties;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/1 11:37
 */
public class KafkaShadowStreamConfig extends ConsumerConfig {

    private Properties properties;
    private String applicationId;
    private String topic;
    private Object topologyBuilder;

    public KafkaShadowStreamConfig(Properties properties, String applicationId, String topic, Object topologyBuilder) {
        this.properties = properties;
        this.applicationId = applicationId;
        this.topic = topic;
        this.topologyBuilder = topologyBuilder;
    }

    @Override
    public String keyOfConfig() {
        return topic + "#" + applicationId;
    }

    @Override
    public String keyOfServer() {
        return String.valueOf(System.identityHashCode(topologyBuilder));
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Object getTopologyBuilder() {
        return topologyBuilder;
    }

    public void setTopologyBuilder(Object topologyBuilder) {
        this.topologyBuilder = topologyBuilder;
    }
}
