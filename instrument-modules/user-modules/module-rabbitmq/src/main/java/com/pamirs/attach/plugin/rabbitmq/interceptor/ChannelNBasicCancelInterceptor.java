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

import com.pamirs.attach.plugin.rabbitmq.common.ChannelHolder;
import com.pamirs.attach.plugin.rabbitmq.destroy.RabbitmqDestroy;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.rabbitmq.client.impl.ChannelN;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author xiaobin.zfb
 * @since 2020/7/31 12:06 上午
 */
@Destroyable(RabbitmqDestroy.class)
public class ChannelNBasicCancelInterceptor extends AroundInterceptor {
    private final static Logger LOGGER = LoggerFactory.getLogger(ChannelNBasicCancelInterceptor.class.getName());

    @Override
    public void doBefore(Advice advice) {
        Object[] args = advice.getParameterArray();
        Object target = advice.getTarget();
        String consumerTag = null;
        try {
            consumerTag = (String) args[0];
            ChannelN channel = (ChannelN) target;
            if (!Pradar.isClusterTestPrefix(consumerTag)) {
                consumerTag = Pradar.addClusterTestPrefix(consumerTag);
                if (ChannelHolder.isExistsConsumerTag(channel, consumerTag)) {
                    ChannelHolder.cancelShadowConsumerTag(channel, consumerTag);
                }
            }
        } catch (Throwable e) {
            LOGGER.error("RabbitMQ basic cancel consumerTag:{}", consumerTag, e);
        }
    }
}
