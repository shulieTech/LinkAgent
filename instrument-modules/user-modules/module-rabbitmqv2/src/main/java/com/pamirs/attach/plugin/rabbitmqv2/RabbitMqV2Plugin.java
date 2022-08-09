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
import com.pamirs.attach.plugin.rabbitmqv2.producer.factory.ChannelNFactory;
import com.pamirs.attach.plugin.rabbitmqv2.producer.proxy.BasicPublishProxy;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import io.shulie.instrument.module.isolation.IsolationManager;
import io.shulie.instrument.module.isolation.enhance.EnhanceClass;
import io.shulie.instrument.module.isolation.proxy.ShadowMethodProxy;
import io.shulie.instrument.module.isolation.proxy.ShadowMethodProxyUtils;
import io.shulie.instrument.module.isolation.register.ShadowProxyConfig;
import io.shulie.instrument.module.isolation.resource.ShadowResourceProxyFactory;
import io.shulie.instrument.module.messaging.common.ResourceInit;
import io.shulie.instrument.module.messaging.consumer.ConsumerManager;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowConsumerExecute;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerIsolationRegister;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerRegister;
import io.shulie.instrument.module.messaging.consumer.module.isolation.ConsumerClass;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/4 10:39
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = RabbitMqV2Plugin.MODULE_NAME, version = "1.0.0", author = "wanglinglong@shulie.io", description = "rabbitmq 新版插件")
public class RabbitMqV2Plugin extends ModuleLifecycleAdapter implements ExtensionModule {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMqV2Plugin.class);

    public final static String MODULE_NAME = "rabbitmqv2";

    private final static String METHOD_SCOPE = "rabbitMqv2";

    @Override
    public boolean onActive() throws Throwable {
        ShadowProxyConfig proxyConfig = new ShadowProxyConfig(MODULE_NAME);
        proxyConfig
                .addEnhance(new EnhanceClass("com.rabbitmq.client.impl.ChannelN")
                        .setFactoryResourceInit(new io.shulie.instrument.module.isolation.common.ResourceInit<ShadowResourceProxyFactory>() {
                            @Override
                            public ShadowResourceProxyFactory init() {
                                return new ChannelNFactory();
                            }
                        })
                        .addEnhanceMethod("basicPublish", new io.shulie.instrument.module.isolation.common.ResourceInit<ShadowMethodProxy>() {
                            @Override
                            public ShadowMethodProxy init() {
                                return new BasicPublishProxy();
                            }
                        }, "java.lang.String", "java.lang.String", "boolean", "boolean", "com.rabbitmq.client.AMQP$BasicProperties", "byte[]"));

        IsolationManager.register(proxyConfig);


        ConsumerRegister consumerRegister = new ConsumerRegister(MODULE_NAME).consumerExecute(new ResourceInit<ShadowConsumerExecute>() {
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

        ConsumerIsolationRegister consumerIsolationRegister = new ConsumerIsolationRegister()
                .addConsumerClass(new ConsumerClass("com.rabbitmq.client.impl.ChannelN")
//                        .setConvertImpl(true)
                        .addEnhanceMethod("basicCancel", METHOD_SCOPE, ShadowMethodProxyUtils.defaultRoute(), "java.lang.String")
                        .addEnhanceMethod("basicAck", METHOD_SCOPE, ShadowMethodProxyUtils.defaultRoute(), "long", "boolean")
                        .addEnhanceMethod("basicAck", METHOD_SCOPE, ShadowMethodProxyUtils.defaultRoute(), "long", "boolean", "boolean")
                        .addEnhanceMethod("basicNack", METHOD_SCOPE, ShadowMethodProxyUtils.defaultRoute(), "long", "boolean")
                        .addEnhanceMethod("basicNack", METHOD_SCOPE, ShadowMethodProxyUtils.defaultRoute(), "long", "boolean", "boolean")
                        .addEnhanceMethod("basicReject", METHOD_SCOPE, ShadowMethodProxyUtils.defaultRoute(), "long", "boolean"))
                .addConsumerClass(new ConsumerClass("com.rabbitmq.client.impl.recovery.RecoveryAwareChannelN")
                        .addEnhanceMethod("basicAck", METHOD_SCOPE, ShadowMethodProxyUtils.defaultRoute(), "long", "boolean")
                        .addEnhanceMethod("basicAck", METHOD_SCOPE, ShadowMethodProxyUtils.defaultRoute(), "long", "boolean", "boolean")
                        .addEnhanceMethod("basicNack", METHOD_SCOPE, ShadowMethodProxyUtils.defaultRoute(), "long", "boolean")
                        .addEnhanceMethod("basicNack", METHOD_SCOPE, ShadowMethodProxyUtils.defaultRoute(), "long", "boolean", "boolean")
                        .addEnhanceMethod("basicReject", METHOD_SCOPE, ShadowMethodProxyUtils.defaultRoute(), "long", "boolean"));

        ConsumerManager.register(consumerRegister, consumerIsolationRegister, "com.rabbitmq.client.impl.ChannelN#basicConsume");
        return true;
    }

}