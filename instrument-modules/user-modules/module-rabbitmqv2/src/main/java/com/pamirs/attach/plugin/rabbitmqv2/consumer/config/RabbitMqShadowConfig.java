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

package com.pamirs.attach.plugin.rabbitmqv2.consumer.config;

import com.rabbitmq.client.impl.ChannelN;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;

import java.util.Arrays;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/4 10:55
 */
public class RabbitMqShadowConfig extends ConsumerConfig {

    private final String queue;

    private final ChannelN channelN;

    private final Class[] paramTypes;

    private final Object[] args;

    public RabbitMqShadowConfig(String queue, ChannelN channelN, Class[] paramTypes, Object[] args) {
        this.queue = queue;
        this.channelN = channelN;
        this.paramTypes = paramTypes;
        this.args = args;
    }

    @Override
    public String keyOfConfig() {
        return "#" + queue;
    }

    @Override
    public String keyOfServer() {
        return "";
    }

    public String getQueue() {
        return queue;
    }

    public ChannelN getChannelN() {
        return channelN;
    }

    public Class[] getParamTypes() {
        return paramTypes;
    }

    public Object[] getArgs() {
        return args;
    }

    @Override
    public String toString() {
        return "RabbitMqShadowConfig{" +
                "queue='" + queue + '\'' +
                ", channelN=" + channelN +
                ", paramTypes=" + Arrays.toString(paramTypes) +
                ", args=" + Arrays.toString(args) +
                '}';
    }
}
