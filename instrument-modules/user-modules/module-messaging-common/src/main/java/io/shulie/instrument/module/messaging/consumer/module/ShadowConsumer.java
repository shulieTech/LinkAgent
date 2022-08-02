package io.shulie.instrument.module.messaging.consumer.module;

import io.shulie.instrument.module.messaging.consumer.execute.ShadowConsumerExecute;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;

/**
 * @author Licey
 * @date 2022/7/27
 */
public class ShadowConsumer {
    private ConsumerConfig consumerConfig;
    private ShadowConsumerExecute consumerExecute;
    private ShadowServer shadowServer;

    private ClassLoader bizClassLoad;
    private boolean started;

    public ShadowConsumer(ConsumerConfig consumerConfig, ShadowConsumerExecute consumerExecute,ClassLoader bizClassLoad) {
        this.consumerConfig = consumerConfig;
        this.consumerExecute = consumerExecute;
        this.bizClassLoad = bizClassLoad;
    }

    public ShadowConsumerExecute getConsumerExecute() {
        return consumerExecute;
    }

    public void setConsumerExecute(ShadowConsumerExecute consumerExecute) {
        this.consumerExecute = consumerExecute;
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
