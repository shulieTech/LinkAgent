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

package com.pamirs.attach.plugin.apache.kafka.streamv2.consumer.server;

import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;
import org.apache.kafka.streams.KafkaStreams;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/1 11:38
 */
public class KafkaShadowStreamServer implements ShadowServer {

    private final KafkaStreams shadowStream;

    private final AtomicBoolean started = new AtomicBoolean(false);

    public KafkaShadowStreamServer(KafkaStreams shadowStream) {
        this.shadowStream = shadowStream;
    }

    @Override
    public Object getShadowTarget() {
        return this.shadowStream;
    }

    @Override
    public void start() {
        started.set(true);
        if (shadowStream == null) {
            return;
        }
        shadowStream.start();
    }

    @Override
    public boolean isRunning() {
        return started.get();
    }

    @Override
    public void stop() {
        started.set(false);
        if (shadowStream == null) {
            return;
        }
        shadowStream.close();
    }
}
