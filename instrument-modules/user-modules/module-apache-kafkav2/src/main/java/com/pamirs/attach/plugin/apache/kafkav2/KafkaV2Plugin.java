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

package com.pamirs.attach.plugin.apache.kafkav2;

import com.pamirs.attach.plugin.apache.kafkav2.producer.factory.ApacheKafkaProducerFactory;
import com.pamirs.attach.plugin.apache.kafkav2.producer.factory.KafkaProducerFactory;
import com.pamirs.attach.plugin.apache.kafkav2.producer.proxy.JavaApiProducerSendProxy;
import com.pamirs.attach.plugin.apache.kafkav2.producer.proxy.SendMethodProxy;
import com.pamirs.attach.plugin.apache.kafkav2.producer.proxy.SendOffsetsToTransactionMethodProxy;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import io.shulie.instrument.module.isolation.IsolationManager;
import io.shulie.instrument.module.isolation.enhance.EnhanceClass;
import io.shulie.instrument.module.isolation.proxy.ShadowMethodProxyUtils;
import io.shulie.instrument.module.isolation.proxy.impl.AddClusterRouteShadowMethodProxy;
import io.shulie.instrument.module.isolation.register.ShadowProxyConfig;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/2 10:10
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = KafkaV2Plugin.moduleName, version = "1.0.0", author = "wanglinglong@shulie.io", description = "kafka新版插件")
public class KafkaV2Plugin extends ModuleLifecycleAdapter implements ExtensionModule {
    private static final Logger logger = LoggerFactory.getLogger(KafkaV2Plugin.class);
    static final String moduleName = "kafkav2";

    @Override
    public boolean onActive() throws Throwable {
        ShadowProxyConfig proxyConfig = new ShadowProxyConfig(moduleName);
        proxyConfig
                .addEnhance(new EnhanceClass("org.apache.kafka.clients.producer.KafkaProducer")
                        .setFactoryResourceInit(ApacheKafkaProducerFactory::new)
                        .addEnhanceMethod("send", SendMethodProxy::new, "org.apache.kafka.clients.producer.ProducerRecord", "org.apache.kafka.clients.producer.Callback")
                        .addEnhanceMethod("sendOffsetsToTransaction", SendOffsetsToTransactionMethodProxy::new)
                        .addEnhanceMethod("partitionsFor", () -> new AddClusterRouteShadowMethodProxy(0))
                        .addEnhanceMethods("abortTransaction", "beginTransaction", "close", "close", "commitTransaction", "flush", "initTransactions", "metrics"))
                .addEnhance(new EnhanceClass("kafka.javaapi.producer.Producer")
                        .setFactoryResourceInit(KafkaProducerFactory::new)
                        .addEnhanceMethods(JavaApiProducerSendProxy::new,"send")
                );

        IsolationManager.register(proxyConfig);

        if (simulatorConfig.getBooleanProperty("kafka.use.other.plugin", false)) {
            return true;
        }

//        ConsumerRegister streamMapRegister = new ConsumerRegister().consumerExecute(KafkaExecute.class);
//        ConsumerManager.register(streamMapRegister, "org.apache.kafka.clients.consumer.KafkaConsumer#subscribe");
        return true;
    }
}
