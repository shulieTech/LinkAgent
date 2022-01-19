package com.pamirs.attach.plugin.rabbitmq.interceptor;

import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/08/31 10:21 下午
 */
public class AMQConnectionInterceptor extends AroundInterceptor {

    private static Logger logger = LoggerFactory.getLogger(ChannelNProcessDeliveryInterceptor.class.getName());

    @Override
    public void doAfter(Advice advice) throws Throwable {
        logger.warn("[RabbitMQ] AMQConnection has closed!");
    }

}
