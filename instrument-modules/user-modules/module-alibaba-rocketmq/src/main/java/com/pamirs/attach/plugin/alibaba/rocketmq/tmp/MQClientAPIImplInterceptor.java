package com.pamirs.attach.plugin.alibaba.rocketmq.tmp;

import com.alibaba.rocketmq.client.consumer.PullResult;
import com.alibaba.rocketmq.client.consumer.PullStatus;

import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/12/17 5:50 PM
 */
public class MQClientAPIImplInterceptor extends AroundInterceptor {

    private final Logger logger = LoggerFactory.getLogger("ROCKET_MQ_TMP_LOGGER");

    @Override
    public void doAfter(Advice advice) {
        PullResult pullResult = (PullResult)advice.getReturnObj();
        if (PullStatus.OFFSET_ILLEGAL.name().equals(pullResult.getPullStatus().name())) {
            logger.info("api pull result return OFFSET_ILLEGAL!");
        }
    }
}
