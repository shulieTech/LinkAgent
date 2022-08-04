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

package com.pamirs.attach.plugin.rabbitmqv2.consumer;

import com.pamirs.attach.plugin.rabbitmqv2.consumer.common.ChannelHolder;
import com.pamirs.attach.plugin.rabbitmqv2.consumer.common.ConfigCache;
import com.pamirs.attach.plugin.rabbitmqv2.consumer.common.support.cache.CacheSupportFactory;
import com.pamirs.attach.plugin.rabbitmqv2.consumer.config.RabbitMqShadowConfig;
import com.pamirs.attach.plugin.rabbitmqv2.consumer.model.ConsumerDetail;
import com.pamirs.attach.plugin.rabbitmqv2.consumer.model.ConsumerMetaData;
import com.pamirs.attach.plugin.rabbitmqv2.consumer.server.ConsumerMetaDataBuilder;
import com.pamirs.attach.plugin.rabbitmqv2.consumer.server.impl.AdminApiConsumerMetaDataBuilder;
import com.pamirs.attach.plugin.rabbitmqv2.consumer.server.impl.AutorecoveringChannelConsumerMetaDataBuilder;
import com.pamirs.attach.plugin.rabbitmqv2.consumer.server.impl.SpringConsumerDecoratorMetaDataBuilder;
import com.pamirs.attach.plugin.rabbitmqv2.consumer.server.impl.SpringConsumerMetaDataBuilder;
import com.pamirs.attach.plugin.rabbitmqv2.utils.AddressUtils;
import com.pamirs.attach.plugin.rabbitmqv2.utils.RabbitMqUtils;
import com.pamirs.pradar.bean.SyncObjectData;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.impl.AMQConnection;
import com.rabbitmq.client.impl.ChannelManager;
import com.rabbitmq.client.impl.ChannelN;
import com.rabbitmq.client.impl.SocketFrameHandler;
import com.rabbitmq.client.impl.recovery.AutorecoveringChannel;
import com.rabbitmq.client.impl.recovery.AutorecoveringConnection;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.resource.SimulatorConfig;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowConsumerExecute;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/4 10:53
 */
public class RabbitMqV2Execute implements ShadowConsumerExecute {

    private final List<ConsumerMetaDataBuilder> consumerMetaDataBuilders = new ArrayList<ConsumerMetaDataBuilder>();

    private static final Logger logger = LoggerFactory.getLogger(RabbitMqV2Execute.class);

    public RabbitMqV2Execute(SimulatorConfig simulatorConfig) throws Exception {
        consumerMetaDataBuilders.add(SpringConsumerMetaDataBuilder.getInstance());
        consumerMetaDataBuilders.add(SpringConsumerDecoratorMetaDataBuilder.getInstance());
        consumerMetaDataBuilders.add(AutorecoveringChannelConsumerMetaDataBuilder.getInstance());
        consumerMetaDataBuilders.add(new AdminApiConsumerMetaDataBuilder(simulatorConfig,
                CacheSupportFactory.create(simulatorConfig)));

    }

    @Override
    public List<ConsumerConfig> prepareConfig(SyncObjectData syncObjectData) {
        ChannelN channelN = (ChannelN) syncObjectData.getTarget();
        String queue = (String) syncObjectData.getArgs()[0];
        List<ConsumerConfig> configList = new ArrayList<ConsumerConfig>();
        configList.add(new RabbitMqShadowConfig(queue, channelN));
        return configList;
    }

    @Override
    public ShadowServer fetchShadowServer(ConsumerConfig config, String shadowConfig) {
        RabbitMqShadowConfig rabbitMqShadowConfig = (RabbitMqShadowConfig) config;
//        List<ConsumerDetail> consumerDetailList = getAllConsumersFromConnection(rabbitMqShadowConfig.getChannelN().getConnection());
//        for (ConsumerDetail consumerDetail : consumerDetailList) {
//            ConsumerMetaData metaData = getConsumerMetaData(consumerDetail);
//            if (metaData == null || !rabbitMqShadowConfig.getQueue().equals(metaData.getQueue())) {
//                continue;
//            }
//            Channel channel = consumerDetail.getChannel();
//            if (metaData.isUseSpring()) {
//                return null;
//            }
        String cTag = null;
        Channel shadowChannel = ChannelHolder.getOrShadowChannel(rabbitMqShadowConfig.getChannelN());
//        String shadowTag = consumeShadowQueue(channel, metaData);
//            try {
//        if (metaData.isUseOriginChannel()) {
//            //spring 要用业务本身的channel去订阅
//            cTag = channel.basicConsume(metaData.getPtQueue(), metaData.isAutoAck(),
//                    metaData.getPtConsumerTag(), false, metaData.isExclusive(),
//                    new HashMap<String, Object>(), metaData.getConsumer());
//        } else {
//            channel = ChannelHolder.getOrShadowChannel(channel);
//            cTag = consumeShadowQueue(channel, metaData);
//        }
//            } catch (Throwable e) {
//                logger.error("[RabbitMq] shadow channel consume error", e);
//            }
//        return new RabbitMqShadowServer(cTag, consumerDetail, metaData, channel);
//        }
        return null;
    }

