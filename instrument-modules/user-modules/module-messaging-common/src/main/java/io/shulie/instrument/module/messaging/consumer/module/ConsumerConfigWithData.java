package io.shulie.instrument.module.messaging.consumer.module;

/**
 * @author Licey
 * @date 2022/8/4
 */
public class ConsumerConfigWithData {
    private ConsumerConfig consumerConfig;

    public ConsumerConfigWithData(ConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    public ConsumerConfig getConsumerConfig() {
        return consumerConfig;
    }

    public void setConsumerConfig(ConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

}
