package com.pamirs.attach.plugin.rabbitmq.interceptor;

import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/08/31 10:21 下午
 */
public class StrictExceptionHandlerInterceptor extends AroundInterceptor {

    private static Logger logger = LoggerFactory.getLogger(ChannelNProcessDeliveryInterceptor.class.getName());

    @Override
    public void doBefore(Advice advice) throws Throwable {
        if (advice.getParameterArray().length >= 2 && advice.getParameterArray()[1] instanceof Throwable) {
            logger.warn("[RabbitMQ] has error!", (Throwable)advice.getParameterArray()[1]);
        }
    }

}
