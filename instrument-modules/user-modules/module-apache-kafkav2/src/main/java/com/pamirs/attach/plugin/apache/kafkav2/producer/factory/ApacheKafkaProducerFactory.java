package com.pamirs.attach.plugin.apache.kafkav2.producer.factory;

import com.shulie.instrument.simulator.api.reflect.Reflect;
import io.shulie.instrument.module.isolation.resource.ShadowResourceLifecycle;
import io.shulie.instrument.module.isolation.resource.ShadowResourceProxyFactory;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

/**
 * @author Licey
 * @date 2022/8/2
 */
public class ApacheKafkaProducerFactory implements ShadowResourceProxyFactory {
    @Override
    public ShadowResourceLifecycle createShadowResource(Object bizTarget) {
        if (!(bizTarget instanceof KafkaProducer)) {
            return null;
        }
        KafkaProducer bizProducer = (KafkaProducer) bizTarget;
        ApacheProducerResource resource = new ApacheProducerResource();

        ProducerConfig producerConfig = Reflect.on(bizProducer).field("producerConfig").get();
        Map originals = Reflect.on(producerConfig).field("originals").get();
        Serializer keySerializer = Reflect.on(producerConfig).field("keySerializer").get();
        Serializer valueSerializer = Reflect.on(producerConfig).field("valueSerializer").get();

        KafkaProducer producer = new KafkaProducer(originals, keySerializer, valueSerializer);
        resource.setKafkaProducer(producer);
        return resource;
    }
}
