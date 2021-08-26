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
package com.pamirs.attach.plugin.apache.kafka.stream.interceptor;

import com.pamirs.attach.plugin.apache.kafka.stream.constants.KafkaStreamsCaches;
import com.pamirs.attach.plugin.apache.kafka.stream.destroy.KafkaStreamDestroy;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.kafka.streams.StreamsConfig;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

/**
 * @author angju
 * @date 2021/5/24 22:56
 */
@Destroyable(KafkaStreamDestroy.class)
public class KafkaStreamsCloseInterceptor extends ParametersWrapperInterceptorAdaptor {
    private Field configField;

    private void initConfigField(Object kafkaStreams) {
        if (configField == null) {
            try {
                configField = kafkaStreams.getClass().getDeclaredField("config");
                configField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                LOGGER.error("kafka-streams can't found declared field config in KafkaStreams", e);
            }
        }
    }

    @Override
    public Object[] getParameter0(Advice advice) throws Throwable {
        Object kafkaStreams = advice.getTarget();

        initConfigField(kafkaStreams);
        if (configField == null) {
            return advice.getParameterArray();
        }

        StreamsConfig streamsConfig = (StreamsConfig) configField.get(kafkaStreams);
        String applicationId = streamsConfig.getString("application.id");
        if (Pradar.isClusterTestPrefix(applicationId)) {
            return advice.getParameterArray();
        }
        TimeUnit timeUnit = (TimeUnit) advice.getParameterArray()[1];
        long timeout = (Long) advice.getParameterArray()[0];
        KafkaStreamsCaches.close(applicationId, timeout, timeUnit);
        return advice.getParameterArray();
    }
}
