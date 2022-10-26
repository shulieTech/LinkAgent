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
package com.pamirs.attach.plugin.apache.kafkav2.util;

import com.pamirs.attach.plugin.apache.kafkav2.constant.KafkaConstants;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.KafkaUtils;

import java.util.Map;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/09/08 4:27 下午
 */
public class KafkaUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaUtils.class);

    public static String getRemoteAddress(Object remoteAddressFieldAccessor, DynamicFieldManager manager) {
        String remoteAddress = manager.getDynamicField(remoteAddressFieldAccessor,
                KafkaConstants.DYNAMIC_FIELD_REMOTE_ADDRESS);

        if (StringUtils.isEmpty(remoteAddress)) {
            if (remoteAddressFieldAccessor instanceof KafkaConsumer) {
                try {
                    remoteAddress = KafkaUtil.getBootstrapServers((KafkaConsumer<?, ?>) remoteAddressFieldAccessor);
                    manager.setDynamicField(remoteAddressFieldAccessor, KafkaConstants.DYNAMIC_FIELD_REMOTE_ADDRESS,
                            remoteAddress);
                    return remoteAddress;
                } catch (Throwable e) {
                    LOGGER.warn("can not get remoteAddress", e);
                }
            }
            return KafkaConstants.UNKNOWN;
        } else {
            return remoteAddress;
        }
    }

    public static String getBootstrapServers(KafkaConsumer<?, ?> consumer) {
        Object metadata = Reflect.on(consumer).get("metadata");

        Object cluster = ReflectUtil.reflectSlience(metadata, "cluster");
        Iterable<Node> nodes;
        if (cluster != null) {
            nodes = Reflect.on(cluster).get("nodes");
        } else {
            Object cache = ReflectUtil.reflectSlience(metadata, "cache");
            if (cache != null) {
                Object tmpNodes = Reflect.on(cache).get("nodes");
                if (tmpNodes instanceof Iterable) {
                    nodes = (Iterable<Node>) tmpNodes;
                } else if (tmpNodes instanceof Map) {
                    nodes = ((Map<?, Node>) tmpNodes).values();
                } else {
                    throw new PressureMeasureError("未支持的kafka版本！未能获取nodes");
                }
            } else {
                throw new PressureMeasureError("未支持的kafka版本！未能获取nodes");
            }
        }
        StringBuilder sb = new StringBuilder();
        for (Node node : nodes) {
            sb.append(Reflect.on(node).get("host").toString()).append(":").append(Reflect.on(node).get("port")
                    .toString()).append(",");
        }
        return sb.substring(0, sb.length() - 1);
    }
}
