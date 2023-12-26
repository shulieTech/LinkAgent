package com.pamirs.attach.plugin.activemqv2.producer.factory;

import com.pamirs.attach.plugin.activemqv2.util.ActiveMQDestinationUtil;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import io.shulie.instrument.module.isolation.resource.ShadowResourceLifecycle;
import io.shulie.instrument.module.isolation.resource.ShadowResourceProxyFactory;
import org.apache.activemq.ActiveMQMessageProducer;
import org.apache.activemq.ActiveMQSession;
import org.apache.activemq.command.ActiveMQDestination;

/**
 * @author guann1n9
 * @date 2023/12/22 1:50 PM
 */
public class ShadowProducerFactory implements ShadowResourceProxyFactory {


    @Override
    public ShadowResourceLifecycle createShadowResource(Object bizTarget) {
        if (!(bizTarget instanceof ActiveMQMessageProducer)) {
            return null;
        }

        try {
            ActiveMQMessageProducer bizProducer = (ActiveMQMessageProducer) bizTarget;
            ActiveMQSession session = Reflect.on(bizProducer).field("session").get();
            ActiveMQDestination destination =  (ActiveMQDestination) bizProducer.getDestination();
            ActiveMQDestination shadowDestination = ActiveMQDestinationUtil.getInstance().mappingShadowDestination(destination);
            //ActiveMQMessageProducer producer = (ActiveMQMessageProducer) session.createProducer(shadowDestination);
            ActiveMQMessageProducer shadowProducer = Reflect.on(session).call("createProducer", shadowDestination).get();
            return new ActiveMQProducerResource(shadowProducer);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public boolean needRoute(Object target) {
        return true;
    }
}
