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

package com.pamirs.attach.plugin.apache.kafka.streamv2.consumer;

import com.pamirs.pradar.bean.SyncObjectData;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowConsumerExecute;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfigWithData;
import org.apache.kafka.streams.KafkaStreams;

import java.util.List;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/1 11:33
 */
public class KafkaShadowStreamPeekExecute extends AbstractKafkaStreamExecute implements ShadowConsumerExecute {

    @Override
    public List<ConsumerConfig> prepareConfig(SyncObjectData syncObjectData) {
        KafkaStreams bizStreams = (KafkaStreams) syncObjectData.getTarget();
        return null;
    }

    @Override
    public ShadowServer fetchShadowServer(List<ConsumerConfigWithData> configList) {
        return null;
    }

}
