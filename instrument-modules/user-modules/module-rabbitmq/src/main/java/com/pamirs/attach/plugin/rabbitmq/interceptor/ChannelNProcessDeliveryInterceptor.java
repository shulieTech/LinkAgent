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

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.pamirs.attach.plugin.rabbitmq.RabbitmqConstants;
import com.pamirs.attach.plugin.rabbitmq.common.ChannelHolder;
import com.pamirs.attach.plugin.rabbitmq.common.ConfigCache;
import com.pamirs.attach.plugin.rabbitmq.common.ConsumerMetaData;
import com.pamirs.attach.plugin.rabbitmq.destroy.RabbitmqDestroy;
import com.pamirs.attach.plugin.rabbitmq.utils.AdminAccessInfo;
import com.pamirs.attach.plugin.rabbitmq.utils.HttpUtils;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.exception.PradarException;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Command;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.impl.AMQConnection;
import com.rabbitmq.client.impl.AMQImpl.Basic;
import com.rabbitmq.client.impl.AMQImpl.Basic.Deliver;
import com.rabbitmq.client.impl.ChannelN;
import com.rabbitmq.client.impl.CredentialsProvider;
import com.rabbitmq.client.impl.recovery.AutorecoveringChannel;
import com.rabbitmq.client.impl.recovery.AutorecoveringConnection;
import com.rabbitmq.client.impl.recovery.RecordedConsumer;
import com.rabbitmq.client.impl.recovery.RecoveryAwareChannelN;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
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
    private static Logger logger = LoggerFactory.getLogger(ChannelNProcessDeliveryInterceptor.class.getName());

    private final SimulatorConfig simulatorConfig;

    public ChannelNProcessDeliveryInterceptor(SimulatorConfig simulatorConfig) {
        this.simulatorConfig = simulatorConfig;
    }

    private final static Cache<String, ShadowConsumeRunner> shadowConsumeRunners = CacheBuilder.newBuilder().build();

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
        Basic.Deliver method = (Deliver)args[1];
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
        String methodName = advice.getBehavior().getName();
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
        try {
            final ConsumerMetaData consumerMetaData = getConsumerMetaData((Channel)advice.getTarget(), m.getConsumerTag(),
                m.getExchange(), m.getRoutingKey());
            final String ptQueue = Pradar.addClusterTestPrefix(consumerMetaData.getQueue());
            shadowConsumeRunners.get(ptQueue, new Callable<ShadowConsumeRunner>() {
                @Override
                public ShadowConsumeRunner call() throws Exception {
                    return new ShadowConsumeRunner((Channel)advice.getTarget(), consumerMetaData);
                }
            }).start();
        } catch (Throwable e) {
            reporterError(e, m.getRoutingKey(), consumerTag);
        }
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
            try {
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
                if (channel instanceof AutorecoveringChannel) {
                    Connection connection = Reflect.on(channel).get("connection");
                    if (connection instanceof AutorecoveringConnection) {
                        Map<String, RecordedConsumer> consumers = Reflect.on(connection).get("consumers");
                        RecordedConsumer recordedConsumer = consumers.get(consumerTag);
                        consumerMetaData = new ConsumerMetaData(recordedConsumer, consumer);
                    }
                }
                if (channel instanceof ChannelN) {
                    logger.info("[rabbitmq] channel is ChannelN, will try to get queue name from rabbitmq admin!");
                    String queue = getQueueFromWebAdmin(channel, exchange, routingKey);
                    logger.info("[rabbitmq] channel is ChannelN, get queue name is {}", queue);
                    if (StringUtils.isEmpty(queue)) {
                        logger.warn(
                            "[rabbitmq] cannot find queueName, shadow consumer will subscribe routingKey instead!");
                        queue = routingKey;
                    }
                    consumerMetaData = new ConsumerMetaData(queue, consumerTag, consumer);
                }
            } catch (ReflectException e) {
                throw new PradarException("未支持的rabbitmq版本！无法获取订阅信息", e);
            }
            ConfigCache.putConsumerMetaData(key, consumerTag, consumerMetaData);
        }
        return consumerMetaData;
    }

    private String getQueueFromWebAdmin(Channel channel, String exchange, String routingKey) {
        try {
            Connection connection = Reflect.on(channel).get("_connection");
            if (connection instanceof AMQConnection) {
                AdminAccessInfo adminAccessInfo = resolveAdminAccessInfo(connection);
                if (!isDirectExchange(exchange, adminAccessInfo)) {
                    logger.warn("[RabbitMQ] exchange : {} is not a direct exchange（only support direct exchange)", exchange);
                    return null;
                }
                return resolveQueueByAdminResponse(exchange, adminAccessInfo, routingKey);
            }
        } catch (Throwable e) {
            logger.warn("get queue from web admin fail!", e);
        }
        return null;
    }

    private AdminAccessInfo resolveAdminAccessInfo(Connection connection) {
        String username = simulatorConfig.getProperty("rabbitmq.admin.username");
        String password = simulatorConfig.getProperty("rabbitmq.admin.password");
        if (username == null || password == null) {
            logger.warn(
                "[RabbitMQ] missing rabbitmq.admin username or password config, will use server username password "
                    + "instead");
            Object object = reflectSilence(connection, "credentialsProvider");
            if (object != null) {//低版本
                CredentialsProvider credentialsProvider = (CredentialsProvider)object;
                username = credentialsProvider.getUsername();
                password = credentialsProvider.getPassword();
            } else {
                username = reflectSilence(connection, "username");
                password = reflectSilence(connection, "password");
                if (username == null || password == null) {
                    throw new PradarException("未支持的rabbitmq版本！无法获取rabbit连接用户名密码");
                }
            }
        }
        InetAddress inetAddress = connection.getAddress();
        String virtualHost = Reflect.on(connection).get("_virtualHost");
        String host = simulatorConfig.getProperty("rabbitmq.admin.host");
        Integer port = simulatorConfig.getIntProperty("rabbitmq.admin.port");
        if (host == null) {
            host = inetAddress.getHostAddress();
            logger.warn("[RabbitMQ] missing rabbitmq.admin.host config, will use server host {} instead", host);
        }
        if (port == null) {
            port = Integer.parseInt("1" + connection.getPort());
            logger.warn("[RabbitMQ] missing rabbitmq.admin.port config, will use default port {} instead", port);
        }
        return new AdminAccessInfo(host, port, username, password, virtualHost);
    }

    private boolean isDirectExchange(String exchange, AdminAccessInfo adminAccessInfo) {
        String url = String.format("/api/exchanges/%s/%s", adminAccessInfo.getVirtualHostEncode(), exchange);
        String response = HttpUtils.doGet(adminAccessInfo, url).getResult();
        JSONObject jsonObject = JSON.parseObject(response);
        return "direct".equals(jsonObject.get("type"));
    }

    private String resolveQueueByAdminResponse(String exchange, AdminAccessInfo adminAccessInfo, String routingKey) {
        String url = String.format("/api/exchanges/%s/%s/bindings/source", adminAccessInfo.getVirtualHostEncode(), exchange);
        String response = HttpUtils.doGet(adminAccessInfo, url).getResult();
        JSONArray jsonArray = JSON.parseArray(response);
        for (Object o : jsonArray) {
            JSONObject jsonObject = (JSONObject)o;
            String configRoutingKey = jsonObject.getString("routing_key");
            if (routingKey.equals(configRoutingKey)) {
                return jsonObject.getString("destination");
            }
        }
        return null;
    }

    private static class ShadowConsumeRunner implements Runnable {

        private final Channel channel;
        private final ConsumerMetaData consumerMetaData;
        private final Thread thread;
        private final String ptConsumerTag;
        private final String ptQueue;
        private final AtomicBoolean flag = new AtomicBoolean(false);

        public ShadowConsumeRunner(Channel channel, ConsumerMetaData consumerMetaData) {
            this.channel = channel;
            this.consumerMetaData = consumerMetaData;
            this.ptConsumerTag = Pradar.addClusterTestPrefix(consumerMetaData.getConsumerTag());
            this.ptQueue = Pradar.addClusterTestPrefix(consumerMetaData.getQueue());
            thread = new Thread(this,
                String.format("ShadowConsumeRunner for %s-%s", ptQueue, ptConsumerTag));
        }

        public void start() {
            if (flag.compareAndSet(false, true)) {
                thread.start();
            }
        }

        @Override
        public void run() {
            String consumerTag = consumerMetaData.getConsumerTag();
            Consumer consumer = consumerMetaData.getConsumer();
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug(
                        "RabbitMQ basicConsume(ptQueue:{},autoAck:{},consumerTag:{},noLocal:{},exclusive:{},"
                            + "arguments:{},"
                            + "ptConsumer:{})",
                        ptQueue, true, ptConsumerTag, false, false, null, consumer);
                }
                String cTag = ChannelHolder.consumeShadowQueue(channel, ptQueue, true, ptConsumerTag, false,
                    consumerMetaData.isExclusive(), consumerMetaData.getArguments(), consumer);
                if (cTag != null) {
                    ChannelHolder.addConsumerTag(channel, consumerTag, cTag, ptQueue);
                } else {
                    reporterError(null, this.ptQueue, this.ptConsumerTag, "get shadow channel is null or closed.");
                }
            } catch (Throwable e) {
                reporterError(e, this.ptQueue, this.ptConsumerTag);
            }
        }
    }

    private static void reporterError(Throwable e, String queue, String consumerTag) {
        reporterError(e, queue, consumerTag, e.getMessage());
    }

    private static void reporterError(Throwable e, String queue, String consumerTag, String cases) {
        ErrorReporter.buildError()
            .setErrorType(ErrorTypeEnum.MQ)
            .setErrorCode("MQ-0001")
            .setMessage("RabbitMQ消费端订阅队列失败！")
            .setDetail("RabbitMqPushConsumerInterceptor:queue:[" + queue + "]," + cases)
            .report();
        logger.error("RabbitMQ PT Consumer Inject failed queue:[{}] consumerTag:{}, {}", queue, consumerTag, cases, e);
    }

    private static <T> T reflectSilence(Object target, String name) {
        try {
            return Reflect.on(target).get(name);
        } catch (ReflectException e) {
            logger.warn("can not find field '{}' from : '{}'", name, target.getClass().getName());
            return null;
        }
    }
}
