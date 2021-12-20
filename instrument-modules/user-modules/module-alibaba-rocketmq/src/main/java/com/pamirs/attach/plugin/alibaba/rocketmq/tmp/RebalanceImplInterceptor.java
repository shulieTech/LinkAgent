package com.pamirs.attach.plugin.alibaba.rocketmq.tmp;

import java.util.Set;

import com.alibaba.rocketmq.common.message.MessageQueue;

import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/12/17 5:43 PM
 */
public class RebalanceImplInterceptor extends AroundInterceptor {

    private final Logger logger = LoggerFactory.getLogger("ROCKET_MQ_TMP_LOGGER");

    @Override
    public void doBefore(Advice advice) throws Throwable {
        Set<MessageQueue> mqSet = (Set<MessageQueue>)advice.getParameterArray()[1];
        StringBuilder sb = new StringBuilder("Rebalance allocate queues is : ");
        for (MessageQueue messageQueue : mqSet) {
            sb.append("[ topic : ").append(messageQueue.getBrokerName()).append("-").append(messageQueue.getQueueId())
                .append(" queueId : ").append(messageQueue.getQueueId()).append("]");
        }
        logger.info(sb.toString());
    }
}
