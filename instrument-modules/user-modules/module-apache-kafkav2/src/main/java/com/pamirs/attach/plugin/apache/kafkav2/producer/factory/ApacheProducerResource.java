package com.pamirs.attach.plugin.apache.kafkav2.producer.factory;

import io.shulie.instrument.module.isolation.resource.ShadowResourceLifecycle;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author Licey
 * @date 2022/8/2
 */
public class ApacheProducerResource implements ShadowResourceLifecycle {
    private static final Logger logger = LoggerFactory.getLogger(ApacheProducerResource.class);
    private KafkaProducer kafkaProducer;

    public void setKafkaProducer(KafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    @Override
    public Object getTarget() {
        return kafkaProducer;
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
        kafkaProducer.close(timeout, TimeUnit.SECONDS);
    }
}
