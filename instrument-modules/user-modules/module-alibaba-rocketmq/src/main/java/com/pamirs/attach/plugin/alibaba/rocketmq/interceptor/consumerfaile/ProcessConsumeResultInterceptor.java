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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.alibaba.rocketmq.common.message.MessageQueue;

import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author angju
 * @date 2021/12/17 18:48
 */
public class ProcessConsumeResultInterceptor extends AroundInterceptor {
    private Logger logger = LoggerFactory.getLogger("ROCKET_MQ_TMP_LOGGER");

    private MessageQueue getMessageQueue(Object target) {
        try {
            Method method = target.getClass().getDeclaredMethod("getMessageQueue");
            method.setAccessible(true);
            return (MessageQueue)method.invoke(target);
        } catch (NoSuchMethodException e) {
        } catch (InvocationTargetException e) {
        } catch (IllegalAccessException illegalAccessException) {
        }
        return null;
    }

    private String topic = null;
    private String broker = null;
    private int queueId = -1;

    @Override
    public void doBefore(Advice advice) {
        Object consumeRequest = advice.getParameterArray()[2];
        if (topic == null) {
            MessageQueue messageQueue = getMessageQueue(consumeRequest);
            if (messageQueue == null) {
                return;
            }
            topic = messageQueue.getTopic();
            broker = messageQueue.getBrokerName();
            queueId = messageQueue.getQueueId();
        }
        logger.error(
            "执行ProcessConsumeResultInterceptor before, topic is " + broker + "-" + topic + " queueId is " + queueId);

    }

    @Override
    public void doAfter(Advice advice) {
        logger.error("执行ProcessConsumeResultInterceptor after, topic is " + broker + "-" + topic + " queueId is " + queueId);
    }

    @Override
    public void doException(Advice advice) {
        logger.error("执行ProcessConsumeResultInterceptor error, topic is {}, queueId is {},error is {}",
            topic, queueId, advice.getThrowable().getMessage());
        logger.error("执行ProcessConsumeResultInterceptor before, topic is " + broker + "-" + topic +
            " queueId is " + queueId + " error is " + advice.getThrowable().getMessage());

    }
}
