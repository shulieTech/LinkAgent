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

package io.shulie.instrument.module.messaging.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/12 11:58
 */
public class ApacheKafkaHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApacheKafkaHandler.class);

    /**
     * 缓存定制化适配过 apache-kafka poll模式的 consumer对象
     *
     * @see com.pamirs.attach.plugin.apache.kafka.interceptor.ConsumerPollInterceptor#cutoff0(com.shulie.instrument.simulator.api.listener.ext.Advice)
     */
    private static final Map<Consumer, String> WORK_WITH_OTHER_BIZ = new ConcurrentHashMap<>();

    private static final Map<Consumer, String> WORK_WITH_OTHER_SHADOW = new ConcurrentHashMap<>();

    /**
     * 记录适配过的业务 kafkaConsumer 对象
     *
     * @param consumer 业务kafkaConsumer 对象
     * @param isShadow 是否是影子对象
     */
    public static void addKafkaConsumerWorkWithOther(Consumer consumer, boolean isShadow) {
        if (consumer == null) {
            return;
        }
        if (isShadow) {
            WORK_WITH_OTHER_SHADOW.put(consumer, "");
        } else {
            WORK_WITH_OTHER_BIZ.put(consumer, "");
        }
    }

    /**
     * 移除注册的consumer对象
     *
     * @param consumer kafkaConsumer对象
     */
    public static void removeKafkaConsumerWorkWithOther(Consumer consumer) {
        if (consumer == null) {
            return;
        }
        WORK_WITH_OTHER_SHADOW.remove(consumer);
        WORK_WITH_OTHER_BIZ.remove(consumer);
    }

    /**
     * 判断对应的kafkaConsumer对象是否适配过
     *
     * @param consumer kafkaConsumer对象
     * @return true or false
     */
    public static boolean isWorkWithOther(Consumer consumer) {
        return WORK_WITH_OTHER_BIZ.get(consumer) != null || WORK_WITH_OTHER_SHADOW.get(consumer) != null;
    }

    /**
     * 是否是影子consumer
     *
     * @param consumer kafkaConsumer对象
     * @return true or false
     */
    public static boolean isShadowConsumerWorkWithOther(Consumer consumer) {
        return WORK_WITH_OTHER_SHADOW.get(consumer) != null;
    }
}
