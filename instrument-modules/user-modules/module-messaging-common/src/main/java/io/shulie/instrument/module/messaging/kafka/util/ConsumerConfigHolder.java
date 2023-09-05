package io.shulie.instrument.module.messaging.kafka.util;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.SyncObjectService;
import com.pamirs.pradar.bean.SyncObject;
import com.pamirs.pradar.bean.SyncObjectData;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.*;

public class ConsumerConfigHolder {

    // 缓存kafka消费者的实际 "bootstrap.servers" 配置, 如果是ip且机器配置了host的域名，在消费者启动后通过反射获取时可能会变成域名，避免这种情况
    private static Map<Integer, String> bootstrapServersMappings = new HashMap<>();
    private static Map<Integer, Set<String>> subscriptions = new HashMap<>();


    public static String getBootstrapServers(KafkaConsumer consumer) {
        refreshBootstrapServersCaches();
        int hashCode = System.identityHashCode(consumer);
        // 从业务消费者里取servers
        String servers = bootstrapServersMappings.get(hashCode);
        if(servers == null){
            Set<String> subscription = ReflectionUtils.getFieldValues(consumer, "subscriptions", "subscription");
            String topic = subscription.iterator().next();
            for (Map.Entry<Integer, Set<String>> entry : subscriptions.entrySet()) {
                if(entry.getValue().contains(Pradar.removeClusterTestPrefix(topic))){
                    servers = bootstrapServersMappings.get(entry.getKey());
                    bootstrapServersMappings.put(hashCode, servers);
                }
            }
        }
        return servers;
    }

    public static void refreshBootstrapServersCaches() {
        SyncObject syncObject = SyncObjectService.removeSyncObject("org.apache.kafka.clients.consumer.KafkaConsumer");
        if (syncObject == null || syncObject.getDatas().isEmpty()) {
            return;
        }

        for (SyncObjectData data : syncObject.getDatas()) {
            KafkaConsumer consumer = (KafkaConsumer) data.getTarget();
            Integer hashCode = System.identityHashCode(consumer);

            if (bootstrapServersMappings.containsKey(hashCode)) {
                continue;
            }

            // 获取消费者配置信息
            ConsumerConfig config = null;
            for (Object arg : data.getArgs()) {
                if (arg instanceof ConsumerConfig) {
                    config = (ConsumerConfig) arg;
                    break;
                }
            }
            if (config == null) {
                continue;
            }

            List<String> servers = config.getList(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG);
            Collections.sort(servers);
            String serverString = servers.toString();
            bootstrapServersMappings.put(hashCode, serverString.substring(1, serverString.length() - 1));

            Set<String> subscription = ReflectionUtils.getFieldValues(consumer, "subscriptions", "subscription");
            subscriptions.put(hashCode, subscription);
        }
    }

}
