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

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.impl.ChannelN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;

/**
 * 负责与业务 Channel 一对一进行映射，防止直接操作业务 Channel 导致 Channel 关闭的问题
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/25 2:34 下午
 */
public class ChannelHolder {
    private final static Logger LOGGER = LoggerFactory.getLogger(ChannelHolder.class);
    public static final Object NULL_OBJECT = new Object();
    private static ConcurrentHashMap<Integer, ConcurrentMap<String, Object>> consumerTags = new ConcurrentHashMap<Integer, ConcurrentMap<String, Object>>();
    private static ConcurrentHashMap<String, String> queueTags = new ConcurrentHashMap<String, String>();
    private static ConcurrentHashMap<Integer, Channel> channelCache = new ConcurrentHashMap<Integer, Channel>();
    /**
     * 业务 consumerTag -> 影子 consumerTag
     */
    private static ConcurrentHashMap<String, String> shadowTags = new ConcurrentHashMap<String, String>();

    public static void release() {
        consumerTags.clear();
        queueTags.clear();
        for (Map.Entry<Integer, Channel> entry : channelCache.entrySet()) {
            if (entry.getValue() != null && entry.getValue().isOpen()) {
                try {
                    entry.getValue().close();
                } catch (Throwable e) {
                    LOGGER.error("rabbitmq close shadow channel error.", e);
                }
            }
        }
        channelCache.clear();
    }

    public static String getQueueByTag(String consumerTag) {
        return queueTags.get(consumerTag);
    }


    /**
     * 向当前连接绑定一个 consumerTag
     *
     * @param target
     * @param businessConsumerTag 业务的 consumerTag
     * @param shadowConsumerTag   影子 consumerTag
     * @param queue               业务 queue 名称
     */
    public static void addConsumerTag(Channel target, String businessConsumerTag, String shadowConsumerTag, String queue) {
        int identityCode = System.identityHashCode(target);
        Channel shadowChannel = channelCache.get(identityCode);
        if (shadowChannel == null) {
            return;
        }
        int key = System.identityHashCode(shadowChannel);
        ConcurrentMap<String, Object> tags = consumerTags.get(key);
        if (tags == null) {
            tags = new ConcurrentHashMap<String, Object>();
            ConcurrentMap<String, Object> oldTags = consumerTags.putIfAbsent(key, tags);
            if (oldTags != null) {
                tags = oldTags;
            }
        }
        tags.put(shadowConsumerTag, NULL_OBJECT);
        /**
         * 防止并发情况，有其他的 在移除 ConsumerTag 可能导致该 Set 从 consumerTags 里面被移除
         */
        consumerTags.put(key, tags);
        queueTags.put(shadowConsumerTag, queue);
        shadowTags.put(businessConsumerTag, shadowConsumerTag);
    }

    /**
     * 增加影子消费者的订阅
     *
     * @param target
     * @param ptQueue
     * @param autoAck
     * @param consumerTag
     * @param noLocal
     * @param exclusive
     * @param arguments
     * @param callback
     * @return 返回 consumerTag
     */
    public static String consumeShadowQueue(Channel target, String ptQueue, boolean autoAck, String consumerTag, boolean noLocal, boolean exclusive, Map<String, Object> arguments, Consumer callback) throws IOException {
        Channel shadowChannel = getShadowChannel(target);
        if (shadowChannel == null || !shadowChannel.isOpen()) {
            LOGGER.warn("rabbitmq basicConsume failed. cause by shadow channel is not found or closed. queue={}, consumerTag={}", ptQueue, consumerTag);
            return null;
        }
        String result = shadowChannel.basicConsume(ptQueue, autoAck, consumerTag, noLocal, exclusive, arguments, callback);
        final int key = System.identityHashCode(shadowChannel);
        ConfigCache.putQueue(key, ptQueue);
        return result;
    }

    /**
     * 取消影子 consumer 的订阅，取消后会移除 consumerTag 的缓存
     *
     * @param target      业务 channel
     * @param consumerTag consumerTag
     */
    public static void cancelShadowConsumerTag(Channel target, String consumerTag) {
        Channel shadowChannel = getShadowChannel(target);
        if (shadowChannel == null || !shadowChannel.isOpen()) {
            LOGGER.warn("rabbitmq basicCancel failed. cause by shadow channel is not found or closed. consumerTag={}", consumerTag);
            return;
        }
        try {
            shadowChannel.basicCancel(consumerTag);
        } catch (IOException e) {
            LOGGER.error("rabbitmq basicCancel shadow consumer error. consumerTag={}", consumerTag, e);
        }
        removeConsumerTag(target, consumerTag);
    }


    /**
     * 移除 ChannelN对应的 ConsumerTag
     *
     * @param target
     * @param consumerTag
     */
    public static void removeConsumerTag(Channel target, String consumerTag) {
        int identityCode = System.identityHashCode(target);
        Channel shadowChannel = channelCache.get(identityCode);
        if (shadowChannel == null) {
            return;
        }
        int key = System.identityHashCode(shadowChannel);
        ConcurrentMap<String, Object> tags = consumerTags.get(key);
        if (tags == null) {
            return;
        }
        tags.remove(consumerTag);
        if (tags.isEmpty()) {
            consumerTags.remove(key);
        }
    }

    /**
     * 判断当前连接有没有此 consumerTag
     *
     * @param target
     * @param consumerTag
     * @return
     */
    public static boolean isExistsConsumerTag(ChannelN target, String consumerTag) {
        int identityCode = System.identityHashCode(target);
        Channel shadowChannel = channelCache.get(identityCode);
        if (shadowChannel == null) {
            return false;
        }
        int key = System.identityHashCode(shadowChannel);
        ConcurrentMap<String, Object> tags = consumerTags.get(key);
        if (tags == null) {
            return false;
        }
        return tags.containsKey(consumerTag);
    }

    /**
     * 检测 queue 是否存在，与业务 Channel 隔离开来
     * 如果获取Channel 失败，则直接返回 false
     *
     * @param target 业务 Channel
     * @param queue  检测的 Queue
     * @return 返回 queue 是否存在
     */
    public static boolean isQueueExists(Channel target, String queue) {
        Channel channel = getShadowChannel(target);
        if (channel == null) {
            return false;
        }
        try {
            channel.queueDeclarePassive(queue);
            return true;
        } catch (Throwable e) {
            return false;
        } finally {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                } catch (TimeoutException e) {
                } catch (Throwable e) {
                }
            }
        }
    }

    /**
     * 根据业务的 consumerTag 判断该 consumer 是否已经存在影子消费者
     *
     * @param consumerTag 业务 consumerTag
     * @return 返回是否已经存在影子 Consumer
     */
    public static boolean existsConsumer(String consumerTag) {
        return shadowTags.containsKey(consumerTag);
    }

    /**
     * 获取影子 Channel
     *
     * @param target
     * @return
     */
    public static Channel getShadowChannel(Channel target) {
        if (target == null || !target.isOpen()) {
            return null;
        }
        int key = System.identityHashCode(target);
        Channel shadowChannel = channelCache.get(key);
        if (shadowChannel == null || !shadowChannel.isOpen()) {
            Channel channel = null;
            try {
                channel = target.getConnection().createChannel();
            } catch (IOException e) {
                return null;
            }
            Channel old = channelCache.putIfAbsent(key, channel);
            if (old != null) {
                if (!old.isOpen()) {
                    channelCache.put(key, channel);
                } else {
                    try {
                        channel.close();
                    } catch (Throwable e) {
                    }
                    channel = old;
                }
            }
            return channel;
        }
        return shadowChannel;
    }
}
