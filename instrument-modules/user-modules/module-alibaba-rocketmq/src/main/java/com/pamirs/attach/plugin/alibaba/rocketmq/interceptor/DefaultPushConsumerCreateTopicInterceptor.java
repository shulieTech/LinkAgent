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
import com.alibaba.rocketmq.client.exception.MQClientException;

import com.pamirs.attach.plugin.alibaba.rocketmq.destroy.MqDestroy;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/30 3:53 下午
 */
@Destroyable(MqDestroy.class)
public class DefaultPushConsumerCreateTopicInterceptor extends AbstractUseShadowConsumerReplaceInterceptor {
    protected final static Logger logger = LoggerFactory.getLogger(DefaultPushConsumerCreateTopicInterceptor.class);

    @Override
    protected CutOffResult execute(DefaultMQPushConsumer consumer, Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args.length == 3) {
            try {
                consumer.createTopic((String)args[0], (String)args[1], (Integer)args[2]);
            } catch (MQClientException e) {
                logger.error("Alibaba-RocketMQ: create topic err, key: {} newTopic:{},queueNum:{}", args[0], args[1],
                    args[2], e);
                throw new PressureMeasureError(e);
            }
        } else if (args.length == 4) {
            try {
                consumer.createTopic((String)args[0], (String)args[1], (Integer)args[2], (Integer)args[3]);
            } catch (Throwable e) {
                logger.error("Alibaba-RocketMQ: create topic err, key: {} newTopic: {}, queueNum: {}, topicSysFlag: {}",
                    args[0], args[1], args[2], args[3], e);
                throw new PressureMeasureError(e);
            }
        }
        return CutOffResult.cutoff(null);
    }
}
