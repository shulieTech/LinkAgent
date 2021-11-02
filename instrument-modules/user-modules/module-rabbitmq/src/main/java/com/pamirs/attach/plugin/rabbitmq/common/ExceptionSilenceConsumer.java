/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.rabbitmq.common;

import java.io.IOException;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/09/01 3:59 下午
 */
public class ExceptionSilenceConsumer implements Consumer {

    private final Consumer consumer;

    private final Logger log = LoggerFactory.getLogger(ExceptionSilenceConsumer.class);

    public ExceptionSilenceConsumer(Consumer consumer) {this.consumer = consumer;}

    @Override
    public void handleConsumeOk(String consumerTag) {
        if (PradarService.isSilence()) {
            return;
        }
        try {
            this.consumer.handleConsumeOk(consumerTag);
        } catch (Throwable e) {
            log.error("shadow consumer invoke handleConsumeOk fail", e);
        }
    }

    @Override
    public void handleCancelOk(String consumerTag) {
        if (PradarService.isSilence()) {
            return;
        }
        try {
            this.consumer.handleCancelOk(consumerTag);
        } catch (Throwable e) {
            log.error("shadow consumer invoke handleCancelOk fail", e);
        }
    }

    @Override
    public void handleCancel(String consumerTag) throws IOException {
        if (PradarService.isSilence()) {
            return;
        }
        try {
            this.consumer.handleCancel(consumerTag);
        } catch (Throwable e) {
            log.error("shadow consumer invoke handleCancel fail", e);
        }
    }

    @Override
    public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
        if (PradarService.isSilence()) {
            return;
        }
        try {
            this.consumer.handleShutdownSignal(consumerTag, sig);
        } catch (Throwable e) {
            log.error("shadow consumer invoke handleShutdownSignal fail", e);
        }
    }

    @Override
    public void handleRecoverOk(String consumerTag) {
        if (PradarService.isSilence()) {
            return;
        }
        try {
            this.consumer.handleRecoverOk(consumerTag);
        } catch (Throwable e) {
            log.error("shadow consumer invoke handleRecoverOk fail", e);
        }
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
        throws IOException {
        if (PradarService.isSilence()) {
            return;
        }
        try {
            this.consumer.handleDelivery(consumerTag, envelope, properties, body);
        } catch (Throwable e) {
            log.error("shadow consumer invoke handleDelivery fail", e);
        }
    }
}
