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
package com.pamirs.attach.plugin.apache.rocketmq.interceptor;

import com.pamirs.attach.plugin.apache.rocketmq.destroy.MqDestroy;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.common.message.MessageQueue;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/30 3:53 下午
 */
@Destroyable(MqDestroy.class)
public class DefaultPushConsumerMaxOffsetInterceptor extends AbstractUseShadowConsumerReplaceInterceptor {

    @Override
    protected CutOffResult execute(DefaultMQPushConsumer consumer, Advice advice) {
        Object[] args = advice.getParameterArray();
        try {
            return CutOffResult.cutoff(consumer.maxOffset((MessageQueue)args[0]));
        } catch (Throwable e) {
            logger.error("Apache-RocketMQ: maxOffset err, queue: {}", args[0], e);
            throw new PressureMeasureError(e);
        }
    }
}
