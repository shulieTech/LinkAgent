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

package io.shulie.instrument.module.messaging.kafka.model;

import com.pamirs.pradar.exception.PressureMeasureError;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import io.shulie.instrument.module.messaging.kafka.util.KafkaUtil;
import io.shulie.instrument.module.messaging.kafka.util.ReflectUtil;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.Set;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/12 14:48
 */
public class KafkaConsumerMetaData {

    public final static String REFLECT_FIELD_COORDINATOR = "coordinator";

    public final static String REFLECT_FIELD_GROUP_ID = "groupId";

    private final Set<String> topics;

    private final String groupId;

    private final String bootstrapServers;

    public KafkaConsumerMetaData(Set<String> topics, String groupId, String bootstrapServers) {
        this.topics = topics;
        this.groupId = groupId;
        this.bootstrapServers = bootstrapServers;
    }

    public static KafkaConsumerMetaData build(KafkaConsumer consumer) {
        Set<String> topics = KafkaUtil.getTopics(consumer);
        try {
            Object coordinator = Reflect.on(consumer).get(REFLECT_FIELD_COORDINATOR);
            Object groupId = ReflectUtil.reflectSlience(consumer, REFLECT_FIELD_GROUP_ID);
            if (groupId == null) {
                groupId = ReflectUtil.reflectSlience(coordinator, REFLECT_FIELD_GROUP_ID);
                if (groupId == null) {
                    throw new PressureMeasureError("未支持的kafka版本！未能获取groupId");
                }
            }
            String bootstrapServers = KafkaUtil.getBootstrapServers(consumer);
            String groupIdStr = "";
            if (groupId instanceof String) {
                groupIdStr = (String) groupId;
            } else {
                groupIdStr = ReflectUtil.reflectSlience(groupId, "value");
            }
            return new KafkaConsumerMetaData(topics, groupIdStr, bootstrapServers);
        } catch (ReflectException e) {
            throw new PressureMeasureError(e);
        }
    }


    public Set<String> getTopics() {
        return topics;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }
}
