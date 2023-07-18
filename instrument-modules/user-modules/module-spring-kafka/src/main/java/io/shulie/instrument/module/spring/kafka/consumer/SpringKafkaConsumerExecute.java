package io.shulie.instrument.module.spring.kafka.consumer;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.bean.SyncObjectData;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowConsumerExecute;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfigWithData;
import io.shulie.instrument.module.spring.kafka.consumer.util.SpringKafkaUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
        Object containerProperties = ReflectionUtils.invoke(bizContainer, "getContainerProperties");
        String[] topics = ReflectionUtils.get(containerProperties, "topics");
        if (topics == null) {
            logger.error("not support spring-kafka topic type,TopicPartitions:{},TopicPattern:{}",
                    ReflectionUtils.get(containerProperties, "topicPartitions"),
                    ReflectionUtils.get(containerProperties, "topicPattern"));
            return null;
        }

        ConsumerFactory consumerFactory = SpringKafkaUtil.readConsumerFactory(bizContainer);
        if (consumerFactory == null) {
            logger.error("spring kafka not support! can not read consumerFactory!");
            return null;
        }

        List<ConsumerConfig> list = new ArrayList<ConsumerConfig>();

        for (String topic : topics) {
            SpringKafkaConsumerConfig config = new SpringKafkaConsumerConfig();
            config.setConsumerFactory(consumerFactory);
            config.setContainerProperties(containerProperties);
            config.setBizTopic(topic);

            String groupId = null;
            if (ReflectionUtils.existsField(containerProperties, "groupId")) {
                groupId = ReflectionUtils.get(containerProperties, "groupId");
            }
            if (groupId == null) {
                Map<String, Object> configs;
                if (ReflectionUtils.existsMethod(consumerFactory.getClass(), "getConfigurationProperties")) {
                    configs = ReflectionUtils.invoke(consumerFactory, "getConfigurationProperties");
                } else {
                    configs = ReflectionUtils.get(consumerFactory, "configs");
                }
                groupId = (String) configs.get("group.id");
            }

            config.setBizGroupId(groupId);
            list.add(config);
        }
        return list;
    }

    @Override
    public ShadowServer fetchShadowServer(List<ConsumerConfigWithData> dataList) {
        SpringKafkaConsumerConfig springKafkaConsumerConfig = (SpringKafkaConsumerConfig) dataList.get(0).getConsumerConfig();

        String[] bizTopics = new String[dataList.size()];
        for (int i = 0; i < dataList.size(); i++) {
            bizTopics[i] = ((SpringKafkaConsumerConfig) dataList.get(i).getConsumerConfig()).getBizTopic();
        }
        String bizGroupId = springKafkaConsumerConfig.getBizGroupId();
        Object properties = prepareContainerProperties(springKafkaConsumerConfig.getContainerProperties(), bizTopics, bizGroupId);

        Object consumerFactory = springKafkaConsumerConfig.getConsumerFactory();
        // spring-kafka 1.1.7版本
        if (!ReflectionUtils.existsField(consumerFactory, "listeners")) {
            Map<String, Object> configs = ReflectionUtils.get(consumerFactory, "configs");
            configs.put("group.id", addClusterTest(bizGroupId));
            consumerFactory = ReflectionUtils.newInstance(consumerFactory.getClass(),
                    configs,
                    ReflectionUtils.get(consumerFactory, "keyDeserializer"),
                    ReflectionUtils.get(consumerFactory, "valueDeserializer"));
        }

        KafkaMessageListenerContainer container = ReflectionUtils.newInstance(KafkaMessageListenerContainer.class, consumerFactory, properties);
        return new SpringKafkaShadowServer(container);
    }

    private Object prepareContainerProperties(Object bizContainerProperties, String[] bizTopics, String bizGroupId) {
        // 构建ContainerProperties有问题，曲线绕下
        Object containerProperties = ReflectionUtils.newInstance(bizContainerProperties.getClass(), Pattern.compile(addClusterTest(bizTopics[0])));
        ReflectionUtils.set(containerProperties, "topicPattern", null);
        ReflectionUtils.set(containerProperties, "topics", addClusterTest(bizTopics));

        BeanUtils.copyProperties(bizContainerProperties, containerProperties,
                "topics", "topicPartitions", "topicPattern", "ackCount", "ackTime", "subBatchPerPartition");

        if (ReflectionUtils.existsField(containerProperties, "groupId")) {
            ReflectionUtils.set(containerProperties, "groupId", addClusterTest(bizGroupId));
        }

        int ackCount = ReflectionUtils.get(bizContainerProperties, "ackCount");
        if (ackCount > 0) {
            ReflectionUtils.set(containerProperties, "ackCount", ackCount);
        }

        int ackTime = ReflectionUtils.get(bizContainerProperties, "ackCount");
        if (ackTime > 0) {
            ReflectionUtils.set(containerProperties, "ackTime", ackTime);
        }

        // spring-kafka 2.2.12.release没有getSubBatchPerPartition方法
        try {
            if (ReflectionUtils.existsMethod(bizContainerProperties.getClass(), "getSubBatchPerPartition")) {
                Boolean subBatchPerPartition = ReflectionUtils.invoke(bizContainerProperties, "getSubBatchPerPartition");
                if (subBatchPerPartition != null) {
                    ReflectionUtils.set(containerProperties, "subBatchPerPartition", subBatchPerPartition);
                }
            }
        } catch (Throwable t) {
            //
        }

        if (ReflectionUtils.existsMethod(containerProperties.getClass(), "getConsumerRebalanceListener")) {
            //自定义了listener， 就保留
            Object listener = ReflectionUtils.invoke(containerProperties, "getConsumerRebalanceListener");
            if (listener != null && !listener.getClass().getName().contains("org.springframework.kafka.listener.AbstractMessageListenerContainer")) {
                ReflectionUtils.set(containerProperties, "consumerRebalanceListener", listener);
            }
        }
        return containerProperties;
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
