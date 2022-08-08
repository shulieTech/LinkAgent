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

package com.pamirs.attach.plugin.alibaba.rocketmqv2;

import com.pamirs.attach.plugin.alibaba.rocketmqv2.consumer.RocketmqShadowConsumerExecute;
import com.pamirs.attach.plugin.alibaba.rocketmqv2.producer.factory.DefaultMqProducerFactory;
import com.pamirs.attach.plugin.alibaba.rocketmqv2.producer.proxy.SendProxy;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import io.shulie.instrument.module.isolation.IsolationManager;
import io.shulie.instrument.module.isolation.enhance.EnhanceClass;
import io.shulie.instrument.module.isolation.register.ShadowProxyConfig;
import io.shulie.instrument.module.messaging.consumer.ConsumerManager;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerRegister;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/7/29 11:29
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = RocketMqv2Plugin.MODULE_NAME, version = "1.0.0", author = "wanglinglong@shulie.io", description = "alibaba-rocketmq新版插件")
public class RocketMqv2Plugin extends ModuleLifecycleAdapter implements ExtensionModule {

    public static final String MODULE_NAME = "alibaba-rocketmqv2";
    private static final Logger logger = LoggerFactory.getLogger(RocketMqv2Plugin.class);

    @Override
    public boolean onActive() throws Throwable {
        ShadowProxyConfig defaultMqProducerProxyConfig = new ShadowProxyConfig(MODULE_NAME);
        defaultMqProducerProxyConfig
                .addEnhance(new EnhanceClass("com.alibaba.rocketmq.client.producer.DefaultMQProducer")
                        .setFactoryResourceInit(DefaultMqProducerFactory::new)
                        .addEnhanceMethods(SendProxy::new, "send")
                        .addEnhanceMethods(SendProxy::new, "sendOneway"))
                .addEnhance(new EnhanceClass("com.alibaba.rocketmq.client.impl.producer.DefaultMQProducerImpl")
                        .setFactoryResourceInit(DefaultMqProducerFactory::new)
                        .addEnhanceMethods(SendProxy::new, "sendDefaultImpl", "sendMessageInTransaction")
                );

        IsolationManager.register(defaultMqProducerProxyConfig);

        ConsumerRegister consumerRegister = new ConsumerRegister(MODULE_NAME).consumerExecute(RocketmqShadowConsumerExecute::new);
        ConsumerManager.register(consumerRegister, "com.alibaba.rocketmq.client.consumer.DefaultMQPushConsumer#start");
        return true;
    }
}
