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
import com.pamirs.attach.plugin.rabbitmq.common.ChannelHolder;
import com.pamirs.attach.plugin.rabbitmq.common.ConfigCache;
import com.pamirs.attach.plugin.rabbitmq.common.ConsumerDetail;
import com.pamirs.attach.plugin.rabbitmq.common.ShadowConsumerProxy;
import com.pamirs.attach.plugin.rabbitmq.consumer.*;
import com.pamirs.attach.plugin.rabbitmq.consumer.admin.support.cache.CacheSupportFactory;
import com.pamirs.attach.plugin.rabbitmq.destroy.RabbitmqDestroy;
import com.pamirs.pradar.*;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.rabbitmq.client.*;
import com.rabbitmq.client.impl.AMQImpl.Basic.Deliver;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.SimulatorConfig;
import com.shulie.instrument.simulator.api.util.StringUtil;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    private final List<ConsumerMetaDataBuilder> consumerMetaDataBuilders = new ArrayList<ConsumerMetaDataBuilder>();

    private final static ScheduledThreadPoolExecutor THREAD_POOL_EXECUTOR = new ScheduledThreadPoolExecutor(
        Runtime.getRuntime()
            .availableProcessors(), new ThreadFactory() {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "shadow-consumer-register-" + threadNumber.getAndIncrement());
            if (t.isDaemon()) {t.setDaemon(false);}
            if (t.getPriority() != Thread.NORM_PRIORITY) {t.setPriority(Thread.NORM_PRIORITY);}
            return t;
        }
    });

    public ChannelNProcessDeliveryInterceptor(SimulatorConfig simulatorConfig) throws Exception {
        this.simulatorConfig = simulatorConfig;
        consumerMetaDataBuilders.add(SpringConsumerMetaDataBuilder.getInstance());
        consumerMetaDataBuilders.add(SpringConsumerDecoratorMetaDataBuilder.getInstance());
        consumerMetaDataBuilders.add(AutorecoveringChannelConsumerMetaDataBuilder.getInstance());
        consumerMetaDataBuilders.add(new AdminApiConsumerMetaDataBuilder(simulatorConfig,
            CacheSupportFactory.create(simulatorConfig)));
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
        if (ConfigCache.isWorkWithSpring()) {
            return null;
        }
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
                String value = ObjectUtils.toString(headers.get(key));
                if (!StringUtil.isEmpty(value)) {
                    rpcContext.put(key, value);
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
        if (ConfigCache.isWorkWithSpring()) {
            return null;
        }
        SpanRecord record = new SpanRecord();
        record.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
        return record;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        if (ConfigCache.isWorkWithSpring()) {
            return null;
        }
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

    private boolean isLocalHost(String ip) {
        return "localhost".equals(ip) || "127.0.0.1".equals(ip);
    }

    private class ShadowConsumerRegisterRunnable implements Runnable {

        private final ConsumerDetail consumerDetail;

        private final int retryTimes;

        private ShadowConsumerRegisterRunnable(ConsumerDetail consumerDetail, int retryTimes) {
            this.consumerDetail = consumerDetail;
            this.retryTimes = retryTimes;
        }

        private ShadowConsumerRegisterRunnable(ConsumerDetail consumerDetail) {
            this(consumerDetail, 0);
        }

        @Override
        public void run() {
            logger.info("[RabbitMQ] SIMULATOR prepare create shadow consumer {} {} current retry times : {}",
                consumerDetail.getChannel(), consumerDetail.getConsumerTag(), retryTimes);
            String consumerTag = consumerDetail.getConsumerTag();
            Channel channel = consumerDetail.getChannel();
            try {
                ConsumerMetaData consumerMetaData = getConsumerMetaData(consumerDetail);
                if(consumerMetaData.isUseSpring()){
                    return;
                }
                if (consumerMetaData == null) {
                    logger.warn("[RabbitMQ] SIMULATOR: can not find consumerMetaData for channel : {}, consumerTag : {}"
                        , consumerDetail.getChannel(), consumerDetail.getConsumerTag());
                    retry();
                    return;
                }

                String queue = consumerMetaData.getQueue();
                if (!GlobalConfig.getInstance().getMqWhiteList().contains(queue + "#")
                    && !GlobalConfig.getInstance().getMqWhiteList().contains("#" + queue)) {
//                    logger.warn("[RabbitMQ] SIMULATOR: {} is not in whitelist. ignore it", queue);
                    //todo need retry？
                    retry();
                    return;
                }
                String ptQueue = consumerMetaData.getPtQueue();
                String ptConsumerTag = consumerMetaData.getPtConsumerTag();
                logger.info("[RabbitMQ] prepare create shadow consumer, queue : {} pt_queue : {} tag : {} pt_tag : {}",
                    queue, ptQueue, consumerTag, ptConsumerTag);
                try {
                    String cTag;
                    if (consumerMetaData.isUseOriginChannel()) {
                        //spring 要用业务本身的channel去订阅
                        cTag = channel.basicConsume(consumerMetaData.getPtQueue(), consumerMetaData.isAutoAck(),
                            ptConsumerTag,
                            false, consumerMetaData.isExclusive(),
                            new HashMap<String, Object>(), new ShadowConsumerProxy(consumerMetaData.getConsumer()));
                    } else {
                        cTag = consumeShadowQueue(channel, consumerMetaData);
                    }
                    if (cTag != null) {
                        if (isInfoEnabled) {
                            logger.info(
                                "[RabbitMQ] create shadow consumer successful! queue : {} pt_queue : {} tag : {} pt_tag : "
                                    + "{}",
                                queue, ptQueue, consumerTag, ptConsumerTag);
                        }
                        ChannelHolder.addConsumerTag(channel, consumerTag, cTag, ptQueue);
                    } else {
                        reporterError(null, ptQueue, ptConsumerTag, "get shadow channel is null or closed.");
                        retry();
                    }
                } catch (Throwable e) {
                    reporterError(e, ptQueue, ptConsumerTag);
                    retry();
                }
            } catch (Throwable e) {
                reporterError(e, null, consumerTag);
                retry();
            }
        }

        public String consumeShadowQueue(Channel target, ConsumerMetaData consumerMetaData) throws
            IOException {
            return consumeShadowQueue(target, consumerMetaData.getPtQueue(), consumerMetaData.isAutoAck(),
                consumerMetaData.getPtConsumerTag(), false, consumerMetaData.isExclusive(),
                consumerMetaData.getArguments(), consumerMetaData.getPrefetchCount(),
                new ShadowConsumerProxy(consumerMetaData.getConsumer()));
        }

        public String consumeShadowQueue(Channel target, String ptQueue, boolean autoAck,
            String ptConsumerTag,
            boolean noLocal, boolean exclusive, Map<String, Object> arguments, int prefetchCount,
            Consumer consumer) throws IOException {
            synchronized (ChannelHolder.class) {
                Channel shadowChannel = ChannelHolder.getOrShadowChannel(target);
                if (shadowChannel == null) {
                    logger.warn(
                        "[RabbitMQ] basicConsume failed. cause by shadow channel is not found. queue={}, consumerTag={}",
                        ptQueue, ptConsumerTag);
                    return null;
                }
                if (!shadowChannel.isOpen()) {
                    logger.warn(
                        "[RabbitMQ] basicConsume failed. cause by shadow channel is not closed. queue={}, consumerTag={}",
                        ptQueue, ptConsumerTag);
                    return null;
                }
                if (prefetchCount > 0) {
                    shadowChannel.basicQos(prefetchCount);
                }
                String result = shadowChannel.basicConsume(ptQueue, autoAck, ptConsumerTag, noLocal, exclusive, arguments,
                    consumer);
                final int key = System.identityHashCode(shadowChannel);
                ConfigCache.putQueue(key, ptQueue);
                return result;
            }
        }

        private void retry() {
            int nexRetryTimes = this.retryTimes + 1;
            if (nexRetryTimes >= 10) {
                logger.error("consumerDetail : {} reach max retry time, give up retry!", consumerDetail);
                return;
            }
            long time = (nexRetryTimes) * 60L;
            long maxTime = 60 * 5L;
            time = Math.min(time, maxTime);
            THREAD_POOL_EXECUTOR.schedule(
                new ShadowConsumerRegisterRunnable(consumerDetail, nexRetryTimes), time, TimeUnit.SECONDS);
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

    private ConsumerMetaData getConsumerMetaData(ConsumerDetail deliverDetail) throws Exception {
        Channel channel = deliverDetail.getChannel();
        String consumerTag = deliverDetail.getConsumerTag();
        final int key = System.identityHashCode(channel);
        ConsumerMetaData consumerMetaData = ConfigCache.getConsumerMetaData(key, consumerTag);
        if (consumerMetaData == null) {
            synchronized (ChannelNProcessDeliveryInterceptor.class) {
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

    private ConsumerMetaData buildConsumerMetaData(ConsumerDetail deliverDetail) throws Exception {
        for (ConsumerMetaDataBuilder consumerMetaDataBuilder : consumerMetaDataBuilders) {
            ConsumerMetaData consumerMetaData = consumerMetaDataBuilder.tryBuild(deliverDetail);
            if (consumerMetaData != null) {
                return consumerMetaData;
            }
        }
        return null;
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

    @Override
    protected void clean() {
        THREAD_POOL_EXECUTOR.shutdownNow();
    }
}
