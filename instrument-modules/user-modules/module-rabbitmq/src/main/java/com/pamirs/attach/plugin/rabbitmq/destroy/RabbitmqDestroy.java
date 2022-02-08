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
package com.pamirs.attach.plugin.rabbitmq.destroy;

import com.pamirs.attach.plugin.rabbitmq.common.ChannelHolder;
import com.pamirs.attach.plugin.rabbitmq.consumer.admin.support.cache.CacheSupportFactory;
import com.pamirs.attach.plugin.rabbitmq.interceptor.SpringBlockingQueueConsumerDeliveryInterceptor;
import com.shulie.instrument.simulator.api.listener.Destroyed;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/6/16 1:53 下午
 */
public class RabbitmqDestroy implements Destroyed {
    @Override
    public void destroy() {
        ChannelHolder.release();
        CacheSupportFactory.destroy();
        for (Object value : SpringBlockingQueueConsumerDeliveryInterceptor.RUNNING_CONTAINER.values()) {
            if(value instanceof AbstractMessageListenerContainer){
                ((AbstractMessageListenerContainer)value).destroy();
            }
        }
        SpringBlockingQueueConsumerDeliveryInterceptor.RUNNING_CONTAINER.clear();
    }
}
