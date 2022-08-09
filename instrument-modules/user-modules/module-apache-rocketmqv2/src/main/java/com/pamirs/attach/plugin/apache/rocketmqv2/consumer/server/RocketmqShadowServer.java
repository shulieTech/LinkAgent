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

package com.pamirs.attach.plugin.apache.rocketmqv2.consumer.server;

import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/7/29 15:49
 */
public class RocketmqShadowServer implements ShadowServer {

    private static final Logger logger = LoggerFactory.getLogger(RocketmqShadowServer.class);

    private final DefaultMQPushConsumer shadowConsumer;

    private final AtomicBoolean started = new AtomicBoolean(false);

    public RocketmqShadowServer(DefaultMQPushConsumer shadowConsumer) {
        this.shadowConsumer = shadowConsumer;
    }

    @Override
    public Object getShadowTarget() {
        return this.shadowConsumer;
    }

    @Override
    public void start() {
        try {
            shadowConsumer.start();
            started.set(true);
        } catch (Throwable e) {
            ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.MQ)
                    .setErrorCode("MQ-0001")
                    .setMessage("Apache-RocketMQ消费端启动失败！")
                    .setDetail("subscription:" + shadowConsumer.getDefaultMQPushConsumerImpl().getSubscriptionInner() + "||"
                            + e.getMessage())
                    .report();
            logger.error("Apache-RocketMQ: start shadow DefaultMQPushConsumer err! subscription:{}",
                    shadowConsumer.getDefaultMQPushConsumerImpl().getSubscriptionInner(), e);
        }
    }

    @Override
    public boolean isRunning() {
        return started.get();
    }

    @Override
    public void stop() {
        try {
            shadowConsumer.shutdown();
            started.set(false);
        } catch (Throwable e) {
            ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.MQ)
                    .setErrorCode("MQ-9999")
                    .setMessage("Apache-RocketMQ消费端关闭失败！")
                    .setDetail("subscription:" + shadowConsumer.getDefaultMQPushConsumerImpl().getSubscriptionInner() + "||"
                            + e.getMessage())
                    .report();
            logger.error("Apache-RocketMQ: shutdown shadow DefaultMQPushConsumer err! subscription:{}",
                    shadowConsumer.getDefaultMQPushConsumerImpl().getSubscriptionInner(), e);
        }
    }
}
