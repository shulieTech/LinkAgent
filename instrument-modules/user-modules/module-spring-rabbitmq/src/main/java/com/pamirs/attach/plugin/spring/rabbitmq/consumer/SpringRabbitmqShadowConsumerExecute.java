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

package com.pamirs.attach.plugin.spring.rabbitmq.consumer;

import com.pamirs.attach.plugin.spring.rabbitmq.consumer.config.SpringRabbitmqShadowConfig;
import com.pamirs.attach.plugin.spring.rabbitmq.consumer.server.SpringRabbitmqShadowServer;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.bean.SyncObjectData;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.util.StringUtil;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowConsumerExecute;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfigWithData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/3 14:30
 */
public class SpringRabbitmqShadowConsumerExecute implements ShadowConsumerExecute {

    private static final Logger logger = LoggerFactory.getLogger(SpringRabbitmqShadowConsumerExecute.class);

    @Override
    public List<ConsumerConfig> prepareConfig(SyncObjectData syncObjectData) {
        AbstractMessageListenerContainer abstractMessageListenerContainer
                = (AbstractMessageListenerContainer) syncObjectData.getTarget();
        String[] queueNames = abstractMessageListenerContainer.getQueueNames();
        List<ConsumerConfig> configList = new ArrayList<>();
        for (String queue : queueNames) {
            configList.add(new SpringRabbitmqShadowConfig(queue, abstractMessageListenerContainer));
        }
        return configList;
    }

    @Override
    public ShadowServer fetchShadowServer(List<ConsumerConfigWithData> configList) {
        AbstractMessageListenerContainer shadowContainer;

        SpringRabbitmqShadowConfig config = (SpringRabbitmqShadowConfig) configList.get(0).getConsumerConfig();
        List<String> needRegisterQueue = configList.stream().map(item -> ((SpringRabbitmqShadowConfig) item.getConsumerConfig()).getQueue()).collect(Collectors.toList());

        try {
            shadowContainer = createShadowContainer(config, needRegisterQueue);
        } catch (Exception e) {
            logger.error("[RabbitMQ] createShadowContainer error", e);
            throw new PressureMeasureError(e);
        }
        return new SpringRabbitmqShadowServer(shadowContainer);
    }


