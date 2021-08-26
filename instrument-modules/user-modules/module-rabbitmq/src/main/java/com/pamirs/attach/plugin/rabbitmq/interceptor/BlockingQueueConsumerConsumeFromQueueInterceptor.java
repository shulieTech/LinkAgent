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
package com.pamirs.attach.plugin.rabbitmq.interceptor;

import com.pamirs.attach.plugin.rabbitmq.RabbitmqConstants;
import com.pamirs.attach.plugin.rabbitmq.common.ChannelHolder;
import com.pamirs.attach.plugin.rabbitmq.destroy.RabbitmqDestroy;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.amqp.rabbit.listener.BlockingQueueConsumer;
import org.springframework.amqp.rabbit.support.Delivery;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * @author angju
 * @date 2020/12/8 22:34
 */
@Destroyable(RabbitmqDestroy.class)
public class BlockingQueueConsumerConsumeFromQueueInterceptor extends AroundInterceptor {

    private Field consumerTagsField;

    public BlockingQueueConsumerConsumeFromQueueInterceptor() {
        try {
            consumerTagsField = BlockingQueueConsumer.class.getDeclaredField(RabbitmqConstants.REFLECT_FIELD_CONSUMER_TAGS);
            consumerTagsField.setAccessible(true);
        } catch (Throwable e) {
        }
    }

    private Map<String, String> getConsumerTags(Object target) {
        try {
            return (Map<String, String>) consumerTagsField.get(target);
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    public void doBefore(Advice advice) {
        if (ArrayUtils.isEmpty(advice.getParameterArray()) || advice.getParameterArray()[0] == null) {
            return;
        }

        if (consumerTagsField == null) {
            return;
        }

        Object[] args = advice.getParameterArray();
        Map<String, String> consumerTags = getConsumerTags(advice.getTarget());
        if (consumerTags == null) {
            return;
        }
        Delivery delivery = (Delivery) args[0];
        if (consumerTags.get(delivery.getConsumerTag()) != null) {
            return;
        }
        String queue = ChannelHolder.getQueueByTag(delivery.getConsumerTag());
        if (queue != null) {
            consumerTags.put(delivery.getConsumerTag(), queue);
        }
    }
}
