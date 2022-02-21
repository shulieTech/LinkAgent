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
package com.pamirs.attach.plugin.rabbitmq.interceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.pamirs.attach.plugin.rabbitmq.RabbitmqConstants;
import com.pamirs.attach.plugin.rabbitmq.common.LastMqWhiteListHolder;
import com.pamirs.attach.plugin.rabbitmq.destroy.RabbitmqDestroy;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.util.StringUtil;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory.CacheMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;

/**
 * @Author: guohz
 * @ClassName: ChannelBasicGetInterceptor
 * @Package: com.pamirs.attach.plugin.rabbitmq.interceptor
 * @Date: 2019-07-25 14:33
 * @Description:
 */
@Destroyable(RabbitmqDestroy.class)
public class SpringBlockingQueueConsumerDeliveryInterceptor extends TraceInterceptorAdaptor {

    public static final ConcurrentHashMap<String, Object> RUNNING_CONTAINER
        = new ConcurrentHashMap<String, Object>();
    private static final ConcurrentHashMap<String, Date> CONTAINER_AT_LAST_RESTART_DATE
        = new ConcurrentHashMap<String, Date>();

    private static final AtomicBoolean MQ_WHITELIST_CHANGED = new AtomicBoolean(false);

    @Override
    protected boolean isClient(Advice advice) {
        return false;
    }

