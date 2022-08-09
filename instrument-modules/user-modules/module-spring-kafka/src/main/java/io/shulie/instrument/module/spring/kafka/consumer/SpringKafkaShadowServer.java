package io.shulie.instrument.module.spring.kafka.consumer;

import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;

/**
 * @author Licey
 * @date 2022/7/28
 */
public class SpringKafkaShadowServer implements ShadowServer {
    private KafkaMessageListenerContainer listenerContainer;

    public SpringKafkaShadowServer(KafkaMessageListenerContainer listenerContainer) {
        this.listenerContainer = listenerContainer;
    }

    @Override
    public Object getShadowTarget() {
        return this.listenerContainer;
    }

    @Override
    public void start() {
        listenerContainer.start();
    }

    @Override
    public boolean isRunning() {
        return listenerContainer.isRunning();
    }

    @Override
    public void stop() {
        listenerContainer.stop();
    }
}
