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

package com.pamirs.attach.plugin.apache.kafka.streamv2.consumer;

import com.pamirs.attach.plugin.apache.kafka.streamv2.consumer.config.KafkaShadowStreamConfig;
import com.pamirs.attach.plugin.apache.kafka.streamv2.consumer.server.KafkaShadowStreamServer;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.bean.SyncObjectData;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowConsumerExecute;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.processor.internals.InternalTopologyBuilder;
import org.apache.kafka.streams.processor.internals.QuickUnion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.*;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/1 14:40
 */
public class KafkaShadowStreamStartExecute implements ShadowConsumerExecute {

    private static final Logger logger = LoggerFactory.getLogger(KafkaShadowStreamStartExecute.class);

    private Boolean isHighLevel;

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

    @Override
    public List<ConsumerConfig> prepareConfig(SyncObjectData syncObjectData) {
        KafkaStreams bizStream = (KafkaStreams) syncObjectData.getTarget();

        // 因为2.0版本与1.0版本对应的类名不一致，所以通过反射获取对应的Object对象
        Object topologyBuilder = Reflect.on(Reflect.on(bizStream).get("streamsMetadataState")).get("builder");
        Set<String> sourceTopicNames = Reflect.on(topologyBuilder).get("sourceTopicNames");

        List<ConsumerConfig> configs = new ArrayList<ConsumerConfig>();
        StreamsConfig streamsConfig = Reflect.on(bizStream).get("config");
        String applicationId = streamsConfig.getString(StreamsConfig.APPLICATION_ID_CONFIG);

        Properties ptProperties = new Properties();
        ptProperties.putAll(streamsConfig.originals());
        /* 将业务applicationID 覆盖影子applicationID */
        if (!Pradar.isClusterTestPrefix(applicationId)) {
            ptProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, Pradar.addClusterTestPrefix(applicationId));
        } else {
            ptProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        }

        for (String topic : sourceTopicNames) {
            KafkaShadowStreamConfig config = new KafkaShadowStreamConfig(ptProperties, applicationId, topic, topologyBuilder);
            configs.add(config);
        }
        return configs;
    }

    @Override
    public ShadowServer fetchShadowServer(ConsumerConfig config, String shadowConfig) {
        KafkaStreams shadowStream;
        if (isHighLevel()) {
            shadowStream = createHighLevelStream((KafkaShadowStreamConfig) config);
        } else {
            shadowStream = createLowLevelStream((KafkaShadowStreamConfig) config);
        }

        return new KafkaShadowStreamServer(shadowStream);
    }

    /**
     * 创建2.0版本的 kafka-stream
     *
     * @param config 配置属性
     * @return KafkaStreams
     */
    private KafkaStreams createHighLevelStream(KafkaShadowStreamConfig config) {
        final StreamsBuilder builder = new StreamsBuilder();
        builder.stream(Pradar.addClusterTestPrefix(config.getTopic()));
        InternalTopologyBuilder internalTopologyBuilder = Reflect.on(builder).get("internalTopologyBuilder");
        copyNodeFactories(config.getTopologyBuilder(), internalTopologyBuilder);

        //构建Topology对象
        final Topology topo = builder.build();
        //构建 kafka流 API实例 将算子以及操作的服务器配置到kafka流
        final KafkaStreams stream = new KafkaStreams(topo, config.getProperties());
        logger.info("register4 topic is {}, applicationId is {}", config.getTopic(), config.getApplicationId());

        return stream;
    }

    /**
     * 创建0.11版本的 kafka-stream
     *
     * @param config 配置属性
     * @return KafkaStreams
     */
    private KafkaStreams createLowLevelStream(KafkaShadowStreamConfig config) {
        final KStreamBuilder builder = new KStreamBuilder();
        builder.stream(Pradar.addClusterTestPrefix(config.getTopic()));
        copyNodeFactories(config.getTopologyBuilder(), builder);

        Object kafkaStreams = null;
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
                    kafkaStreams = constructor.newInstance(builder, config.getProperties());
                    logger.info("register topic is {}, applicationId is {}", config.getTopic(), config.getApplicationId());
                    break;
                }
            }
        } catch (Throwable e) {
            logger.error("kakfa-streams initKafkaStreamOpt_0_11_0_0 error.", e);
        }
        return kafkaStreams instanceof KafkaStreams ? (KafkaStreams) kafkaStreams : null;
    }

    /**
     * copy NodeFactories 字段值
     *
     * @param originTopologyBuilder 原始的TopologyBuilder对象
     * @param shadowTopologyBuilder 影子的TopologyBuilder对象
     */
    private void copyNodeFactories(Object originTopologyBuilder, Object shadowTopologyBuilder) {
        LinkedHashMap originNodeFactories = Reflect.on(originTopologyBuilder).get("nodeFactories");
        LinkedHashMap shadowNodeFactories = Reflect.on(shadowTopologyBuilder).get("nodeFactories");
        QuickUnion nodeGrouper = Reflect.on(shadowTopologyBuilder).get("nodeGrouper");

        Set<Map.Entry> shadowSet = shadowNodeFactories.entrySet();
        String sourceName = "";
        for (Map.Entry entry : shadowSet) {
            if (entry.getKey().toString().startsWith("KSTREAM-SOURCE-")) {
                sourceName = entry.getKey().toString();
                break;
            }
        }
        Set<Map.Entry> originSet = originNodeFactories.entrySet();
        for (Map.Entry entry : originSet) {
            if (entry.getKey().toString().startsWith("KSTREAM-SOURCE-")) {
                continue;
            }
            shadowNodeFactories.put(entry.getKey(), entry.getValue());
            nodeGrouper.add(entry.getKey());
            nodeGrouper.unite(entry.getKey(), sourceName);
        }
    }
}