    @Override
    public void beforeLast(Advice advice) {
        final AbstractMessageListenerContainer abstractMessageListenerContainer
            = (AbstractMessageListenerContainer)advice.getTarget();
        final String listenerId = abstractMessageListenerContainer.getListenerId();
        String cacheKey = listenerId;
        if (StringUtil.isEmpty(cacheKey)) {
            cacheKey = String.valueOf(abstractMessageListenerContainer.hashCode());
        }
        final String[] queueNames = abstractMessageListenerContainer.getQueueNames();
        try {
            final Set<String> mqWhiteList = GlobalConfig.getInstance().getMqWhiteList();
            if (!Pradar.isClusterTestPrefix(cacheKey) && RUNNING_CONTAINER.putIfAbsent(cacheKey, new Object())
                == null) {
                final String beanName = Reflect.on(abstractMessageListenerContainer).get("beanName");
                final AbstractMessageListenerContainer ptContainer = abstractMessageListenerContainer.getClass()
                    .newInstance();
                final List<String> ptQueueNameList = new ArrayList<String>();
                for (String queueName : queueNames) {
                    if (!mqWhiteList.contains(queueName + "#")
                        && !mqWhiteList.contains("#" + queueName)) {
                        continue;
                    }
                    ptQueueNameList.add(Pradar.addClusterTestPrefix(queueName));
                }
                if (!ptQueueNameList.isEmpty()) {
                    final String[] ptQueueNames = ptQueueNameList.toArray(new String[] {});
                    ptContainer.setQueueNames(ptQueueNames);
                    if (!StringUtil.isEmpty(listenerId)) {
                        ptContainer.setListenerId(Pradar.addClusterTestPrefix(listenerId));
                    } else {
                        ptContainer.setListenerId(Pradar.addClusterTestPrefix(cacheKey));
                    }
                    if (!StringUtil.isEmpty(beanName)) {
                        ptContainer.setBeanName(Pradar.addClusterTestPrefix(beanName));
                    }
                    initAbstractMessageListenerContainer(abstractMessageListenerContainer, ptContainer,
                        "errorHandler",
                        "messageConverter",
                        "acknowledgeMode",
                        "channelTransacted",
                        "autoStartup",
                        "phase",
                        "applicationContext",
                        "messageListener"
                    );
                    final ConnectionFactory busConnectionFactory
                        = abstractMessageListenerContainer.getConnectionFactory();
                    /*
                    if this bus Container use CachingConnectionFactory and cacheMode is CHANNEL, create new
                    CachingConnectionFactory for
                     pt container.
                    because if not do this, when trigger PressureMeasureError, the connection of bus and pt container
                    use together while be close.
                    also see com.rabbitmq.client.impl.AMQConnection.doFinalShutdown
                     */
                    if (busConnectionFactory instanceof CachingConnectionFactory
                        && ((CachingConnectionFactory)busConnectionFactory).getCacheMode().equals(
                        CacheMode.CHANNEL)) {
                        CachingConnectionFactory busCachingConnectionFactory
                            = (CachingConnectionFactory)busConnectionFactory;
                        CachingConnectionFactory ptConnectionFactory = new CachingConnectionFactory();
                        ptConnectionFactory.setHost(busCachingConnectionFactory.getHost());
                        ptConnectionFactory.setPort(busCachingConnectionFactory.getPort());
                        try {
                            final Address[] addresses = Reflect.on(busCachingConnectionFactory).get("addresses");
                            if (addresses != null) {
                                Reflect.on(ptConnectionFactory).set("addresses", addresses);
                            }
                        } catch (Throwable e) {
                            LOGGER.warn("[RabbitMQ] CachingConnectionFactory find field addresses fail", e);
                        }
                        ptConnectionFactory.setUsername(busCachingConnectionFactory.getUsername());
                        ptConnectionFactory.setPassword(busCachingConnectionFactory.getRabbitConnectionFactory()
                            .getPassword());
                        ptConnectionFactory.setVirtualHost(busCachingConnectionFactory.getVirtualHost());
                        ptConnectionFactory.setConnectionTimeout(
                            busCachingConnectionFactory.getRabbitConnectionFactory().getConnectionTimeout());
                        ptConnectionFactory.setConnectionThreadFactory(
                            ((CachingConnectionFactory)busConnectionFactory).getRabbitConnectionFactory()
                                .getThreadFactory());
                        ptConnectionFactory.setRequestedHeartBeat(
                            busCachingConnectionFactory.getRabbitConnectionFactory().getRequestedHeartbeat());
                        initCachingConnectionFactory(busCachingConnectionFactory, ptConnectionFactory,
                            "applicationContext",
                            "connectionLimit",
                            "publisherConfirms",
                            "channelCacheSize",
                            "channelCheckoutTimeout",
                            "closeExceptionLogger",
                            "connectionCacheSize",
                            "publisherConfirms",
                            "publisherReturns",
                            "publisherConfirms",
                            "connectionNameStrategy",
                            "executorService");
                        ptConnectionFactory.afterPropertiesSet();
                        ptContainer.setConnectionFactory(ptConnectionFactory);
                    } else {
                        ptContainer.setConnectionFactory(busConnectionFactory);
                    }
                    if (ptContainer instanceof SimpleMessageListenerContainer) {
                        initSimpleMessageListenerContainer(
                            (SimpleMessageListenerContainer)abstractMessageListenerContainer,
                            (SimpleMessageListenerContainer)ptContainer,
                            "concurrentConsumers",
                            "maxConcurrentConsumers",
                            "startConsumerMinInterval",
                            "stopConsumerMinInterval",
                            "consecutiveActiveTrigger",
                            "consecutiveIdleTrigger",
                            "prefetchCount",
                            "receiveTimeout",
                            "defaultRequeueRejected",
                            "adviceChain",
                            "recoveryBackOff",
                            "mismatchedQueuesFatal",
                            "missingQueuesFatal",
                            "consumerTagStrategy",
                            "idleEventInterval",
                            "applicationEventPublisher");
                    }
                    ptContainer.afterPropertiesSet();
                    ptContainer.start();
                    LOGGER.info(
                        String.format("[RabbitMQ] shadow consumer create successfully. cacheKey: %s, ptQueueNames: %s",
                            cacheKey,
                            Arrays.toString(ptQueueNames)));
                    RUNNING_CONTAINER.put(cacheKey, ptContainer);
                }
            } else {
                final Object value = RUNNING_CONTAINER.get(cacheKey);
                if (value instanceof AbstractMessageListenerContainer) {
                    final AbstractMessageListenerContainer ptContainer = (AbstractMessageListenerContainer)value;
                    final List<String> ptQueueNameList = Arrays.asList(ptContainer.getQueueNames());

                    // Will not restart for a short time.
                    if (!ptContainer.isRunning()) {
                        final Date containerAtLastRestartDate = CONTAINER_AT_LAST_RESTART_DATE.get(cacheKey);
                        final Date now = new Date();
                        if (containerAtLastRestartDate == null || containerAtLastRestartDate.before(now)) {
                            final Date beforeDate = CONTAINER_AT_LAST_RESTART_DATE.put(cacheKey,
                                new Date(now.getTime() + 60 * 1000));
                            // in a concurrent state, make sure only restart once.
                            if (containerAtLastRestartDate == null || containerAtLastRestartDate.equals(beforeDate)) {
                                LOGGER.warn(
                                    String.format("[RabbitMQ] ptContainer restart. cacheKey: %s", cacheKey));
                                ptContainer.start();
                            }
                        }
                    } else {
                        // compare to last mqWhitelist,to add new ptQueueNames
                        final boolean change = !mqWhiteList.equals(LastMqWhiteListHolder.LAST_MQ_WHITELIST.get());
                        if (change && MQ_WHITELIST_CHANGED.compareAndSet(false, true)) {
                            final HashSet<String> needAddPtQueueSet = new HashSet<String>();
                            for (String queueName : queueNames) {
                                final String ptQueueName = Pradar.addClusterTestPrefix(queueName);
                                if ((mqWhiteList.contains(queueName + "#") || (mqWhiteList.contains("#" + queueName)))
                                    && !ptQueueNameList.contains(
                                    ptQueueName)) {
                                    needAddPtQueueSet.add(ptQueueName);
                                }
                            }
                            try {
                                if (!needAddPtQueueSet.isEmpty()) {
                                    ptContainer.addQueueNames(needAddPtQueueSet.toArray(new String[] {}));
                                }
                            } finally {
                                LastMqWhiteListHolder.LAST_MQ_WHITELIST.set(mqWhiteList);
                                MQ_WHITELIST_CHANGED.compareAndSet(true, false);
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            LOGGER.warn(String.format("[RabbitMQ] ptContainer start fail. cacheKey: %s", cacheKey), e);
            RUNNING_CONTAINER.remove(cacheKey);
        }

        Object[] args = advice.getParameterArray();
        if (ArrayUtils.isEmpty(args) || args.length != 2 || args[1] == null) {
            return;
        }
        Message message = (Message)args[1];
        if (message == null) {
            return;
        }

        validatePressureMeasurement(message);
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
            LOGGER.warn(String.format("[RabbitMQ] initSimpleMessageListenerContainer miss fileds:%s",
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
            LOGGER.warn(String.format("[RabbitMQ] initAbstractMessageListenerContainer miss fileds:%s",
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
            LOGGER.warn(String.format("[RabbitMQ] initCachingConnectionFactory miss fileds:%s",
                Arrays.toString(missFiledList.toArray())));
        }
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (ArrayUtils.isEmpty(args) || args.length != 2 || args[1] == null) {
            return null;
        }
        Message message = (Message)args[1];
        if (message == null) {
            return null;
        }
        Channel channel = (Channel)args[0];
        Connection connection = channel.getConnection();
        SpanRecord record = new SpanRecord();
        record.setRemoteIp(connection.getAddress().getHostAddress());
        record.setPort(connection.getPort() + "");
        record.setService(message.getMessageProperties().getConsumerQueue());
        record.setMethod(message.getMessageProperties().getReceivedExchange());
        byte[] body = message.getBody();
        record.setRequestSize(body.length);
        record.setRequest(body);
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        if (headers != null) {
            Map<String, String> rpcContext = new HashMap<String, String>();
            for (String key : Pradar.getInvokeContextTransformKeys()) {
                String value = ObjectUtils.toString(headers.get(key));
                if (!StringUtil.isEmpty(value)) {
                    rpcContext.put(key, value);
                }
            }
            record.setContext(rpcContext);
        }
        return record;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (ArrayUtils.isEmpty(args) || args.length != 2 || args[1] == null) {
            return null;
        }
        Message message = (Message)args[1];
        if (message == null) {
            return null;
        }

        SpanRecord record = new SpanRecord();
        return record;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (ArrayUtils.isEmpty(args) || args.length != 2 || args[1] == null) {
            return null;
        }
        Message message = (Message)args[1];
        if (message == null) {
            return null;
        }
        SpanRecord record = new SpanRecord();
        record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        record.setResponse(advice.getThrowable());
        return record;
    }

    private void validatePressureMeasurement(Message message) {
        try {
            Pradar.setClusterTest(false);
            String queue = message.getMessageProperties().getConsumerQueue();
            String exchange = message.getMessageProperties().getReceivedExchange();
            String routingKey = message.getMessageProperties().getReceivedRoutingKey();
            if (Pradar.isClusterTestPrefix(queue)) {
                Pradar.setClusterTest(true);
            } else if (Pradar.isClusterTestPrefix(exchange)) {
                Pradar.setClusterTest(true);
            } else if (Pradar.isClusterTestPrefix(routingKey)) {
                Pradar.setClusterTest(true);
            }
        } catch (Throwable e) {
            LOGGER.error("", e);
            if (Pradar.isClusterTest()) {
                throw new PressureMeasureError(e);
            }
        }
    }

    @Override
    public String getPluginName() {
        return RabbitmqConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return RabbitmqConstants.PLUGIN_TYPE;
    }
}
