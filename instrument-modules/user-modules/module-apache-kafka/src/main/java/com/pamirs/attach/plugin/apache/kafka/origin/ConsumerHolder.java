/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.apache.kafka.origin;

import com.pamirs.attach.plugin.apache.kafka.util.AopTargetUtil;
import com.pamirs.pradar.pressurement.agent.shared.util.PradarSpringUtil;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import io.shulie.instrument.module.messaging.kafka.ApacheKafkaHandler;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/05/12 4:15 下午
 */
public class ConsumerHolder {

    public static boolean isZTO;

    private static final Logger logger = LoggerFactory.getLogger(ConsumerHolder.class);
    private static final boolean isInfoEnabled = logger.isInfoEnabled();

    static {
        try {
            Class.forName("com.zto.consumer.KafkaConsumerProxy");
            isZTO = true;
        } catch (ClassNotFoundException e) {
            isZTO = false;
        }
    }

    private static final Set<String> SHADOW_CONTAINER_BEAN_NAMES = new HashSet<String>();

    private static final Map<Consumer<?, ?>, String> WORK_WITH_SPRING = new ConcurrentHashMap<>();

    private static final Map<Integer, ConsumerProxy> PROXY_MAPPING = new HashMap<Integer, ConsumerProxy>();

    private static final AtomicBoolean spring_kafka_consumer_init_flag = new AtomicBoolean();
    /**
     * key topic+groupid
     * value 业务消费组consumer对象的hashcode
     */
    private static final Map<String, Integer> SHADOW_PROXY_MAPPING = new HashMap<String, Integer>();

    private final static Map<Integer, ConsumerMetaData> cache = new ConcurrentHashMap();

    public static void release() {
        cache.clear();
        Iterator<Map.Entry<Integer, ConsumerProxy>> it = PROXY_MAPPING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ConsumerProxy> entry = it.next();
            it.remove();
            Consumer consumer = entry.getValue().getPtConsumer();
            if (consumer != null) {
                consumer.close();
            }
        }
        PROXY_MAPPING.clear();

