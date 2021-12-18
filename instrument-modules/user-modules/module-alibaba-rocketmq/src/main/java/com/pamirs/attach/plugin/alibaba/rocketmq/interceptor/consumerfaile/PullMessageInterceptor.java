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

import com.alibaba.rocketmq.client.impl.consumer.PullRequest;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author angju
 * @date 2021/12/17 17:31
 */
public class PullMessageInterceptor extends AroundInterceptor {


    private Logger logger = LoggerFactory.getLogger("ROCKET_MQ_TMP_LOGGER");




    @Override
    public void doBefore(Advice advice) {
        PullRequest pullRequest = (PullRequest) advice.getParameterArray()[0];
        logger.error("执行pullMessage before, topic is " + pullRequest.getMessageQueue().getTopic()
                + " queueId is " + pullRequest.getMessageQueue().getQueueId()
                );
    }

    @Override
    public void doAfter(Advice advice) {
        PullRequest pullRequest = (PullRequest) advice.getParameterArray()[0];
        logger.error("执行pullMessage after, topic is " + pullRequest.getMessageQueue().getTopic()
                + " queueId is " + pullRequest.getMessageQueue().getQueueId()
        );
    }

    @Override
    public void doException(Advice advice) {
        PullRequest pullRequest = (PullRequest) advice.getParameterArray()[0];
        logger.error("执行pullMessage error, topic is " + pullRequest.getMessageQueue().getTopic()
                + " queueId is " + pullRequest.getMessageQueue().getQueueId() + " error is " + advice.getThrowable().getMessage()
        );
    }

}
