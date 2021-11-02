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
package com.pamirs.attach.plugin.rabbitmq.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/5/9 1:56 上午
 */
public class ConfigCache {

    private static boolean workWithSpring;
    private static Map<Integer, Object> caches = new ConcurrentHashMap<Integer, Object>();
    private static Map<Integer, String> queueCaches = new ConcurrentHashMap<Integer, String>();
    private static Map<ConsumerMetaDataCacheKey, ConsumerMetaData> consumerMetaDataCaches
            = new ConcurrentHashMap<ConsumerMetaDataCacheKey, ConsumerMetaData>();


    public static void removeConsumerMetaDataCaches(ConsumerMetaDataCacheKey key) {
        consumerMetaDataCaches.remove(key);
    }

    static {
        try {
            Class.forName("org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer");
            workWithSpring = true;
        } catch (ClassNotFoundException e) {
            workWithSpring = false;
        }
    }

    public static void release() {
        caches.clear();
        queueCaches.clear();
        consumerMetaDataCaches.clear();
    }

    public static boolean isWorkWithSpring() {
        return workWithSpring;
    }

    public static boolean containsQueue(Integer key) {
        return queueCaches.containsKey(key);
    }

    public static boolean containsCache(Integer key) {
        return caches.containsKey(key);
    }

    public static void putCache(Integer key, Object value) {
        caches.put(key, value);
    }

    public static void putQueue(Integer key, String value) {
        queueCaches.put(key, value);
    }

    public static String getQueue(Integer key) {
        return queueCaches.get(key);
    }

    public static void putConsumerMetaData(Integer key, String consumerTag, ConsumerMetaData consumerMetaData) {
        consumerMetaDataCaches.put(new ConsumerMetaDataCacheKey(key, consumerTag), consumerMetaData);
    }

    public static ConsumerMetaData getConsumerMetaData(Integer key, String consumerTag) {
        return consumerMetaDataCaches.get(new ConsumerMetaDataCacheKey(key, consumerTag));
    }

    public static class ConsumerMetaDataCacheKey {

        private final int channelKey;

        private final String consumerTag;

        public ConsumerMetaDataCacheKey(int channelKey, String consumerTag) {
            this.channelKey = channelKey;
            this.consumerTag = consumerTag;
        }

        @Override
        public int hashCode() {
            int result = 1;
            result = 31 * result + channelKey;
            result = 31 * result + consumerTag.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ConsumerMetaDataCacheKey) {
                ConsumerMetaDataCacheKey anotherObject = (ConsumerMetaDataCacheKey) obj;
                return anotherObject.channelKey == this.channelKey && anotherObject.consumerTag.equals(
                        this.consumerTag);
            }
            return false;
        }
    }
}
