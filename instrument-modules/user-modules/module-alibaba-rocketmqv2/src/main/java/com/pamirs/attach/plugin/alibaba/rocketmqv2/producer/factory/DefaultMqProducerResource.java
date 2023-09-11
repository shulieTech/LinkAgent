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

package com.pamirs.attach.plugin.alibaba.rocketmqv2.producer.factory;

import com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import io.shulie.instrument.module.isolation.resource.ShadowResourceLifecycle;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/4 16:00
 */
public class DefaultMqProducerResource implements ShadowResourceLifecycle {

    private final DefaultMQProducer producer;

    public DefaultMqProducerResource(DefaultMQProducer producer) {
        this.producer = producer;
    }

    @Override
    public Object getTarget() {
        return producer;
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public void start() {

    }

    @Override
    public void destroy(long timeout) {

    }
}
