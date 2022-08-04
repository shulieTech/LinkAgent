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
package com.pamirs.attach.plugin.rabbitmqv2.consumer.model;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/11/17 4:13 下午
 */
public class ConsumerDetail {

    private final Connection connection;

    private final String consumerTag;

    private final Channel channel;

    private final Consumer consumer;

    private final String connectionLocalIp;

    private final int connectionLocalPort;

    public ConsumerDetail(Connection connection, String consumerTag, Channel channel, Consumer consumer,
                          String connectionLocalIp, int connectionLocalPort) {
        this.connection = connection;
        this.consumerTag = consumerTag;
        this.channel = channel;
        this.consumer = consumer;
        this.connectionLocalIp = connectionLocalIp;
        this.connectionLocalPort = connectionLocalPort;
    }

    public String getConsumerTag() {
        return consumerTag;
    }

    public Channel getChannel() {
        return channel;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public Connection getConnection() {
        return connection;
    }

    public String getConnectionLocalIp() {
        return connectionLocalIp;
    }

    public int getConnectionLocalPort() {
        return connectionLocalPort;
    }

    @Override
    public String toString() {
        return "ConsumerDetail{" +
            "connection=" + connection +
            ", consumerTag='" + consumerTag + '\'' +
            ", channel=" + channel +
            ", consumer=" + consumer +
            ", connectionLocalIp='" + connectionLocalIp + '\'' +
            ", connectionLocalPort=" + connectionLocalPort +
            '}';
    }
}