        for (String beanName : SHADOW_CONTAINER_BEAN_NAMES) {
            ConcurrentMessageListenerContainer container = PradarSpringUtil.getBeanFactory().getBean(beanName, ConcurrentMessageListenerContainer.class);
            container.stop();
        }
        SHADOW_CONTAINER_BEAN_NAMES.clear();
        for (Consumer<?, ?> consumer : WORK_WITH_SPRING) {
            consumer.close();
        }
        WORK_WITH_SPRING.clear();
    }

    public static void addWorkWithSpring(Consumer<?, ?> consumer) {
        ConsumerHolder.WORK_WITH_SPRING.put(consumer, "");
    }

    public static boolean isWorkWithOtherFramework(Consumer<?, ?> consumer) {
        extractSpringKafkaConsumersFromSpringContext();
        return (WORK_WITH_SPRING.get(consumer) != null || ApacheKafkaHandler.isWorkWithOther(consumer)) && !ConsumerHolder.isZTO;
    }

    public static boolean isWorkWithSpring(Consumer<?, ?> consumer) {
        extractSpringKafkaConsumersFromSpringContext();
        return WORK_WITH_SPRING.get(consumer) != null && !ConsumerHolder.isZTO;
    }

    public static ConsumerProxy getProxy(Object target) {
        return PROXY_MAPPING.get(System.identityHashCode(target));
    }

    public static ConsumerMetaData getConsumerMetaData(KafkaConsumer consumer) {
        ConsumerMetaData consumerMetaData = cache.get(consumer);
        if (consumerMetaData == null) {
            synchronized (cache) {
                consumerMetaData = cache.get(consumer);
                if (consumerMetaData == null) {
                    consumerMetaData = ConsumerMetaData.build(consumer);
                    cache.put(System.identityHashCode(consumer), consumerMetaData);
                }
            }
        }
        return consumerMetaData;
    }

    private static String getConsumerMetaDataKey(ConsumerMetaData consumerMetaData) {
        StringBuilder s = new StringBuilder();
        for (Object key : consumerMetaData.getTopics().toArray()) {
            s.append(key).append("#");
        }
        s.append(consumerMetaData.getGroupId());
        return s.toString();
    }

    public static Map<Integer, ConsumerProxy> getProxyMapping() {
        return PROXY_MAPPING;
    }

    public static Map<String, Integer> getShadowProxyMapping() {
        return SHADOW_PROXY_MAPPING;
    }

    public static Map<Integer, ConsumerMetaData> getCache() {
        return cache;
    }

    public static ConsumerProxy getProxyOrCreate(KafkaConsumer consumer, long timeout) {
        int code = System.identityHashCode(consumer);
        ConsumerMetaData consumerMetaData = getConsumerMetaData(consumer);
        ConsumerProxy consumerProxy = ConsumerHolder.PROXY_MAPPING.get(code);
        if (consumerProxy == null) {
            synchronized (ConsumerHolder.PROXY_MAPPING) {
                consumerProxy = ConsumerHolder.PROXY_MAPPING.get(code);
                if (consumerProxy == null) {
                    try {
                        consumerProxy = new ConsumerProxy(consumer, consumerMetaData, getAllowMaxLag(), timeout);
                        if (isInfoEnabled) {
                            logger.info("shadow consumer create successful! with biz group id : {} biz topic : {} pt group id : {} pt_topic : {}",
                                    consumerMetaData.getGroupId(), consumerMetaData.getTopics(),
                                    consumerMetaData.getPtGroupId(), consumerMetaData.getShadowTopics());
                        }
                        ConsumerHolder.PROXY_MAPPING.put(code, consumerProxy);
                        ConsumerHolder.SHADOW_PROXY_MAPPING.put(getConsumerMetaDataKey(consumerMetaData), code);
                    } catch (Exception e) {
                        logger.error("shadow consumer create fail!", e);
                        return null;
                    }
                }
            }
        }
        return consumerProxy;
    }

    private static long getAllowMaxLag() {
        long maxLagMillSecond = TimeUnit.SECONDS.toMillis(3);
        String maxLagMillSecondStr = System.getProperty("shadow.kafka.maxLagMillSecond");
        if (!StringUtils.isEmpty(maxLagMillSecondStr)) {
            try {
                maxLagMillSecond = Long.parseLong(maxLagMillSecondStr);
            } catch (NumberFormatException ignore) {
            }
        }
        return maxLagMillSecond;
    }

    private static void extractSpringKafkaConsumersFromSpringContext() {
        if (spring_kafka_consumer_init_flag.get()) {
            return;
        }
        Object factory = PradarSpringUtil.getBeanFactory();
        if (factory == null) {
            return;
        }
        Object containsBean = Reflect.on(factory).call("containsBean", "org.springframework.kafka.config.internalKafkaListenerEndpointRegistry").get();
        if (Boolean.FALSE.equals(containsBean)) {
            spring_kafka_consumer_init_flag.set(true);
            return;
        }
        Object endpointRegistry = Reflect.on(factory).call("getBean", "org.springframework.kafka.config.internalKafkaListenerEndpointRegistry").get();
        Map<String, Object> listenerContainers = Reflect.on(endpointRegistry).get("listenerContainers");
        if (listenerContainers.isEmpty()) {
            spring_kafka_consumer_init_flag.set(true);
            return;
        }
        for (Object container : listenerContainers.values()) {
            if ("ConcurrentMessageListenerContainer".equals(container.getClass().getSimpleName())) {
                List containers = Reflect.on(container).get("containers");
                // 说明某些container还没初始化完全
                if (containers.isEmpty()) {
                    return;
                }
                for (Object innerContainer : containers) {
                    Consumer consumer = extractKafkaConsumer(innerContainer);
                    if (consumer == null) {
                        return;
                    }
                    WORK_WITH_SPRING.put(consumer, "");
                }
            } else if ("KafkaMessageListenerContainer".equals(container.getClass().getSimpleName())) {
                Consumer consumer = extractKafkaConsumer(container);
                if (consumer == null) {
                    return;
                }
                WORK_WITH_SPRING.put(consumer, "");
            }
        }
        spring_kafka_consumer_init_flag.set(true);
    }

    private static Consumer extractKafkaConsumer(Object KafkaMessageListenerContainer) {
        Object listenerConsumer = Reflect.on(KafkaMessageListenerContainer).get("listenerConsumer");
        // container没初始化好
        if (listenerConsumer == null) {
            return null;
        }
        Consumer consumer = Reflect.on(listenerConsumer).get("consumer");
        if (AopTargetUtil.isAopProxy(consumer)) {
            try {
                return (Consumer) AopTargetUtil.getTarget(consumer);
            } catch (Exception e) {
                logger.error("extract kafka consumer from spring-kafka-container occur exception", e);
            }
        }
        return consumer;
    }

    public static void addShadowContainerBeanName(String name){
        SHADOW_CONTAINER_BEAN_NAMES.add(name);
    }
}
