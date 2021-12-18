package com.pamirs.attach.plugin.alibaba.rocketmq.tmp;

import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/12/17 5:59 PM
 */
public class ProcessQueueInterceptor extends AroundInterceptor {

    private final Logger logger = LoggerFactory.getLogger("ROCKET_MQ_TMP_LOGGER");

    @Override
    public void doBefore(Advice advice) throws Throwable {
        if ((Boolean)advice.getParameterArray()[0]) {
            logger.warn("ProcessQueue droped", new Exception());
        }
    }
}
