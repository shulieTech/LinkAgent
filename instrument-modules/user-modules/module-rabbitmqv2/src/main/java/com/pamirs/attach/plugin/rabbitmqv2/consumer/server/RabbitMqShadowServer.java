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

package com.pamirs.attach.plugin.rabbitmqv2.consumer.server;

import com.rabbitmq.client.Channel;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/4 10:55
 */
public class RabbitMqShadowServer implements ShadowServer {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMqShadowServer.class);

    private final String ptConsumerTag;
    private final Channel channel;

    public RabbitMqShadowServer(String ptConsumerTag, Channel channel) {
        this.ptConsumerTag = ptConsumerTag;
        this.channel = channel;
    }

    @Override
    public void start() {
    }

    @Override
    public boolean isRunning() {
        return channel.isOpen();
    }

    @Override
    public void stop() {
        try {
            channel.close();
        } catch (Exception e) {
            logger.error("[RabbitMq] channel close error", e);
        }
    }

    public String getPtConsumerTag() {
        return ptConsumerTag;
    }

    public Channel getChannel() {
        return channel;
    }
}