    /**
     * 创建影子container
     *
     * @param config            业务配置
     * @param needRegisterQueue 需要注册的queue
     * @return 影子AbstractMessageListenerContainer
     */
    private AbstractMessageListenerContainer createShadowContainer(SpringRabbitmqShadowConfig config, List<String> needRegisterQueue) throws Exception {
        final AbstractMessageListenerContainer busContainer = config.getAbstractMessageListenerContainer();
        String listenerId = busContainer.getListenerId();
        if (StringUtil.isEmpty(listenerId)) {
            listenerId = String.valueOf(busContainer.hashCode());
        }
        final String beanName = Reflect.on(busContainer).get("beanName");
        final AbstractMessageListenerContainer ptContainer = busContainer.getClass().newInstance();
        String[] ptQueueNames = needRegisterQueue.stream().map(Pradar::addClusterTestPrefix).toArray(String[]::new);
        ptContainer.setQueueNames(ptQueueNames);
        ptContainer.setListenerId(Pradar.addClusterTestPrefix(listenerId));
        if (!StringUtil.isEmpty(beanName)) {
            ptContainer.setBeanName(Pradar.addClusterTestPrefix(beanName));
        }
        initAbstractMessageListenerContainer(busContainer, ptContainer, "errorHandler",
                "messageConverter", "acknowledgeMode", "channelTransacted", "autoStartup", "phase",
                "applicationContext", "messageListener");
        final ConnectionFactory busConnectionFactory = busContainer.getConnectionFactory();

        if (busConnectionFactory instanceof CachingConnectionFactory
                && ((CachingConnectionFactory) busConnectionFactory).getCacheMode().equals(CachingConnectionFactory.CacheMode.CHANNEL)) {
            CachingConnectionFactory busCachingConnectionFactory
                    = (CachingConnectionFactory) busConnectionFactory;
            CachingConnectionFactory ptConnectionFactory = new CachingConnectionFactory();
            ptConnectionFactory.setHost(busCachingConnectionFactory.getHost());
            ptConnectionFactory.setPort(busCachingConnectionFactory.getPort());
            try {
                final Object addresses = Reflect.on(busCachingConnectionFactory).get("addresses");
                if (addresses != null) {
                    Reflect.on(ptConnectionFactory).set("addresses", addresses);
                }
            } catch (Throwable e) {
                logger.warn("[RabbitMQ] CachingConnectionFactory find field addresses fail", e);
            }
            ptConnectionFactory.setUsername(busCachingConnectionFactory.getUsername());
            ptConnectionFactory.setPassword(
                    busCachingConnectionFactory.getRabbitConnectionFactory().getPassword());
            ptConnectionFactory.setVirtualHost(busCachingConnectionFactory.getVirtualHost());
            ptConnectionFactory.setConnectionTimeout(
                    busCachingConnectionFactory.getRabbitConnectionFactory().getConnectionTimeout());
            ptConnectionFactory.setConnectionThreadFactory(
                    ((CachingConnectionFactory) busConnectionFactory).getRabbitConnectionFactory()
                            .getThreadFactory());
            ptConnectionFactory.setRequestedHeartBeat(
                    busCachingConnectionFactory.getRabbitConnectionFactory().getRequestedHeartbeat());
            initCachingConnectionFactory(busCachingConnectionFactory, ptConnectionFactory, "applicationContext",
                    "connectionLimit", "publisherConfirms", "channelCacheSize", "channelCheckoutTimeout",
                    "closeExceptionLogger", "connectionCacheSize", "publisherConfirms", "publisherReturns",
                    "publisherConfirms", "connectionNameStrategy", "executorService");
            ptConnectionFactory.afterPropertiesSet();
            ptContainer.setConnectionFactory(ptConnectionFactory);
        } else {
            ptContainer.setConnectionFactory(busConnectionFactory);
        }
        if (ptContainer instanceof SimpleMessageListenerContainer) {
            initSimpleMessageListenerContainer((SimpleMessageListenerContainer) busContainer,
                    (SimpleMessageListenerContainer) ptContainer, "concurrentConsumers", "maxConcurrentConsumers",
                    "startConsumerMinInterval", "stopConsumerMinInterval", "consecutiveActiveTrigger",
                    "consecutiveIdleTrigger", "prefetchCount", "receiveTimeout", "defaultRequeueRejected",
                    "adviceChain", "recoveryBackOff", "mismatchedQueuesFatal", "missingQueuesFatal",
                    "consumerTagStrategy", "idleEventInterval", "applicationEventPublisher");
        }
        ptContainer.afterPropertiesSet();
        logger.info(
                String.format("[RabbitMQ] shadow consumer create successfully.ptQueueNames: %s", ptQueueNames));
        return ptContainer;
    }

    private void initSimpleMessageListenerContainer(SimpleMessageListenerContainer busContainer,
                                                    SimpleMessageListenerContainer ptContainer, String... fields) {
        final List<String> missFiledList = new ArrayList<String>();
        for (String field : fields) {
            try {
                Reflect.on(ptContainer).set(field, Reflect.on(busContainer).get(field));
            } catch (Throwable e) {
                missFiledList.add(field);
            }
        }
        if (!missFiledList.isEmpty()) {
            logger.warn(String.format("[RabbitMQ] initSimpleMessageListenerContainer miss fileds:%s",
                    Arrays.toString(missFiledList.toArray())));
        }
    }

    private void initAbstractMessageListenerContainer(AbstractMessageListenerContainer busContainer,
                                                      AbstractMessageListenerContainer ptContainer, String... fields) {
        final List<String> missFiledList = new ArrayList<String>();
        for (String field : fields) {
            try {
                Reflect.on(ptContainer).set(field, Reflect.on(busContainer).get(field));
            } catch (Throwable e) {
                missFiledList.add(field);
            }
        }
        if (!missFiledList.isEmpty()) {
            logger.warn(String.format("[RabbitMQ] initAbstractMessageListenerContainer miss fileds:%s",
                    Arrays.toString(missFiledList.toArray())));
        }
    }

    private void initCachingConnectionFactory(CachingConnectionFactory busCachingConnectionFactory,
                                              CachingConnectionFactory ptCachingConnectionFactory, String... fields) {
        final List<String> missFiledList = new ArrayList<String>();
        for (String field : fields) {
            try {
                Reflect.on(ptCachingConnectionFactory).set(field, Reflect.on(busCachingConnectionFactory).get(field));
            } catch (Throwable e) {
                missFiledList.add(field);
            }
        }
        if (!missFiledList.isEmpty()) {
            logger.warn(String.format("[RabbitMQ] initCachingConnectionFactory miss fileds:%s",
                    Arrays.toString(missFiledList.toArray())));
        }
    }
}
