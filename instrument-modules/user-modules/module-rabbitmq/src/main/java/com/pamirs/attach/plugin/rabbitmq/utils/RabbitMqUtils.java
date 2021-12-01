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
package com.pamirs.attach.plugin.rabbitmq.utils;

import com.pamirs.pradar.exception.PradarException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.impl.AMQConnection;
import com.rabbitmq.client.impl.ChannelN;
import com.rabbitmq.client.impl.recovery.AutorecoveringChannel;
import com.rabbitmq.client.impl.recovery.AutorecoveringConnection;
import com.shulie.instrument.simulator.api.reflect.Reflect;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/11/18 7:31 下午
 */
public class RabbitMqUtils {

    public static AMQConnection unWrapConnection(Connection connection) {
        if (connection instanceof AMQConnection) {
            return (AMQConnection)connection;
        }
        if (connection instanceof AutorecoveringConnection) {
            return Reflect.on(connection).get("delegate");
        }
        throw new PradarException("unsupport connection");
    }

    public static ChannelN unWrapChannel(Channel channel) {
        if (channel instanceof ChannelN) {
            return (ChannelN)channel;
        }
        if (channel instanceof AutorecoveringChannel) {
            return Reflect.on(channel).get("delegate");
        }
        throw new PradarException("unsupport connection");
    }
}
