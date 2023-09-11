package io.shulie.instrument.module.spring.kafka.consumer.util;

import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Licey
 * @date 2022/7/28
 */
public class SpringKafkaUtil {
    private static final Logger logger = LoggerFactory.getLogger(SpringKafkaUtil.class);

    private static Map<String, Field> fieldMap = new ConcurrentHashMap<String, Field>();

    public static ConsumerFactory readConsumerFactory(KafkaMessageListenerContainer container) {
        return (ConsumerFactory) read(container, "consumerFactory");
    }

    public static Object read(Object obj, String fieldName) {
        String key = obj.getClass().getClassLoader() + "#" + obj.getClass().getName() + "#" + fieldName;
        Field field = fieldMap.get(key);
        try {
            if (field == null) {
                field = Reflect.on(obj).field0(fieldName);
                field.setAccessible(true);
                fieldMap.put(key, field);
            }
            return field.get(obj);
        } catch (Throwable e) {
            logger.warn("read field {} fail!", fieldName, e);
        }
        return null;
    }

}
