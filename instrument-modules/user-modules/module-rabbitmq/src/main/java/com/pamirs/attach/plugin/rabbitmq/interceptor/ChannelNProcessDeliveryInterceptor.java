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

import com.pamirs.attach.plugin.rabbitmq.RabbitmqConstants;
import com.pamirs.attach.plugin.rabbitmq.common.*;
import com.pamirs.attach.plugin.rabbitmq.destroy.RabbitmqDestroy;
import com.pamirs.attach.plugin.rabbitmq.destroy.ShadowConsumerDisableListenerImpl;
import com.pamirs.pradar.*;
import com.pamirs.pradar.exception.PradarException;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.rabbitmq.client.*;
import com.rabbitmq.client.impl.AMQImpl.Basic;
import com.rabbitmq.client.impl.AMQImpl.Basic.Deliver;
import com.rabbitmq.client.impl.ChannelN;
import com.rabbitmq.client.impl.recovery.AutorecoveringChannel;
import com.rabbitmq.client.impl.recovery.AutorecoveringConnection;
import com.rabbitmq.client.impl.recovery.RecordedConsumer;
import com.rabbitmq.client.impl.recovery.RecoveryAwareChannelN;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import com.shulie.instrument.simulator.api.resource.SimulatorConfig;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: mubai<chengjiacai @ shulie.io>
 * @Date: 2020-03-09 17:44
 * @Description:
 */
@Destroyable(RabbitmqDestroy.class)
public class ChannelNProcessDeliveryInterceptor extends TraceInterceptorAdaptor {
    private Logger logger = LoggerFactory.getLogger(ChannelNProcessDeliveryInterceptor.class.getName());
    private boolean isInfoEnabled = logger.isInfoEnabled();

    private final SimulatorConfig simulatorConfig;

    @Resource
    private DynamicFieldManager dynamicFieldManager;

    public ChannelNProcessDeliveryInterceptor(SimulatorConfig simulatorConfig) {
        this.simulatorConfig = simulatorConfig;
    }

    /**
     * 是否是调用端
     *
     * @return
     */
    @Override
    public boolean isClient(Advice advice) {
        return false;
    }

    @Override
    public String getPluginName() {
        return RabbitmqConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return RabbitmqConstants.PLUGIN_TYPE;
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        registerListener();
        Object[] args = advice.getParameterArray();
        Command command = (Command) args[0];
        Basic.Deliver method = (Deliver) args[1];
        SpanRecord record = new SpanRecord();
        record.setService(method.getExchange());
        String routingKey = method.getRoutingKey();
        String queue = method.getRoutingKey();
        record.setMethod(routingKey + "@" + queue);
        BasicProperties contentHeader = (BasicProperties) command.getContentHeader();
        Map<String, Object> headers = contentHeader.getHeaders();
        if (headers != null) {
            Map<String, String> rpcContext = new HashMap<String, String>();
            for (String key : Pradar.getInvokeContextTransformKeys()) {
                Object value = headers.get(key);
                if (value != null) {
                    rpcContext.put(key, value.toString());
                }
            }
            record.setContext(rpcContext);
        }
        byte[] body = command.getContentBody();
        record.setRequestSize(body.length);
        record.setRequest(body);
        Channel channel = (Channel) advice.getTarget();
        Connection connection = channel.getConnection();
        record.setRemoteIp(connection.getAddress().getHostAddress());
        record.setPort(connection.getPort() + "");
        return record;
    }


    static AtomicBoolean registered = new AtomicBoolean(false);

    private void registerListener() {
        if (registered.get()) {
            return;
        }
        final ShadowConsumerDisableListenerImpl shadowConsumerDisableListener = new ShadowConsumerDisableListenerImpl();
        EventRouter.router().addListener(shadowConsumerDisableListener);
        registered.set(true);

    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        SpanRecord record = new SpanRecord();
        record.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
        return record;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        SpanRecord record = new SpanRecord();
        record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        record.setResponse(advice.getThrowable());
        return record;
    }

