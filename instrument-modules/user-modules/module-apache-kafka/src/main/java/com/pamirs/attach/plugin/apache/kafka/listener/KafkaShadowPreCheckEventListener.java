package com.pamirs.attach.plugin.apache.kafka.listener;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.SyncObjectService;
import com.pamirs.pradar.bean.SyncObject;
import com.pamirs.pradar.bean.SyncObjectData;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.preparation.ShadowMqPreCheckEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.apache.kafka.common.security.JaasContext;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class KafkaShadowPreCheckEventListener implements PradarEventListener {

    public final static String REFLECT_FIELD_GROUP_ID = "groupId";
    public final static String REFLECT_FIELD_COORDINATOR = "coordinator";

    /**
     * 检查过的成功的topic#group
     */
    private static Set<String> successCheckedTopicGroups = new HashSet<String>();

    private final static Logger LOGGER = LoggerFactory.getLogger(KafkaShadowPreCheckEventListener.class.getName());

    @Override
    public EventResult onEvent(IEvent iEvent) {
        if (!(iEvent instanceof ShadowMqPreCheckEvent)) {
            return EventResult.IGNORE;
        }
        ShadowMqPreCheckEvent event = (ShadowMqPreCheckEvent) iEvent;
        String type = event.getType();
        if (!"KAFKA".equals(type)) {
            return EventResult.IGNORE;
        }

        Map<String, List<String>> topicGroups = event.getTopicGroups();
        Map<String, String> result = new HashMap<String, String>();

        SyncObject syncObject = SyncObjectService.getSyncObject("org.apache.kafka.clients.consumer.KafkaConsumer#subscribe");
        if (syncObject == null) {
            LOGGER.error("[apache-kafka] handler shadow mq precheck event failed because all business consumer doesn't exists!");
            for (Map.Entry<String, List<String>> entry : topicGroups.entrySet()) {
                String topic = entry.getKey();
                for (String group : entry.getValue()) {
                    result.put(topic + "#" + group, "请在应用启动参数内加 -Dagent.sync.module.enable=true 参数启动探针sync模块");
                }
            }
            event.handlerResult(result);
            return EventResult.success("[apache-kafka]: handler shadow mq preCheck event success.");
        }

        for (Map.Entry<String, List<String>> entry : topicGroups.entrySet()) {
            doCheckTopicGroups(entry.getKey(), entry.getValue(), result);
        }
        event.handlerResult(result);
        return EventResult.success("[apache-kafka]: handler shadow mq preCheck event success.");
    }

    private void doCheckTopicGroups(String topic, List<String> groups, Map<String, String> result) {
        SyncObject syncObject = SyncObjectService.getSyncObject("org.apache.kafka.clients.consumer.KafkaConsumer#subscribe");
        Object consumer = null;
        for (String group : groups) {
            for (SyncObjectData data : syncObject.getDatas()) {
                Object target = data.getTarget();
                String groupId = getGroup(target);
                if (group.equals(groupId)) {
                    consumer = target;
                    break;
                }
            }
            if (consumer == null) {
                LOGGER.error("[apache-kafka] handler shadow mq precheck event failed because can,t find business consumer!");
                result.put(topic + "#" + group, String.format("topic:%s, group:%s 找不到对应的业务消费者", topic, group));
                continue;
            }
            Thread.currentThread().setContextClassLoader(consumer.getClass().getClassLoader());
            doCheckTopicGroup((KafkaConsumer) consumer, topic, group, result);
        }
    }

    private void doCheckTopicGroup(KafkaConsumer consumer, String topic, String group, Map<String, String> result) {

        String key = topic + "#" + group;
        if (successCheckedTopicGroups.contains(key)) {
            result.put(key, "success");
            return;
        }

        String ptTopic = Pradar.addClusterTestPrefix(topic);
        String ptGroup = Pradar.addClusterTestPrefix(group);

        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers(consumer));
        AdminClient adminClient = AdminClient.create(properties);

        if (isTopicExists(adminClient, ptTopic) && isGroupExists(adminClient, ptGroup)) {
            successCheckedTopicGroups.add(key);
            result.put(key, "success");
            return;
        }

        try {
            KafkaConsumer ptConsumer = createShadowConsumer(consumer, ptTopic);
            boolean topicExists = isTopicExists(adminClient, ptTopic);
            boolean groupExists = isGroupExists(adminClient, ptGroup);
            if (topicExists && groupExists) {
                successCheckedTopicGroups.add(key);
                result.put(key, "success");
                return;
            }
            if (!topicExists) {
                LOGGER.error("[apache-kafka] handler shadow mq precheck event failed, create topic {} failed!", ptTopic);
                result.put(key, "自动创建topic:" + ptTopic + "失败,请手动创建影子topic");
            } else {
                LOGGER.error("[apache-kafka] handler shadow mq precheck event failed, create group {} failed!", ptGroup);
                result.put(key, "自动创建group:" + ptGroup + "失败,请手动创建影子group");
            }
            ptConsumer.close();
        } catch (Exception e) {
            LOGGER.error("[apache-kafka] handler shadow mq precheck event failed", e);
        }

    }

    private boolean isTopicExists(AdminClient adminClient, String topic) {
        try {
            Collection<TopicListing> topicListings = adminClient.listTopics().listings().get(3, TimeUnit.SECONDS);
            for (TopicListing listing : topicListings) {
                if (topic.equals(listing.name())) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            LOGGER.error("[apache-kafka] handler shadow mq precheck event failed, list topics occur exception", e);
            return false;
        }
    }

    private boolean isGroupExists(AdminClient adminClient, String group) {
        try {
            Method method = ReflectionUtils.findMethod(adminClient.getClass(), "listConsumerGroups");
            if (method == null) {
                LOGGER.info("[apache-kafka] lower kafka version not support listConsumerGroups, ignore check group exists!");
                return true;
            }
            Object result = ReflectionUtils.invokeMethod(method, adminClient);
            if (result == null) {
                return false;
            }
            KafkaFutureImpl future = ReflectionUtils.get(result, "valid");
            Collection groupListing = (Collection) future.get(3, TimeUnit.SECONDS);
            for (Object obj : groupListing) {
                String groupId = ReflectionUtils.get(obj, "groupId");
                if (group.equals(groupId)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            LOGGER.error("[apache-kafka] handler shadow mq precheck event failed, list groups occur exception", e);
            return false;
        }
    }


    /**
     * 获取consumer对应的groupId
     *
     * @param bizConsumer 业务consumer
     * @return group
     */
    private String getGroup(Object bizConsumer) {
        try {
            Object coordinator = ReflectionUtils.get(bizConsumer, REFLECT_FIELD_COORDINATOR);
            Field field = ReflectionUtils.findField(bizConsumer.getClass(), REFLECT_FIELD_GROUP_ID);
            Object groupId = null;
            if (field != null) {
                groupId = ReflectionUtils.get(bizConsumer, REFLECT_FIELD_GROUP_ID);
            }
            if (groupId == null) {
                groupId = ReflectionUtils.get(coordinator, REFLECT_FIELD_GROUP_ID);
                if (groupId == null) {
                    throw new PressureMeasureError("未支持的kafka版本！未能获取groupId");
                }
            }
            String groupIdStr = "";
            if (groupId instanceof String) {
                groupIdStr = (String) groupId;
            } else {
                groupIdStr = ReflectionUtils.get(groupId, "value");
            }
            return groupIdStr;
        } catch (ReflectException e) {
            throw new PressureMeasureError(e);
        }
    }

    @Override
    public int order() {
        return 31;
    }

    /**
     * 创建影子kafka consumer
     *
     * @return KafkaConsumer
     */
    private KafkaConsumer createShadowConsumer(KafkaConsumer bizConsumer, String topic) {
        Properties config = new Properties();
        Object coordinator = Reflect.on(bizConsumer).get("coordinator");
        Object client = Reflect.on(bizConsumer).get("client");
        Object kafkaClient = Reflect.on(client).get("client");
        Object fetcher = Reflect.on(bizConsumer).get("fetcher");
        Object metadata = Reflect.on(bizConsumer).get("metadata");
        Object keyDeserializer = Reflect.on(bizConsumer).get("keyDeserializer");
        Object valueDeserializer = Reflect.on(bizConsumer).get("valueDeserializer");

        if (keyDeserializer != null) {
            config.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer.getClass());
        }
        if (valueDeserializer != null) {
            config.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer.getClass());
        }
        config.put(org.apache.kafka.clients.consumer.ConsumerConfig.CLIENT_ID_CONFIG,
                Pradar.addClusterTestPrefix(ReflectionUtils.get(bizConsumer, "clientId")));
        config.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers(bizConsumer));
        config.put(org.apache.kafka.clients.consumer.ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, (getAllowMaxLag() * 2 * 3) + "");
        config.put(org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,
                (getAllowMaxLag() * 2 + 5000) + "");
        config.put(org.apache.kafka.clients.consumer.ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, (getAllowMaxLag() * 3) + "");
        putSlience(config, org.apache.kafka.clients.consumer.ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, bizConsumer, "requestTimeoutMs");
        putSlience(config, org.apache.kafka.clients.consumer.ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, bizConsumer, "retryBackoffMs");

        /**
         * 认证配置
         */
        Object selector = ReflectionUtils.get(kafkaClient, "selector");
        Object channelBuilder = ReflectionUtils.get(selector, "channelBuilder");
        if (channelBuilder.getClass().getName().equals("org.apache.kafka.common.network.SaslChannelBuilder")) {
            String clientSaslMechanism = ReflectionUtils.get(channelBuilder, "clientSaslMechanism");
            config.put(SaslConfigs.SASL_MECHANISM, clientSaslMechanism);
            config.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG,
                    ReflectionUtils.get(channelBuilder, "securityProtocol").toString());
            if (clientSaslMechanism != null && !"".equals(clientSaslMechanism)) {
                Map jaasContexts = ReflectionUtils.get(channelBuilder, "jaasContexts");
                if (jaasContexts == null) {
                    throw new RuntimeException("未支持的kafka版本，无法获取jaasContexts");
                }
                JaasContext jaasContext = (JaasContext) jaasContexts.get(clientSaslMechanism);
                if (jaasContext != null) {
                    String password = jaasContext.dynamicJaasConfig().value();
                    config.put(SaslConfigs.SASL_JAAS_CONFIG, password);
                } else {
                    LOGGER.warn("[apache-kafka] business kafka consumer using sasl but jaasContext not found jaasContexts is : {}",
                            jaasContexts);
                }
            } else {
                LOGGER.warn("[apache-kafka] business kafka consumer using sasl but clientSaslMechanism is blank");
            }
        }

        Object interceptors = ReflectionUtils.get(bizConsumer, "interceptors");
        if (interceptors != null) {
            List list = ReflectionUtils.get(interceptors, "interceptors");
            if (list != null && list.size() > 0) {
                List classList = new ArrayList();
                for (Object o : list) {
                    classList.add(o.getClass());
                }
                putSlience(config, org.apache.kafka.clients.consumer.ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, classList);
            }
        }

        putSlience(config, org.apache.kafka.clients.consumer.ConsumerConfig.METADATA_MAX_AGE_CONFIG, metadata, "metadataExpireMs");

        putSlience(config, org.apache.kafka.clients.consumer.ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, kafkaClient, "reconnectBackoffMs");
        putSlience(config, org.apache.kafka.clients.consumer.ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, kafkaClient, "reconnectBackoffMax");
        putSlience(config, org.apache.kafka.clients.consumer.ConsumerConfig.SEND_BUFFER_CONFIG, kafkaClient, "socketSendBuffer");
        putSlience(config, org.apache.kafka.clients.consumer.ConsumerConfig.RECEIVE_BUFFER_CONFIG, kafkaClient, "socketReceiveBuffer");
        putSlience(config, org.apache.kafka.clients.consumer.ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, kafkaClient, "requestTimeoutMs");

        Object subscriptions = ReflectionUtils.get(bizConsumer, "subscriptions");
        if (subscriptions != null) {
            Object defaultResetStrategy = ReflectionUtils.get(subscriptions, "defaultResetStrategy");
            if (defaultResetStrategy != null) {
                putSlience(config, org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                        defaultResetStrategy.toString().toLowerCase(Locale.ROOT));
            }
        }

        copyHeartbeatConfig(config, coordinator);

        config.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, Pradar.addClusterTestPrefix(getGroup(bizConsumer)));
        putSlience(config, org.apache.kafka.clients.consumer.ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, coordinator, "sessionTimeoutMs");
        putSlience(config, org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, coordinator, "autoCommitEnabled");
        putSlience(config, org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, coordinator, "autoCommitIntervalMs");
        putSlience(config, org.apache.kafka.clients.consumer.ConsumerConfig.EXCLUDE_INTERNAL_TOPICS_CONFIG, coordinator, "excludeInternalTopics");

        putSlience(config, org.apache.kafka.clients.consumer.ConsumerConfig.FETCH_MIN_BYTES_CONFIG, fetcher, "minBytes");
        putSlience(config, org.apache.kafka.clients.consumer.ConsumerConfig.FETCH_MAX_BYTES_CONFIG, fetcher, "maxBytes");
        putSlience(config, org.apache.kafka.clients.consumer.ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, fetcher, "maxWaitMs");
        putSlience(config, org.apache.kafka.clients.consumer.ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, fetcher, "fetchSize");
        putSlience(config, org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG, fetcher, "maxPollRecords");
        putSlience(config, org.apache.kafka.clients.consumer.ConsumerConfig.CHECK_CRCS_CONFIG, fetcher, "checkCrcs");

        KafkaConsumer kafkaConsumer;
        try {
            kafkaConsumer = new KafkaConsumer(config, (Deserializer) keyDeserializer,
                    (Deserializer) valueDeserializer);
        } catch (Exception e) {
            kafkaConsumer = new KafkaConsumer(config);
        }

        Object ptInterceptors = Reflect.on(kafkaConsumer).get("interceptors");
        List list = null;
        if (Reflect.on(ptInterceptors).existsField("interceptors")) {
            list = Reflect.on(ptInterceptors).get("interceptors");
        }
        if ((list == null || list.isEmpty()) && interceptors != null) {
            LOGGER.info("[apache-kafka] set kafka biz interceptors to pt consumer:{}", interceptors);
            Reflect.on(kafkaConsumer).set("interceptors", interceptors);
        }

        kafkaConsumer.subscribe(Collections.singletonList(topic));
        return kafkaConsumer;
    }

    private long getAllowMaxLag() {
        long maxLagMillSecond = TimeUnit.SECONDS.toMillis(3);
        String maxLagMillSecondStr = System.getProperty("shadow.kafka.maxLagMillSecond");
        if (!StringUtils.isEmpty(maxLagMillSecondStr)) {
            try {
                maxLagMillSecond = Long.parseLong(maxLagMillSecondStr);
            } catch (NumberFormatException ignore) {
            }
        }
        return maxLagMillSecond;
    }

    private void putSlience(Properties config, String configStr, Object value) {
        try {
            config.put(configStr, value.toString());
        } catch (ReflectException ignore) {
        }
    }

    private void putSlience(Properties config, String configStr, Object obj, String name) {
        try {
            Object value = Reflect.on(obj).get(name).toString();
            config.put(configStr, value);
        } catch (ReflectException ignore) {
        }
    }

    private void copyHeartbeatConfig(Properties config, Object coordinator) {
        try {
            Object heartbeat = Reflect.on(coordinator).get("heartbeat");
            if (Reflect.on(heartbeat).existsField("rebalanceConfig")) {
                heartbeat = Reflect.on(heartbeat).get("rebalanceConfig");
            }
            config.put(org.apache.kafka.clients.consumer.ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, Reflect.on(heartbeat).get("sessionTimeoutMs"));
            config.put(org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, Reflect.on(heartbeat).get("rebalanceTimeoutMs"));
            config.put(org.apache.kafka.clients.consumer.ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, Reflect.on(heartbeat).get("heartbeatIntervalMs"));
        } catch (Exception e) {
            //
        }
    }

    public static String getBootstrapServers(KafkaConsumer<?, ?> consumer) {
        Object metadata = Reflect.on(consumer).get("metadata");
        Field clusterField = ReflectionUtils.findField(metadata.getClass(), "cluster");
        Object cluster = clusterField != null ? ReflectionUtils.get(metadata, "cluster") : null;
        Iterable<Node> nodes;
        if (cluster != null) {
            nodes = Reflect.on(cluster).get("nodes");
        } else {
            Object cache = ReflectionUtils.get(metadata, "cache");
            if (cache != null) {
                Object tmpNodes = Reflect.on(cache).get("nodes");
                if (tmpNodes instanceof Iterable) {
                    nodes = (Iterable<Node>) tmpNodes;
                } else if (tmpNodes instanceof Map) {
                    nodes = ((Map<?, Node>) tmpNodes).values();
                } else {
                    throw new PressureMeasureError("未支持的kafka版本！未能获取nodes");
                }
            } else {
                throw new PressureMeasureError("未支持的kafka版本！未能获取nodes");
            }
        }
        StringBuilder sb = new StringBuilder();
        for (Node node : nodes) {
            sb.append(Reflect.on(node).get("host").toString()).append(":").append(Reflect.on(node).get("port")
                    .toString()).append(",");
        }
        return sb.substring(0, sb.length() - 1);
    }

}
