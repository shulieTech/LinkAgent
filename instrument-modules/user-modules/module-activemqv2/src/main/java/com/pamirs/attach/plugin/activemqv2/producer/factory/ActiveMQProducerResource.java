package com.pamirs.attach.plugin.activemqv2.producer.factory;

import io.shulie.instrument.module.isolation.resource.ShadowResourceLifecycle;
import org.apache.activemq.ActiveMQMessageProducer;

/**
 * @author guann1n9
 * @date 2023/12/22 1:56 PM
 */
public class ActiveMQProducerResource  implements ShadowResourceLifecycle {


    private ActiveMQMessageProducer producer;


    public ActiveMQProducerResource(ActiveMQMessageProducer producer) {
        this.producer = producer;
    }

    @Override
    public Object getTarget() {
        return producer;
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public void start() {

    }

    @Override
    public void destroy(long timeout) {
        try {
            producer.close();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
