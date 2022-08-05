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
import com.pamirs.attach.plugin.rabbitmqv2.consumer.config.RabbitMqShadowConfig;
import com.pamirs.attach.plugin.rabbitmqv2.consumer.server.RabbitMqShadowServer;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.bean.SyncObjectData;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.impl.ChannelN;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowConsumerExecute;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfigWithData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/4 10:53
 */
public class RabbitMqV2Execute implements ShadowConsumerExecute {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMqV2Execute.class);

    @Override
    public List<ConsumerConfig> prepareConfig(SyncObjectData syncObjectData) {
        ChannelN channelN = (ChannelN) syncObjectData.getTarget();
        String queue = (String) syncObjectData.getArgs()[0];
        List<ConsumerConfig> configList = new ArrayList<ConsumerConfig>();
        configList.add(new RabbitMqShadowConfig(queue, channelN, syncObjectData.getParamTypes(), syncObjectData.getArgs()));
        return configList;
    }

    @Override
    public ShadowServer fetchShadowServer(List<ConsumerConfigWithData> configList) {
        //因为prepareConfig肯定只返回一个Config对象，所以这里去下标为0的就好了
        RabbitMqShadowConfig config = (RabbitMqShadowConfig) configList.get(0).getConsumerConfig();
        Channel shadowChannel = ChannelHolder.getOrShadowChannel(config.getChannelN());

        try {
            Method basicConsume = Reflect.on(shadowChannel).exactMethod("basicConsume", config.getParamTypes());
            Object[] args = config.getArgs();
            args[0] = Pradar.addClusterTestPrefix(args[0].toString());
            String shadowTag = basicConsume.invoke(shadowChannel, args).toString();
            return new RabbitMqShadowServer(shadowTag, shadowChannel);
        } catch (Exception e) {
            logger.error("[RabbitMq] shadow Channel basicConsume error, config:" + config, e);
            return null;
        }
    }
}
