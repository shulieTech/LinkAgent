package io.shulie.instrument.module.spring.kafka.consumer;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;
import org.springframework.kafka.core.ConsumerFactory;

import java.util.Map;

/**
 * @author Licey
 * @date 2022/7/28
 */
public class SpringKafkaConsumerConfig extends ConsumerConfig {
    private Object consumerFactory;
    private Object containerProperties;

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

    public Object getContainerProperties() {
        return containerProperties;
    }

    public void setContainerProperties(Object containerProperties) {
        this.containerProperties = containerProperties;
    }

    public Object getConsumerFactory() {
        return consumerFactory;
    }

    public void setConsumerFactory(Object consumerFactory) {
        this.consumerFactory = consumerFactory;
    }

    @Override
    public String keyOfConfig() {
        return bizTopic + "#" + bizGroupId;
    }

    @Override
    public String keyOfServer() {
        Map<String, Object> configs = ReflectionUtils.get(consumerFactory, "configs");
        return String.valueOf(configs.get("bootstrap.servers"));
    }
}
