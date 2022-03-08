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
package com.pamirs.attach.plugin.alibaba.rocketmq.interceptor;

import com.alibaba.rocketmq.client.consumer.DefaultMQPushConsumer;
import com.alibaba.rocketmq.common.message.MessageQueue;

import com.pamirs.attach.plugin.alibaba.rocketmq.destroy.MqDestroy;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/30 3:53 下午
 */
@Destroyable(MqDestroy.class)
public class DefaultPushConsumerMinOffsetInterceptor extends AbstractUseShadowConsumerReplaceInterceptor {

    @Override
    protected CutOffResult execute(DefaultMQPushConsumer consumer, Advice advice) {
        try {
            return CutOffResult.cutoff(consumer.minOffset((MessageQueue)advice.getParameterArray()[0]));
        } catch (Throwable e) {
            logger.error("Alibaba-RocketMQ: minOffset err, queue: {}", advice.getParameterArray()[0], e);
            throw new PressureMeasureError(e);
        }
    }
}
