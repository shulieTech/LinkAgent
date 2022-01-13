package com.pamirs.attach.plugin.rabbitmq.interceptor;

import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.ShutdownSignalException;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/01/13 2:58 PM
 */
public class NotifyConsumerOfShutdownInterceptor extends AroundInterceptor {

    private static Logger logger = LoggerFactory.getLogger(NotifyConsumerOfShutdownInterceptor.class.getName());

    @Override
    public void doAfter(Advice advice) throws Throwable {
        Object[] args = advice.getParameterArray();
        if (args.length != 3) {
            logger.warn("[RabbitMQ] ConsumerOfShutdown! args : {}", args);
            return;
        }
        try {
            String consumerTag = (String)args[0];
            Consumer consumer = (Consumer)args[1];
            ShutdownSignalException signal = (ShutdownSignalException)args[2];
            logger.warn("[RabbitMQ] ConsumerOfShutdown! consumerTag : {}, consumer:{}", consumerTag, consumer, signal);
        } catch (Exception ignore) {
            logger.warn("[RabbitMQ] ConsumerOfShutdown! args : {}", args);
        }
    }

}

