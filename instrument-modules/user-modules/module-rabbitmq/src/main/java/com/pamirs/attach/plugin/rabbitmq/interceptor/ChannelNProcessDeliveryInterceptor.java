package com.pamirs.attach.plugin.rabbitmq.interceptor;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import com.pamirs.attach.plugin.rabbitmq.RabbitmqConstants;
import com.pamirs.attach.plugin.rabbitmq.common.ChannelHolder;
import com.pamirs.attach.plugin.rabbitmq.common.ConfigCache;
import com.pamirs.attach.plugin.rabbitmq.common.ConsumeResult;
import com.pamirs.attach.plugin.rabbitmq.common.DeliverDetail;
import com.pamirs.attach.plugin.rabbitmq.consumer.AdminApiConsumerMetaDataBuilder;
import com.pamirs.attach.plugin.rabbitmq.consumer.AutorecoveringChannelConsumerMetaDataBuilder;
import com.pamirs.attach.plugin.rabbitmq.consumer.ConsumerMetaData;
import com.pamirs.attach.plugin.rabbitmq.destroy.RabbitmqDestroy;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Command;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.impl.AMQImpl.Basic.Deliver;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import com.shulie.instrument.simulator.api.resource.SimulatorConfig;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        Object[] args = advice.getParameterArray();
        Command command = (Command)args[0];
        Deliver method = (Deliver)args[1];
        SpanRecord record = new SpanRecord();
        record.setService(method.getExchange());
        record.setMethod(method.getRoutingKey());
        BasicProperties contentHeader = (BasicProperties)command.getContentHeader();
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
        Channel channel = (Channel)advice.getTarget();
        Connection connection = channel.getConnection();
        record.setRemoteIp(connection.getAddress().getHostAddress());
        record.setPort(connection.getPort() + "");
        return record;
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
        AMQP.Basic.Deliver m = (AMQP.Basic.Deliver)args[1];
        validatePressureMeasurement(m.getConsumerTag());
        try {
            Command command = (Command)args[0];
            BasicProperties contentHeader = (BasicProperties)command.getContentHeader();
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
        AMQP.Basic.Deliver m = (AMQP.Basic.Deliver)args[1];

        String consumerTag = m.getConsumerTag();
        if (Pradar.isClusterTestPrefix(consumerTag) || ChannelHolder.existsConsumer(consumerTag)) {
            return;
        }
        String routingKey = m.getRoutingKey();
        String exchange = m.getExchange();
        Channel channel = (Channel)advice.getTarget();
        DeliverDetail deliverDetail = new DeliverDetail(consumerTag, exchange, routingKey, channel);
        try {
            ConsumerMetaData consumerMetaData = getConsumerMetaData(deliverDetail);
            if (consumerMetaData == null) {
                LOGGER.error("[RabbitMQ] SIMULATOR: can not build consumerMetaData!");
                return;
            }

            String queue = consumerMetaData.getQueue();
            if (!GlobalConfig.getInstance().getMqWhiteList().contains("#" + queue)) {
                LOGGER.warn("[RabbitMQ] SIMULATOR: {} is not in whitelist. ignore it", queue);
                return;
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
                dynamicFieldManager.setDynamicField(consumeResult.getShadowChannel(), RabbitmqConstants.IS_AUTO_ACK_FIELD,
                    consumerMetaData.isAutoAck());
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

    private ConsumerMetaData getConsumerMetaData(DeliverDetail deliverDetail) {
        Channel channel = deliverDetail.getChannel();
        String consumerTag = deliverDetail.getConsumerTag();
        final int key = System.identityHashCode(channel);
        ConsumerMetaData consumerMetaData = ConfigCache.getConsumerMetaData(key, consumerTag);
        if (consumerMetaData == null) {
            synchronized (ChannelNProcessDeliveryInterceptor.class) {
                consumerMetaData = ConfigCache.getConsumerMetaData(key, consumerTag);
                if (consumerMetaData == null) {
                    consumerMetaData = buildConsumerMetaData(deliverDetail);
                    ConfigCache.putConsumerMetaData(key, consumerTag, consumerMetaData);
                }
            }
        }
        return consumerMetaData;
    }

    private ConsumerMetaData buildConsumerMetaData(DeliverDetail deliverDetail) {
        ConsumerMetaData consumerMetaData = AutorecoveringChannelConsumerMetaDataBuilder.getInstance().tryBuild(deliverDetail);
        return consumerMetaData != null ? consumerMetaData : new AdminApiConsumerMetaDataBuilder(simulatorConfig, null).tryBuild(
            deliverDetail);
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
