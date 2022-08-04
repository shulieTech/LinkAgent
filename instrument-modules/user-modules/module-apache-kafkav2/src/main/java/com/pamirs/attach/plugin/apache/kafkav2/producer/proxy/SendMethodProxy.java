package com.pamirs.attach.plugin.apache.kafkav2.producer.proxy;

import com.pamirs.pradar.Pradar;
import io.shulie.instrument.module.isolation.proxy.ShadowMethodProxy;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * @author Licey
 * @date 2022/8/2
 */
public class SendMethodProxy implements ShadowMethodProxy {
    private static final Logger logger = LoggerFactory.getLogger(SendMethodProxy.class);

    @Override
    public Object executeMethod(Object shadowTarget, Method method, Object... args) {
        if (args != null && args.length > 0) {
            KafkaProducer kafkaProducer = (KafkaProducer) shadowTarget;
            ProducerRecord bizRecord = (ProducerRecord) args[0];
            ProducerRecord shadowProducerRecord = new ProducerRecord(
                    Pradar.addClusterTestPrefix(bizRecord.topic()),
                    null,
                    null,
                    bizRecord.key(),
                    bizRecord.value(),
                    bizRecord.headers());
            if (args.length == 1) {
                return kafkaProducer.send(shadowProducerRecord);
            } else if (args.length == 2) {
                return kafkaProducer.send(shadowProducerRecord, (Callback) args[1]);
            }
        }
        throw new RuntimeException("not support apache-kafka version!");
    }
}
