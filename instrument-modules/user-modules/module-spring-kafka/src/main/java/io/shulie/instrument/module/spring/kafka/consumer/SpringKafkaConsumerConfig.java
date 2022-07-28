package io.shulie.instrument.module.spring.kafka.consumer;

import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;
import org.apache.commons.lang.StringUtils;
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

    private TopicPartitionOffset[] topicPartitions;

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

    @Override
    public String keyOfConfig() {
        String[] topics = containerProperties.getTopics();
        String groupId = containerProperties.getGroupId();
        if (topics == null || topics.length == 0) {
            return null;
        }
        return topics[0] + "#" + groupId;
    }
}