    @Override
    public void beforeFirst(Advice advice) {
        Object[] args = advice.getParameterArray();
        String methodName = advice.getBehaviorName();
        if (!PradarSwitcher.isClusterTestEnabled()) {
            logger.warn("PradarSwitcher isClusterTestEnabled false, {} to start shadow {} skip it",
                    advice.getTargetClass().getName(), methodName);
            return;
        }
        AMQP.Basic.Deliver m = (AMQP.Basic.Deliver) args[1];
        validatePressureMeasurement(m.getConsumerTag());
        try {
            Command command = (Command) args[0];
            BasicProperties contentHeader = (BasicProperties) command.getContentHeader();
            Map<String, Object> headers = contentHeader.getHeaders();
            if (null != headers && headers.get(PradarService.PRADAR_CLUSTER_TEST_KEY) != null && ClusterTestUtils
                    .isClusterTestRequest(headers.get(PradarService.PRADAR_CLUSTER_TEST_KEY).toString())) {
                Pradar.setClusterTest(true);
            }
            if (!Pradar.isClusterTest()) {
                String routingKey = m.getRoutingKey();
                if (StringUtils.isNotBlank(routingKey) && ClusterTestUtils.isClusterTestRequest(routingKey)) {
                    Pradar.setClusterTest(true);
                }
                String exchange = m.getExchange();
                if (StringUtils.isNotBlank(exchange) && ClusterTestUtils.isClusterTestRequest(exchange)) {
                    Pradar.setClusterTest(true);
                }
            }
        } catch (Throwable e) {
            if (Pradar.isClusterTest()) {
                throw new PressureMeasureError(e);
            }
        }
    }

    @Override
    public void afterLast(final Advice advice) {
        if (ConfigCache.isWorkWithSpring()) {
            return;
        }
        Object[] args = advice.getParameterArray();
        AMQP.Basic.Deliver m = (AMQP.Basic.Deliver) args[1];

        String consumerTag = m.getConsumerTag();
        if (Pradar.isClusterTestPrefix(consumerTag) || ChannelHolder.existsConsumer(consumerTag)) {
            return;
        }
        String routingKey = m.getRoutingKey();
        String exchange = m.getExchange();
        try {
            Channel channel = (Channel) advice.getTarget();
            ConsumerMetaData consumerMetaData = getConsumerMetaData(channel, consumerTag,
                    exchange, routingKey);

            String queue = consumerMetaData.getQueue();
            if (queue == null || consumerMetaData.isRoutingKeyExchangeModel()) {
                consumerMetaData.setRoutingKeyExchangeModel(true);
                for (String s : GlobalConfig.getInstance().getMqWhiteList()) {
                    if (s.startsWith(exchange + "#" + routingKey)) {
                        int index = s.lastIndexOf("@");
                        if (index < 0 || index == s.length() - 1) {
                            LOGGER.error("[RabbitMQ] rabbit mq config wrong {}, example : {}", s,
                                    "exchange#routingkey@queue");
                            continue;
                        }
                        queue = s.substring(index + 1);
                        consumerMetaData.setQueue(queue);
                        break;
                    }
                }
                if (queue == null) {
                    LOGGER.warn(
                            "[RabbitMQ] SIMULATOR: rabbitmq exchange : {}  routingKey : {} is not in whitelist. ignore it",
                            exchange, routingKey);
                    return;
                }
            } else {
                if (PradarSwitcher.whiteListSwitchOn() && !GlobalConfig.getInstance().getMqWhiteList().contains("#" + queue)) {
                    LOGGER.warn("[RabbitMQ] SIMULATOR: rabbitmq queue : {} is not in whitelist. ignore it", "#" + queue);
                    return;
                }
            }

            String ptQueue = consumerMetaData.getPtQueue();
            String ptConsumerTag = consumerMetaData.getPtConsumerTag();
            if (isInfoEnabled) {
                logger.info("[RabbitMQ] prepare create shadow consumer, queue : {} pt_queue : {} tag : {} pt_tag : {}",
                        queue, ptQueue, consumerTag, ptConsumerTag);
            }
            try {
                ConsumeResult consumeResult = ChannelHolder.consumeShadowQueue(channel, consumerMetaData);
                String cTag = consumeResult.getTag();
                dynamicFieldManager.setDynamicField(consumeResult.getShadowChannel(), RabbitmqConstants.IS_AUTO_ACK_FIELD, consumerMetaData.isAutoAck());
                if (cTag != null) {
                    if (isInfoEnabled) {
                        logger.info(
                                "[RabbitMQ] create shadow consumer successful! queue : {} pt_queue : {} tag : {} pt_tag : {}",
                                queue, ptQueue, consumerTag, ptConsumerTag);
                    }
                    ChannelHolder.addConsumerTag(channel, consumerTag, cTag, ptQueue);
                } else {
                    reporterError(null, ptQueue, ptConsumerTag, "get shadow channel is null or closed.");
                }
            } catch (Throwable e) {
                reporterError(e, ptQueue, ptConsumerTag);
            }
        } catch (Throwable e) {
            reporterError(e, routingKey, consumerTag);
        }
    }

    @Override
    public void exceptionLast(Advice advice) {
    }

    private static Consumer getConsumerFromChannel(Object channel, String consumerTag) {
        Map<String, Consumer> _consumers = Reflect.on(channel).get("_consumers");
        return _consumers.get(consumerTag);
    }

