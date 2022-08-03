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

package com.pamirs.attach.plugin.apache.kafkav2.consumer.server;

import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/2 14:35
 */
public class KafkaShadowConsumerServer implements ShadowServer {

    private final KafkaConsumer shadowConsumer;

    private final AtomicBoolean started = new AtomicBoolean(false);

    public KafkaShadowConsumerServer(KafkaConsumer shadowConsumer) {
        this.shadowConsumer = shadowConsumer;
    }

    @Override
    public void start() {
        started.set(true);
    }

    @Override
    public boolean isRunning() {
        return started.get();
    }

    @Override
    public void stop() {
        shadowConsumer.close();
        started.set(false);
    }
}
