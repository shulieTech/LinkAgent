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
package com.pamirs.attach.plugin.apache.kafka.stream.interceptor;

import com.pamirs.attach.plugin.apache.kafka.stream.common.KStreamProcessorProcessTypeEnum;
import com.pamirs.attach.plugin.apache.kafka.stream.constants.KafkaStreamsCaches;
import com.pamirs.attach.plugin.apache.kafka.stream.obj.KafkaStreamConfig;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.common.BytesUtils;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.ForeachAction;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.processor.internals.ProcessorContextImpl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author angju
 * @date 2021/5/8 15:36
 */
public abstract class AbstractKStreamProcessorProcessInterceptor extends TraceInterceptorAdaptor {
    private Field contextField;
    private Field kstreamMapField;
    private Field mapperField;

    private Boolean isHighLevel;

    /**
     * 操纵字段名称
     *
     * @return
     */
    protected abstract KStreamProcessorProcessTypeEnum getActionFieldName();


    @Override
    public String getPluginName() {
        return "apache-kafka-stream";
    }

    @Override
    public int getPluginType() {
        return MiddlewareType.TYPE_MQ;
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
    public void beforeFirst(Advice advice) {
        Object target = advice.getTarget();
        KafkaStreamConfig config = new KafkaStreamConfig();
        advice.attach(config);
        initField(target, config);
    }

    @Override
    public void afterFirst(Advice advice) {
        KafkaStreamConfig config = advice.attachment();
        registerShadowConsumerStream(config);
    }

    @Override
    public void afterLast(Advice advice) {
        advice.attach(null);
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        if (contextField == null) {
            try {
                contextField = advice.getTarget().getClass().getSuperclass().getDeclaredField("context");
            } catch (NoSuchFieldException e) {

            }
        }
        if (contextField == null) {
            return null;
        }
        ProcessorContextImpl processorContext = null;
        try {
            processorContext = (ProcessorContextImpl) contextField.get(advice.getTarget());
        } catch (IllegalAccessException e) {
        }
        if (processorContext == null) {
            return null;
        }

        KafkaStreamConfig config = advice.attachment();
        if (config == null) {
            return null;
        }

        String group = config.getApplicationId();

        String remoteAddress = null;
        Properties ptProperties = config.getProperties();
        if (ptProperties != null) {
            remoteAddress = ptProperties.getProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG);
        }


        SpanRecord spanRecord = new SpanRecord();
        if (PradarSwitcher.isKafkaMessageHeadersEnabled()) {
            if (isHighLevel()) {
                Headers headers = processorContext.recordContext().headers();
                if (headers != null) {
                    Map<String, String> ctx = new HashMap<String, String>(12);
                    Header[] headerArray = headers.toArray();
                    List<String> keys = Pradar.getInvokeContextTransformKeys();
                    for (Header header : headerArray) {
                        if (keys.contains(header.key())) {
                            ctx.put(header.key(), BytesUtils.toString(header.value()));
                        }
                    }
                    spanRecord.setContext(ctx);
                }
            }

        }
        /* key subject */
        StringBuilder message = new StringBuilder();
        if (advice.getParameterArray()[0] != null) {
            message.append(advice.getParameterArray()[0]).append(":");
        }
        if (advice.getParameterArray()[1] != null) {
            message.append(advice.getParameterArray()[1]);
        }
        spanRecord.setRequest(message.toString());

        spanRecord.setService(processorContext.topic());
        spanRecord.setMethod(group == null ? "" : group);
        spanRecord.setRemoteIp(remoteAddress);
        spanRecord.setClusterTest(Pradar.isClusterTestPrefix(config.getTopic()));
        try {
            long record = processorContext.timestamp();
            spanRecord.setCallbackMsg(System.currentTimeMillis() - record + "");
        } catch (Throwable t) {
            //ignore
        }
        return spanRecord;
    }


    @Override
    public SpanRecord afterTrace(Advice advice) {
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setResponse(advice.getReturnObj());
        return spanRecord;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setResponse(advice.getThrowable());
        return spanRecord;
    }


