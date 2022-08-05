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

package com.pamirs.attach.plugin.rabbitmqv2;

import com.pamirs.attach.plugin.rabbitmqv2.consumer.RabbitMqV2Execute;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import io.shulie.instrument.module.messaging.common.ResourceInit;
import io.shulie.instrument.module.messaging.consumer.ConsumerManager;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowConsumerExecute;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerRegister;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/4 10:39
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "rabbitmqv2", version = "1.0.0", author = "wanglinglong@shulie.io", description = "rabbitmq 新版插件")
public class RabbitMqV2Plugin extends ModuleLifecycleAdapter implements ExtensionModule {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMqV2Plugin.class);

    @Override
    public boolean onActive() throws Throwable {
        ConsumerRegister consumerRegister = new ConsumerRegister().consumerExecute(new ResourceInit<ShadowConsumerExecute>() {
            @Override
            public ShadowConsumerExecute init() {
                try {
                    return new RabbitMqV2Execute();
                } catch (Exception e) {
                    logger.error("[RabbitMQ] init Rabbitmq execute error", e);
                    return null;
                }
            }
        });
        ConsumerManager.register(consumerRegister, "com.rabbitmq.client.impl.ChannelN#basicConsume");
        return true;
    }
}