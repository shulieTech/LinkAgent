package io.shulie.instrument.module.spring.kafka.consumer;

import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

/**
 * @author Licey
 * @date 2022/7/28
 */
public class SpringKafkaConsumerConfig extends ConsumerConfig {
    private ConsumerFactory consumerFactory;
    private ContainerProperties containerProperties;

    private String bizTopic;
    private String bizGroupId;

    public String getBizTopic() {
        return bizTopic;
    }

    public void setBizTopic(String bizTopic) {
        this.bizTopic = bizTopic;
    }

    public String getBizGroupId() {
        return bizGroupId;
    }

    public void setBizGroupId(String bizGroupId) {
        this.bizGroupId = bizGroupId;
    }

    public ContainerProperties getContainerProperties() {
        return containerProperties;
    }

    public void setContainerProperties(ContainerProperties containerProperties) {
        this.containerProperties = containerProperties;
    }

    public ConsumerFactory getConsumerFactory() {
        return consumerFactory;
    }

    public void setConsumerFactory(ConsumerFactory consumerFactory) {
        this.consumerFactory = consumerFactory;
    }

    @Override
    public String keyOfConfig() {
        return bizTopic + "#" + bizGroupId;
    }

    @Override
    public String keyOfServer() {
        return "";
    }
}