    private void registerShadowConsumerStream(KafkaStreamConfig config) {
        if (config == null) {
            LOGGER.warn("register kafka stream config is null...");
            return;
        }
        String topic = config.getTopic();
        String applicationId = config.getApplicationId();
        Object mapper = config.getMapper();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("register topic is {}, applicationId is {}", topic, applicationId);
        }
        if (mapper == null || StringUtils.isBlank(topic) || Pradar.isClusterTestPrefix(topic) || KafkaStreamsCaches.contains(applicationId)) {
            return;
        }
        synchronized (this) {
            if (KafkaStreamsCaches.contains(applicationId)) {
                return;
            }
            switch (getActionFieldName()) {
                case MAP:
                    LOGGER.warn("register1 topic is {}, applicationId is {}", topic, applicationId);
                    initShadowMapOpt(config);
                    break;
                case FOREACH:
                    LOGGER.warn("register2 topic is {}, applicationId is {}", topic, applicationId);
                    initShadowForeachOpt(config);
                    break;
                default:
                    throw new PressureMeasureError("not support kafka stream api " + getActionFieldName());
            }

        }
    }


    private boolean isHighLevel() {
        /**
         * 加一下检测的缓存
         */
        if (this.isHighLevel == null) {
            boolean highLevel = false;
            try {
                //0.11.0.0有的类
                Thread.currentThread().getContextClassLoader().loadClass("org.apache.kafka.streams.kstream.KStreamBuilder");
            } catch (ClassNotFoundException e) {
                highLevel = true;
            }
            this.isHighLevel = highLevel;
        }

        return isHighLevel;
    }

    private void initShadowForeachOpt(KafkaStreamConfig config) {
        if (isHighLevel()) {
            initShadowForeachOpt_2_0_0(config);
        } else {
            initShadowForeachOpt_0_11_0_0(config);
        }
    }

    private void initShadowMapOpt(KafkaStreamConfig config) {
        if (isHighLevel()) {
            initShadowMapOpt_2_0_0(config);
        } else {
            initShadowMapOpt_0_11_0_0(config);
        }
    }

    /**
     * map操作影子流初始化 0.11.0.0版本
     */
    private void initShadowMapOpt_0_11_0_0(KafkaStreamConfig config) {
        String topic = config.getTopic();
        String applicationId = config.getApplicationId();
        Object mapper = config.getMapper();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("register6 topic is {}, applicationId is {}", topic, applicationId);
        }

        final KStreamBuilder builder = new KStreamBuilder();
        builder.stream(Pradar.addClusterTestPrefix(topic)).map((KeyValueMapper) mapper);
        initKafkaStreamOpt_0_11_0_0(builder, config);
    }

    /**
     * foreach操作影子流初始化 0.11.0.0版本
     */
    private void initShadowForeachOpt_0_11_0_0(KafkaStreamConfig config) {
        String topic = config.getTopic();
        Object mapper = config.getMapper();
        final KStreamBuilder builder = new KStreamBuilder();
        builder.stream(Pradar.addClusterTestPrefix(topic)).foreach((ForeachAction) mapper);
        initKafkaStreamOpt_0_11_0_0(builder, config);
    }


    private void initKafkaStreamOpt_0_11_0_0(final KStreamBuilder builder, final KafkaStreamConfig config) {
        String topic = config.getTopic();
        String applicationId = config.getApplicationId();
        Properties ptProperties = config.getProperties();
        try {
            Class kafkaStreamsClass = Thread.currentThread().getContextClassLoader().loadClass("org.apache.kafka.streams.KafkaStreams");
            Constructor<?>[] constructors = kafkaStreamsClass.getDeclaredConstructors();
            for (Constructor constructor : constructors) {
                /**
                 * org.apache.kafka.streams.KafkaStreams#KafkaStreams(org.apache.kafka.streams.processor.TopologyBuilder, java.util.Properties)
                 */
                if (constructor.getParameterTypes().length == 2
                        && constructor.getParameterTypes()[0].getName().equals("org.apache.kafka.streams.processor.TopologyBuilder")
                        && constructor.getParameterTypes()[1].getName().equals("java.util.Properties")) {
                    final Object kafkaStreams = constructor.newInstance(builder, ptProperties);
                    LOGGER.warn("register7 topic is {}, applicationId is {}", topic, applicationId);
                    Method startMethod = kafkaStreams.getClass().getDeclaredMethod("start");
                    startMethod.invoke(kafkaStreams);
                    KafkaStreamsCaches.addKafkaStreams(applicationId, kafkaStreams);
                    break;
                }
            }
        } catch (Throwable e) {
            LOGGER.error("kakfa-streams initKafkaStreamOpt_0_11_0_0 error.", e);
        }
    }


    /**
     * map操作影子流初始化 2.0.0版本
     */
    private void initShadowMapOpt_2_0_0(KafkaStreamConfig config) {
        String topic = config.getTopic();
        String applicationId = config.getApplicationId();
        Object mapper = config.getMapper();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("register3 topic is {}, applicationId is {}", topic, applicationId);
        }
        //
        final StreamsBuilder builder = new StreamsBuilder();
        //
        builder.stream(Pradar.addClusterTestPrefix(topic)).map((KeyValueMapper) mapper);
        initKafkaStreamOpt_2_0_0(builder, config);
    }

    /**
     * foreach操作影子流初始化 2.0.0版本
     */
    private void initShadowForeachOpt_2_0_0(KafkaStreamConfig config) {
        String topic = config.getTopic();
        Object mapper = config.getMapper();
        final StreamsBuilder builder = new StreamsBuilder();
        builder.stream(Pradar.addClusterTestPrefix(topic)).foreach((ForeachAction) mapper);
        initKafkaStreamOpt_2_0_0(builder, config);
    }

    private void initKafkaStreamOpt_2_0_0(final StreamsBuilder builder, final KafkaStreamConfig config) {
        String topic = config.getTopic();
        String applicationId = config.getApplicationId();
        Properties ptProperties = config.getProperties();
        //构建Topology对象
        final Topology topo = builder.build();
        //构建 kafka流 API实例 将算子以及操作的服务器配置到kafka流
        final KafkaStreams stream = new KafkaStreams(topo, ptProperties);
        KafkaStreamsCaches.addKafkaStreams(applicationId, stream);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("register4 topic is {}, applicationId is {}", topic, applicationId);
        }
        stream.start();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("register5 topic is {}, applicationId is {}", topic, applicationId);
        }

    }


    private synchronized void initField(Object target, KafkaStreamConfig config) {
        try {
            initApplicationIdAndProp(target, config);
            if (kstreamMapField == null) {
                kstreamMapField = target.getClass().getDeclaredField("this$0");
                kstreamMapField.setAccessible(true);
            }
            Object kStreamMap = kstreamMapField.get(target);
            if (mapperField == null) {
                mapperField = kStreamMap.getClass().getDeclaredField(getActionFieldName().getActionFieldName());
                mapperField.setAccessible(true);
            }
            Object mapper = mapperField.get(kStreamMap);
            config.setMapper(mapper);
        } catch (Throwable e) {
            config.setHasError(true);
            LOGGER.error("kafka-streams initField error.", e);
        }
    }

    private void initApplicationIdAndProp(Object target, KafkaStreamConfig config) {
        Field streamsConfigField = null;
        try {
            if (contextField == null) {
                contextField = target.getClass().getSuperclass().getDeclaredField("context");
                contextField.setAccessible(true);
            }
            ProcessorContextImpl processorContext = (ProcessorContextImpl) contextField.get(target);
            String topic = processorContext.topic();
            config.setTopic(topic);
            String applicationId = processorContext.applicationId();
            config.setApplicationId(applicationId);
            Properties ptProperties = new Properties();
            streamsConfigField = processorContext.getClass().getSuperclass().getDeclaredField("config");
            streamsConfigField.setAccessible(true);
            StreamsConfig streamsConfig = (StreamsConfig) streamsConfigField.get(processorContext);
            ptProperties.putAll(streamsConfig.originals());
            /* 将业务applicationID 覆盖影子applicationID */
            if (!Pradar.isClusterTestPrefix(processorContext.applicationId())) {
                ptProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, Pradar.addClusterTestPrefix(applicationId));
            } else {
                ptProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
            }
            config.setProperties(ptProperties);
        } catch (Throwable e) {
            config.setHasError(true);
            LOGGER.error("kafka-streams initApplicationIdAndProp error.", e);
        }
    }
}
