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

package com.pamirs.attach.plugin.apache.kafka.streamv2;

import com.pamirs.attach.plugin.apache.kafka.streamv2.consumer.KafkaShadowStreamStartExecute;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import io.shulie.instrument.module.messaging.consumer.ConsumerManager;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerRegister;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/1 11:28
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "kafka-streamv2", version = "1.0.0", author = "wanglinglong@shulie.io", description = "apache-kafka-stream新版插件")
public class KafkaStreamV2plugin extends ModuleLifecycleAdapter implements ExtensionModule {
    private static final Logger logger = LoggerFactory.getLogger(KafkaStreamV2plugin.class);

    @Override
    public boolean onActive() throws Throwable {
//        ConsumerRegister streamMapRegister = new ConsumerRegister().consumerExecute(new KafkaShadowStreamMapExecute());
//        ConsumerManager.register(streamMapRegister, "org.apache.kafka.streams.kstream.internals.KStreamMap$KStreamMapProcessor#process");
//
//        ConsumerRegister streamPeekRegister = new ConsumerRegister().consumerExecute(new KafkaShadowStreamPeekExecute());
//        ConsumerManager.register(streamPeekRegister, "org.apache.kafka.streams.kstream.internals.KStreamPeek$KStreamPeekProcessor#process");

        ConsumerRegister streamMapRegister = new ConsumerRegister("kafka-streamv2").consumerExecute(KafkaShadowStreamStartExecute::new);
        ConsumerManager.register(streamMapRegister, "org.apache.kafka.streams.KafkaStreams#start");
        return true;
    }
}