    private void validatePressureMeasurement(String consumerTag) {
        try {
            Pradar.setClusterTest(false);
            consumerTag = StringUtils.trimToEmpty(consumerTag);
            if (Pradar.isClusterTestPrefix(consumerTag)) {
                Pradar.setClusterTest(true);
            }
        } catch (Throwable e) {
            logger.error("rabbitmq validate pressure request err!", e);
            if (Pradar.isClusterTest()) {
                throw new PressureMeasureError(e);
            }
        }
    }

    private ConsumerMetaData getConsumerMetaData(Channel channel, String consumerTag, String exchange,
                                                 String routingKey) {
        final int key = System.identityHashCode(channel);
        Consumer consumer = getConsumerFromChannel(channel, consumerTag);
        ConsumerMetaData consumerMetaData = ConfigCache.getConsumerMetaData(key, consumerTag);
        if (consumerMetaData == null) {
            synchronized (ChannelNProcessDeliveryInterceptor.class) {
                consumerMetaData = ConfigCache.getConsumerMetaData(key, consumerTag);
                if (consumerMetaData == null) {
                    consumerMetaData = buildConsumerMetaData(channel, consumerTag, exchange, routingKey, consumer);
                    ConfigCache.putConsumerMetaData(key, consumerTag, consumerMetaData);
                }
            }
        }
        return consumerMetaData;
    }

    private ConsumerMetaData buildConsumerMetaData(Channel channel, String consumerTag, String exchange, String routingKey,
                                                   Consumer consumer) {
        ConsumerMetaData consumerMetaData;
        String queue = getQueue(channel, consumerTag, exchange, routingKey, consumer);
        consumerMetaData = new ConsumerMetaData(queue, consumerTag, new ExceptionSilenceConsumer(consumer),
                simulatorConfig.getBooleanProperty(RabbitmqConstants.EXCLUSIVE_CONFIG, false),
                simulatorConfig.getBooleanProperty(RabbitmqConstants.AUTO_ACK_CONFIG, true),
                simulatorConfig.getIntProperty(RabbitmqConstants.PREFETCH_COUNT_CONFIG, 0),
                simulatorConfig.getBooleanProperty(RabbitmqConstants.NO_LOCAL_CONFIG, false));
        return consumerMetaData;
    }

    private String getQueue(Channel channel, String consumerTag, String exchange, String routingKey, Consumer consumer) {
        if (exchange == null || "".equals(exchange)) {
            if (isInfoEnabled) {
                logger.info("[RabbitMQ] using default exchange so use routingKey as queue, it is : {} ", routingKey);
            }
            return routingKey;
        }
        String queue = null;
        try {
            channel = unWrapChannel(channel, consumerTag, consumer);
            if (channel instanceof AutorecoveringChannel) {
                queue = getQueueWithRecoveringChannel(channel, consumerTag, queue);
            }
            if (channel instanceof ChannelN) {
                if (isInfoEnabled) {
                    logger.info("[RabbitMQ] channel is ChannelN, can not auto detect queue name!");
                }
                return null;
            }
        } catch (ReflectException e) {
            throw new PradarException("[RabbitMQ] 未支持的rabbitmq版本！无法获取订阅信息", e);
        }
        return queue;
    }

    private String getQueueWithRecoveringChannel(Channel channel, String consumerTag, String queue) {
        Connection connection = Reflect.on(channel).get("connection");
        if (connection instanceof AutorecoveringConnection) {
            Map<String, RecordedConsumer> consumers = Reflect.on(connection).get("consumers");
            queue = consumers.get(consumerTag).getQueue();
        }
        return queue;
    }

    private Channel unWrapChannel(Channel channel, String consumerTag, Consumer consumer) {
        if (channel instanceof RecoveryAwareChannelN) {
            if (consumer instanceof DefaultConsumer) {
                channel = Reflect.on(consumer).get("_channel");
            } else {
                Map<String, Consumer> consumers = Reflect.on(channel).get("_consumers");
                Consumer consumerFromRecovery = consumers.get(consumerTag);
                if (consumerFromRecovery.getClass().getName().contains("AutorecoveringChannel")) {
                    channel = Reflect.on(consumerFromRecovery).get("this$0");
                }
            }
        }
        return channel;
    }

    private void reporterError(Throwable e, String queue, String consumerTag) {
        reporterError(e, queue, consumerTag, e.getMessage());
    }

    private void reporterError(Throwable e, String queue, String consumerTag, String cases) {
        ErrorReporter.buildError()
                .setErrorType(ErrorTypeEnum.MQ)
                .setErrorCode("MQ-0001")
                .setMessage("RabbitMQ消费端订阅队列失败！")
                .setDetail("RabbitMqPushConsumerInterceptor:queue:[" + queue + "]," + cases)
                .report();
        logger.error("[RabbitMQ] PT Consumer Inject failed queue:[{}] consumerTag:{}, {}", queue, consumerTag, cases, e);
    }

}
