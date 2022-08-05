package com.pamirs.attach.plugin.apache.kafkav2.producer.proxy;

import com.pamirs.pradar.Pradar;
import io.shulie.instrument.module.isolation.proxy.ShadowMethodProxy;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author Licey
 * @date 2022/8/2
 */
public class SendOffsetsToTransactionMethodProxy implements ShadowMethodProxy {
    @Override
    public Object executeMethod(Object shadowTarget, Method method, Object... args) throws Exception {
        if (args.length > 0) {
            Map<TopicPartition, OffsetAndMetadata> offsets = (Map<TopicPartition, OffsetAndMetadata>) args[0];
            for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : offsets.entrySet()) {
                if (!Pradar.isClusterTestPrefix(entry.getKey().topic())) {
                    throw new RuntimeException("kafka SendOffsetsToTransaction send topic:" + entry.getKey().topic() + " with cluster Test!");
                }
            }
        }
        throw new RuntimeException("not support method: kafka SendOffsetsToTransaction!");
    }
}
