package io.shulie.instrument.module.spring.kafka.consumer;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.bean.SyncObjectData;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowConsumerExecute;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfigWithData;
import io.shulie.instrument.module.messaging.exception.MessagingRuntimeException;
import io.shulie.instrument.module.spring.kafka.consumer.util.SpringKafkaUtil;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.support.TopicPartitionOffset;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Licey
 * @date 2022/7/28
 */
public class SpringKafkaConsumerExecute implements ShadowConsumerExecute {
    private static final Logger logger = LoggerFactory.getLogger(SpringKafkaConsumerExecute.class);

    @Override
    public List<ConsumerConfig> prepareConfig(SyncObjectData syncObjectData) {
        Object target = syncObjectData.getTarget();
        //noinspection rawtypes
        KafkaMessageListenerContainer bizContainer = (KafkaMessageListenerContainer) target;
        ContainerProperties containerProperties = bizContainer.getContainerProperties();

        String[] topics = containerProperties.getTopics();
        if (topics == null) {
            logger.error("not support spring-kafka topic type,TopicPartitions:{},TopicPattern:{}", containerProperties.getTopicPartitions(), containerProperties.getTopicPattern());
            return null;
        }

        List<ConsumerConfig> list = new ArrayList<ConsumerConfig>();

        for (String topic : topics) {
            SpringKafkaConsumerConfig config = new SpringKafkaConsumerConfig();
            ConsumerFactory consumerFactory = SpringKafkaUtil.readConsumerFactory(bizContainer);
            if (consumerFactory == null) {
                logger.error("spring kafka not support! can not read consumerFactory!");
                return null;
            }
            config.setConsumerFactory(consumerFactory);
            config.setContainerProperties(containerProperties);
            config.setBizTopic(topic);
            config.setBizGroupId(containerProperties.getGroupId());
            list.add(config);
        }
        return list;
    }

    @Override
    public ShadowServer fetchShadowServer(List<ConsumerConfigWithData> dataList) {
        SpringKafkaConsumerConfig springKafkaConsumerConfig = (SpringKafkaConsumerConfig) dataList.get(0).getConsumerConfig();

        ContainerProperties properties = prepareContainerProperties(
                springKafkaConsumerConfig.getContainerProperties()
                , dataList.stream().map((d) -> ((SpringKafkaConsumerConfig) d.getConsumerConfig()).getBizTopic()).collect(Collectors.toList()).toArray(new String[dataList.size()])
                , springKafkaConsumerConfig.getBizGroupId());
        KafkaMessageListenerContainer container = new KafkaMessageListenerContainer(springKafkaConsumerConfig.getConsumerFactory(), properties);
        return new SpringKafkaShadowServer(container);
    }

    private ContainerProperties prepareContainerProperties(ContainerProperties bizContainerProperties, String[] bizTopics, String bizGroupId) {
        ContainerProperties containerProperties = new ContainerProperties(addClusterTest(bizTopics));
//        if (bizContainerProperties.getTopics() != null) {
//            containerProperties = new ContainerProperties(addClusterTest(bizContainerProperties.getTopics()));
//        } else if (bizContainerProperties.getTopicPattern() != null) {
//            containerProperties = new ContainerProperties(addClusterTest(bizContainerProperties.getTopicPattern()));
//        } else if (bizContainerProperties.getTopicPartitions() != null) {
//            containerProperties = new ContainerProperties(addClusterTest(bizContainerProperties.getTopicPartitions()));
//        } else {
//            throw new IllegalStateException("topics, topicPattern, or topicPartitions must be provided");
//        }

        BeanUtils.copyProperties(bizContainerProperties, containerProperties,
                "topics", "topicPartitions", "topicPattern", "ackCount", "ackTime", "subBatchPerPartition");

        containerProperties.setGroupId(addClusterTest(bizGroupId));

        if (bizContainerProperties.getAckCount() > 0) {
            containerProperties.setAckCount(bizContainerProperties.getAckCount());
        }
        if (containerProperties.getAckTime() > 0) {
            containerProperties.setAckTime(bizContainerProperties.getAckTime());
        }
        // spring-kafka 2.2.12.release没有getSubBatchPerPartition方法
        try {
            Boolean subBatchPerPartition = bizContainerProperties.getSubBatchPerPartition();
            if (subBatchPerPartition != null) {
                containerProperties.setSubBatchPerPartition(subBatchPerPartition);
            }
        } catch (Throwable t) {
            //
        }

        if (containerProperties.getConsumerRebalanceListener() != null) {
            //自定义了listener， 就保留
            ConsumerRebalanceListener listener = containerProperties.getConsumerRebalanceListener();
            if (!listener.getClass().getName().contains("org.springframework.kafka.listener.AbstractMessageListenerContainer")) {
                containerProperties.setConsumerRebalanceListener(listener);
            }
        }
        return containerProperties;
    }

    private TopicPartitionOffset[] addClusterTest(TopicPartitionOffset[] topicPartitions) {
        TopicPartitionOffset[] offsets = new TopicPartitionOffset[topicPartitions.length];
        for (int i = 0; i < topicPartitions.length; i++) {
            String topic = addClusterTest(topicPartitions[i].getTopic());
            int partition = topicPartitions[i].getPartition();
            TopicPartitionOffset.SeekPosition position = topicPartitions[i].getPosition();
            Long offset = topicPartitions[i].getOffset();
            offsets[i] = new TopicPartitionOffset(topic, partition, offset, position);
            offsets[i].setRelativeToCurrent(topicPartitions[i].isRelativeToCurrent());
        }
        return offsets;
    }

    private Pattern addClusterTest(Pattern topicPattern) {
        throw new MessagingRuntimeException("not support topicPattern!");
    }

    private String[] addClusterTest(String[] data) {
        String[] ctData = new String[data.length];
        for (int i = 0; i < data.length; i++) {
            ctData[i] = Pradar.addClusterTestPrefix(data[i]);
        }
        return ctData;
    }

    private String addClusterTest(String data) {
        return Pradar.addClusterTestPrefix(data);
    }

}
