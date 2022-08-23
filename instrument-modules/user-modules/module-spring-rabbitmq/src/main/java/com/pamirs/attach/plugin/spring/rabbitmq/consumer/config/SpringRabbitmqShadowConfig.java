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

package com.pamirs.attach.plugin.spring.rabbitmq.consumer.config;

import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/3 14:32
 */
public class SpringRabbitmqShadowConfig extends ConsumerConfig {

    private final String queue;

    private final AbstractMessageListenerContainer abstractMessageListenerContainer;

    public SpringRabbitmqShadowConfig(String queue, AbstractMessageListenerContainer abstractMessageListenerContainer) {
        this.queue = queue;
        this.abstractMessageListenerContainer = abstractMessageListenerContainer;
    }

    @Override
    public String keyOfConfig() {
        return "#" + this.queue;
    }

    @Override
    public String keyOfServer() {
        return "";
    }

    public String getQueue() {
        return queue;
    }

    public AbstractMessageListenerContainer getAbstractMessageListenerContainer() {
        return abstractMessageListenerContainer;
    }
}
