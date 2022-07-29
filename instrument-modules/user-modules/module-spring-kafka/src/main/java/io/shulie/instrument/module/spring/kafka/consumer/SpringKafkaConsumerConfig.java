package io.shulie.instrument.module.spring.kafka.consumer;

import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.TopicPartitionOffset;

/**
 * @author Licey
 * @date 2022/7/28
 */
public class SpringKafkaConsumerConfig extends ConsumerConfig {
    private ConsumerFactory consumerFactory;
    private ContainerProperties containerProperties;
    private String topic;
    private TopicPartitionOffset[] topicPartitions;

    @Override
    public String keyOfConfig() {
        return topic + "#" + containerProperties.getGroupId();
    }

    public TopicPartitionOffset[] getTopicPartitions() {
        return topicPartitions;
    }

    public void setTopicPartitions(TopicPartitionOffset[] topicPartitions) {
        this.topicPartitions = topicPartitions;
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

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
