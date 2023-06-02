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
package com.pamirs.attach.plugin.apache.kafka.util;

import com.pamirs.attach.plugin.apache.kafka.KafkaConstants;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import io.shulie.instrument.module.messaging.kafka.util.ConsumerConfigHolder;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/09/08 4:27 下午
 */
public class KafkaUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaUtils.class);

    public static String getRemoteAddress(Object remoteAddressFieldAccessor, DynamicFieldManager manager) {
        String remoteAddress = manager.getDynamicField(remoteAddressFieldAccessor,
                KafkaConstants.DYNAMIC_FIELD_REMOTE_ADDRESS);
        if (StringUtils.isEmpty(remoteAddress)) {
            if (remoteAddressFieldAccessor instanceof KafkaConsumer) {
                try {
                    remoteAddress = ConsumerConfigHolder.getBootstrapServers((KafkaConsumer<?, ?>) remoteAddressFieldAccessor);
                    if (remoteAddress.contains(",")) {
                        String[] split = remoteAddress.split(",");
                        remoteAddress = Arrays.stream(split).sorted().collect(Collectors.joining(","));
                    }
                    manager.setDynamicField(remoteAddressFieldAccessor, KafkaConstants.DYNAMIC_FIELD_REMOTE_ADDRESS, remoteAddress);
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

}
