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

import com.pamirs.attach.plugin.rabbitmqv2.constant.RabbitMqConstant;
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
        List<ConsumerConfig> configList = new ArrayList<ConsumerConfig>();
        if (!verifyParam(syncObjectData.getParamTypes())) {
            return configList;
        }
        // 对于Spring-rabbitMq不需要走这个逻辑
        Object consumer = syncObjectData.getArgs()[6];
        if (consumer != null
                && ("org.springframework.amqp.rabbit.listener.BlockingQueueConsumer$InternalConsumer".equals(consumer.getClass().getName())
                || "org.springframework.amqp.rabbit.listener.BlockingQueueConsumer$ConsumerDecorator".equals(consumer.getClass().getName()))) {
            RabbitMqConstant.SPRING_RABBIT = true;
            return configList;
        }

        ChannelN channelN = (ChannelN) syncObjectData.getTarget();
        String queue = (String) syncObjectData.getArgs()[0];
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

    /**
     * 只需要切com.rabbitmq.client.impl.ChannelN#basicConsume(java.lang.String, boolean, java.lang.String, boolean, boolean, java.util.Map<java.lang.String,java.lang.Object>, com.rabbitmq.client.Consumer)
     * 就可以了
     *
     * @param paramTypes 参数类型集合
     * @return true 参数合法、false 参数不合法
     */
    private boolean verifyParam(Class[] paramTypes) {
        if (paramTypes.length != 7) {
            return false;
        }
        boolean first = "java.lang.String".equals(paramTypes[0].getName());
        boolean second = "boolean".equals(paramTypes[1].getName());
        boolean third = "java.lang.String".equals(paramTypes[2].getName());
        boolean fourth = "boolean".equals(paramTypes[3].getName());
        boolean fifth = "boolean".equals(paramTypes[4].getName());
        boolean sixth = "java.util.Map".equals(paramTypes[5].getName());
        boolean seventh = "com.rabbitmq.client.Consumer".equals(paramTypes[6].getName());
        return first && second && third && fourth && fifth && sixth && seventh;
    }
}