    private String consumeShadowQueue(Channel target, ConsumerMetaData consumerMetaData) throws
            IOException {
        return consumeShadowQueue(target, consumerMetaData.getPtQueue(), consumerMetaData.isAutoAck(),
                consumerMetaData.getPtConsumerTag(), false, consumerMetaData.isExclusive(),
                consumerMetaData.getArguments(), consumerMetaData.getPrefetchCount(),
                consumerMetaData.getConsumer());
    }

    public String consumeShadowQueue(Channel target, String ptQueue, boolean autoAck, String ptConsumerTag, boolean noLocal, boolean exclusive, Map<String, Object> arguments, int prefetchCount, Consumer consumer) throws IOException {
        if (target == null) {
            logger.warn(
                    "[RabbitMQ] basicConsume failed. cause by shadow channel is not found. queue={}, consumerTag={}",
                    ptQueue, ptConsumerTag);
            return null;
        }
        if (!target.isOpen()) {
            logger.warn(
                    "[RabbitMQ] basicConsume failed. cause by shadow channel is not closed. queue={}, consumerTag={}",
                    ptQueue, ptConsumerTag);
            return null;
        }
        if (prefetchCount > 0) {
            target.basicQos(prefetchCount);
        }
        String result = target.basicConsume(ptQueue, autoAck, ptConsumerTag, noLocal, exclusive, arguments,
                consumer);
        final int key = System.identityHashCode(target);
        return result;
    }


    private ConsumerMetaData getConsumerMetaData(ConsumerDetail deliverDetail) {
        Channel channel = deliverDetail.getChannel();
        String consumerTag = deliverDetail.getConsumerTag();
        final int key = System.identityHashCode(channel);
        ConsumerMetaData consumerMetaData = ConfigCache.getConsumerMetaData(key, consumerTag);
        if (consumerMetaData == null) {
            synchronized (RabbitMqV2Execute.class) {
                consumerMetaData = ConfigCache.getConsumerMetaData(key, consumerTag);
                if (consumerMetaData == null) {
                    consumerMetaData = buildConsumerMetaData(deliverDetail);
                    if (consumerMetaData != null) {
                        ConfigCache.putConsumerMetaData(key, consumerTag, consumerMetaData);
                    }
                }
            }
        }
        return consumerMetaData;
    }

    private ConsumerMetaData buildConsumerMetaData(ConsumerDetail deliverDetail) {
        for (ConsumerMetaDataBuilder consumerMetaDataBuilder : consumerMetaDataBuilders) {
            ConsumerMetaData consumerMetaData = consumerMetaDataBuilder.tryBuild(deliverDetail);
            if (consumerMetaData != null) {
                return consumerMetaData;
            }
        }
        return null;
    }

    private List<ConsumerDetail> getAllConsumersFromConnection(Connection connection) {
        List<ConsumerDetail> consumerDetails = new ArrayList<ConsumerDetail>();
        Set<Channel> channels = new HashSet<Channel>();
        if (connection instanceof AMQConnection) {
            ChannelManager _channelManager = Reflect.on(connection).get("_channelManager");
            Map<Integer, ChannelN> _channelMap = Reflect.on(_channelManager).get("_channelMap");
            channels.addAll(_channelMap.values());
        } else if (connection instanceof AutorecoveringConnection) {
            Map<Integer, AutorecoveringChannel> _channels = Reflect.on(connection).get("channels");
            channels.addAll(_channels.values());
        } else {
            logger.error("[RabbitMQ] SIMULATOR unsupport rabbitmqConnection");
        }
        AMQConnection amqConnection = RabbitMqUtils.unWrapConnection(connection);
        SocketFrameHandler frameHandler = Reflect.on(amqConnection).get("_frameHandler");
        String localIp = frameHandler.getLocalAddress().getHostAddress();
        if (isLocalHost(localIp)) {
            localIp = AddressUtils.getLocalAddress();
            logger.warn("[RabbitMQ] SIMULATOR get localIp from connection is localIp use {} instead", localIp);
        }
        int localPort = frameHandler.getLocalPort();
        for (Channel channel : channels) {
            ChannelN channelN = RabbitMqUtils.unWrapChannel(channel);
            Map<String, Consumer> _consumers = Reflect.on(channelN).get("_consumers");
            for (Map.Entry<String, Consumer> entry : _consumers.entrySet()) {
                consumerDetails.add(new ConsumerDetail(connection, entry.getKey(),
                        channel, entry.getValue(), localIp, localPort));
            }
        }
        return consumerDetails;
    }

    private boolean isLocalHost(String ip) {
        return "localhost".equals(ip) || "127.0.0.1".equals(ip);
    }
}
