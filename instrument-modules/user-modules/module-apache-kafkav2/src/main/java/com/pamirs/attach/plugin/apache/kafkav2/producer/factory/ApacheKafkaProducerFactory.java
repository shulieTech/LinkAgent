package com.pamirs.attach.plugin.apache.kafkav2.producer.factory;

import com.pamirs.pradar.Pradar;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import io.shulie.instrument.module.isolation.resource.ShadowResourceLifecycle;
import io.shulie.instrument.module.isolation.resource.ShadowResourceProxyFactory;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serializer;

import java.util.HashMap;
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
        Serializer keySerializer = Reflect.on(bizProducer).field("keySerializer").get();
        Serializer valueSerializer = Reflect.on(bizProducer).field("valueSerializer").get();
        HashMap<Object, Object> config = new HashMap<>(originals);
        if(config.get("transactional.id") != null){
            String txId = config.get("transactional.id").toString();
            String shadowTxId = Pradar.getClusterTestPrefix() + txId;
            config.put("transactional.id",shadowTxId);
        }
        KafkaProducer producer = new KafkaProducer(config, keySerializer, valueSerializer);
        resource.setKafkaProducer(producer);
        return resource;
    }

    @Override
    public boolean needRoute(Object target) {
        return true;
    }
}
