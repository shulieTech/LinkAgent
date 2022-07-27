package io.shulie.instrument.module.messaging.consumer.module;

import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;

/**
 * @author Licey
 * @date 2022/7/27
 */
public class ShadowConsumer {
    private ConsumerConfig consumerConfig;
    private ShadowServer shadowServer;
    private boolean started;

    public ShadowConsumer(ConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    public ConsumerConfig getConsumerConfig() {
        return consumerConfig;
    }

    public void setConsumerConfig(ConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    public ShadowServer getShadowServer() {
        return shadowServer;
    }

    public void setShadowServer(ShadowServer shadowServer) {
        this.shadowServer = shadowServer;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }
}
