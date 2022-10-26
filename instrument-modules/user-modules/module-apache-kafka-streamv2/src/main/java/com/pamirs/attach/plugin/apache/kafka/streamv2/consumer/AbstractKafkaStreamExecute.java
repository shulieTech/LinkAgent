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
import com.pamirs.pradar.Pradar;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.processor.internals.ProcessorContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Properties;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/1 14:33
 */
public abstract class AbstractKafkaStreamExecute {

    private static final Logger logger = LoggerFactory.getLogger(AbstractKafkaStreamExecute.class);

    private Field contextField;
    private Field kstreamMapField;
    private Field mapperField;

//    protected synchronized void initField(Object target, KafkaShadowStreamConfig config) {
//        try {
//            initApplicationIdAndProp(target, config);
//            if (kstreamMapField == null) {
//                kstreamMapField = target.getClass().getDeclaredField("this$0");
//                kstreamMapField.setAccessible(true);
//            }
//            Object kStreamMap = kstreamMapField.get(target);
//            if (mapperField == null) {
//                mapperField = kStreamMap.getClass().getDeclaredField(getActionFieldName().getActionFieldName());
//                mapperField.setAccessible(true);
//            }
//            Object mapper = mapperField.get(kStreamMap);
//            config.setMapper(mapper);
//        } catch (Throwable e) {
//            config.setHasError(true);
//            logger.error("kafka-streams initField error.", e);
//        }
//    }

    protected void initApplicationIdAndProp(Object target, KafkaShadowStreamConfig config) {
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
//            config.setHasError(true);
            logger.error("kafka-streams initApplicationIdAndProp error.", e);
        }
    }
}
