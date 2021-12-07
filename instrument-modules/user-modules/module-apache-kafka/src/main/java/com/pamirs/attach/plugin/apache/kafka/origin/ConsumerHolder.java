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
package com.pamirs.attach.plugin.apache.kafka.origin;

import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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

    private static final Set<Consumer<?, ?>> WORK_WITH_SPRING = Collections.synchronizedSet(
            new HashSet<Consumer<?, ?>>());

    private static final Map<Integer, ConsumerProxy> PROXY_MAPPING = new HashMap<Integer, ConsumerProxy>();
    /**
     * key topic+groupid
     * value 业务消费组consumer对象的hashcode
     */
    private static final Map<String, Integer> SHADOW_PROXY_MAPPING = new HashMap<String, Integer>();

    private final static Map<Integer, ConsumerMetaData> cache = new ConcurrentHashMap();

    public static void release() {
        WORK_WITH_SPRING.clear();
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
    }

    public static void addWorkWithSpring(Consumer<?, ?> consumer) {
        ConsumerHolder.WORK_WITH_SPRING.add((Consumer<?, ?>) consumer);
    }

    public static boolean isWorkWithOtherFramework(Consumer<?, ?> consumer) {
        return WORK_WITH_SPRING.contains(consumer) && !ConsumerHolder.isZTO;
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

    private static String getConsumerMetaDataKey(ConsumerMetaData consumerMetaData){
        StringBuilder s= new StringBuilder();
        for (Object key : consumerMetaData.getTopics().toArray()){
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
}
