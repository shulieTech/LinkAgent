/*
 * *
 *  * Copyright 2021 Shulie Technology, Co.Ltd
 *  * Email: shulie@shulie.io
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.pamirs.attach.plugin.alibaba.rocketmq.interceptor.consumerfaile;

import com.alibaba.rocketmq.common.message.MessageQueue;
import com.pamirs.pradar.interceptor.ResultInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author angju
 * @date 2021/12/17 18:29
 */
public class PullKernelImplInterceptor extends ResultInterceptorAdaptor {
    private Logger logger = LoggerFactory.getLogger("ROCKET_MQ_TMP_LOGGER");

    @Override
    public void doBefore(Advice advice) {
        MessageQueue messageQueue = (MessageQueue) advice.getParameterArray()[0];
        logger.error("执行PullKernelImpl before, topic is " + messageQueue.getTopic() + " queueId is " + messageQueue.getQueueId());
    }

    @Override
    public Object getResult0(Advice advice) {
        MessageQueue messageQueue = (MessageQueue) advice.getParameterArray()[0];
        logger.error("执行PullKernelImpl after, topic is " +
                messageQueue.getTopic() + " queueId is " + messageQueue.getQueueId());
        return advice.getReturnObj();
    }

    @Override
    public void doException(Advice advice) {
        MessageQueue messageQueue = (MessageQueue) advice.getParameterArray()[0];
        logger.error("执行PullKernelImpl before, topic is " + messageQueue.getTopic() +
                " queueId is " + messageQueue.getQueueId() + " error is " + advice.getThrowable().getMessage());

    }
}
