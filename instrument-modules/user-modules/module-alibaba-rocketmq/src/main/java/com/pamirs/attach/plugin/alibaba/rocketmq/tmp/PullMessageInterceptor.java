package com.pamirs.attach.plugin.alibaba.rocketmq.tmp;

import com.alibaba.rocketmq.client.impl.consumer.PullRequest;

import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/12/17 5:29 PM
 */
public class PullMessageInterceptor extends AroundInterceptor {

    private final Logger logger = LoggerFactory.getLogger("ROCKET_MQ_TMP_LOGGER");

    @Override
    public void doBefore(Advice advice) {
        PullRequest pullRequest = (PullRequest)advice.getParameterArray()[0];
        logger.info("pull request with : topic : {} queue : {} LastConsumeTimestamp : {}",
            pullRequest.getMessageQueue().getBrokerName() + "-" + pullRequest.getMessageQueue().getTopic(),
            pullRequest.getMessageQueue().getQueueId(),
            pullRequest.getProcessQueue().getLastConsumeTimestamp());
    }
}
