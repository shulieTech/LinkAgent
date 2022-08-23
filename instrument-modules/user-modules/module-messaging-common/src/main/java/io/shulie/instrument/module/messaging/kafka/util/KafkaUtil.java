/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.shulie.instrument.module.messaging.kafka.util;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import io.shulie.instrument.module.messaging.kafka.ApacheKafkaHandler;
import io.shulie.instrument.module.messaging.kafka.model.KafkaConsumerMetaData;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.JaasContext;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/09/08 4:27 下午
 */
public class KafkaUtil {

    public final static String DYNAMIC_FIELD_REMOTE_ADDRESS = "remoteAddress";

    public final static String REFLECT_FIELD_COORDINATOR = "coordinator";

    public final static String REFLECT_FIELD_GROUP_ID = "groupId";

    public final static String UNKNOWN = "unknow";

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaUtil.class);

    private final static Map<Integer, KafkaConsumerMetaData> cache = new ConcurrentHashMap<>();

    public static String getRemoteAddress(Object remoteAddressFieldAccessor, DynamicFieldManager manager) {
        String remoteAddress = manager.getDynamicField(remoteAddressFieldAccessor, DYNAMIC_FIELD_REMOTE_ADDRESS);

        if (StringUtils.isEmpty(remoteAddress)) {
            if (remoteAddressFieldAccessor instanceof KafkaConsumer) {
                try {
                    remoteAddress = KafkaUtil.getBootstrapServers((KafkaConsumer<?, ?>) remoteAddressFieldAccessor);
                    manager.setDynamicField(remoteAddressFieldAccessor, DYNAMIC_FIELD_REMOTE_ADDRESS,
                            remoteAddress);
                    return remoteAddress;
                } catch (Throwable e) {
                    LOGGER.warn("can not get remoteAddress", e);
                }
            }
            return UNKNOWN;
        } else {
            return remoteAddress;
        }
    }

    public static String getBootstrapServers(KafkaConsumer<?, ?> consumer) {
        Object metadata = Reflect.on(consumer).get("metadata");

        Object cluster = ReflectUtil.reflectSlience(metadata, "cluster");
        Iterable<Node> nodes;
        if (cluster != null) {
            nodes = Reflect.on(cluster).get("nodes");
        } else {
            Object cache = ReflectUtil.reflectSlience(metadata, "cache");
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


    /**
     * 获取consumer对应的groupId
     *
     * @param bizConsumer 业务consumer
     * @return group
     */
    public static String getGroup(KafkaConsumer bizConsumer) {
        try {
            Object coordinator = Reflect.on(bizConsumer).get(REFLECT_FIELD_COORDINATOR);
            Object groupId = ReflectUtil.reflectSlience(bizConsumer, REFLECT_FIELD_GROUP_ID);
            if (groupId == null) {
                groupId = ReflectUtil.reflectSlience(coordinator, REFLECT_FIELD_GROUP_ID);
                if (groupId == null) {
                    throw new PressureMeasureError("未支持的kafka版本！未能获取groupId");
                }
            }
            String groupIdStr = "";
            if (groupId instanceof String) {
                groupIdStr = (String) groupId;
            } else {
                groupIdStr = ReflectUtil.reflectSlience(groupId, "value");
            }
            return groupIdStr;
        } catch (ReflectException e) {
            throw new PressureMeasureError(e);
        }
    }


    /**
     * 提取kafkaConsumer基本信息
     *
     * @param consumer KafkaConsumer
     * @return KafkaConsumerMetaData
     */
    public static KafkaConsumerMetaData getConsumerMetaData(KafkaConsumer consumer) {
        KafkaConsumerMetaData consumerMetaData = cache.get(System.identityHashCode(consumer));
        if (consumerMetaData == null) {
            synchronized (cache) {
                consumerMetaData = cache.get(System.identityHashCode(consumer));
                if (consumerMetaData == null) {
                    consumerMetaData = KafkaConsumerMetaData.build(consumer);
                    cache.put(System.identityHashCode(consumer), consumerMetaData);
                }
            }
        }
        return consumerMetaData;
    }

    public static Set<String> getTopics(KafkaConsumer bizConsumer) {
        return Reflect.on((Object) Reflect.on(bizConsumer).get("subscriptions")).call("subscription").get();
    }

    /**
     * 创建影子kafka consumer
     *
     * @param bizConsumer 配置信息
     * @return KafkaConsumer
     */
    public static KafkaConsumer createShadowConsumer(KafkaConsumer bizConsumer) {
        KafkaConsumerMetaData consumerMetaData = getConsumerMetaData(bizConsumer);
        Properties config = new Properties();
        Object coordinator = Reflect.on(bizConsumer).get("coordinator");
        Object client = Reflect.on(bizConsumer).get("client");
        Object kafkaClient = Reflect.on(client).get("client");
        Object fetcher = Reflect.on(bizConsumer).get("fetcher");
        Object metadata = Reflect.on(bizConsumer).get("metadata");
        Object keyDeserializer = Reflect.on(bizConsumer).get("keyDeserializer");
        Object valueDeserializer = Reflect.on(bizConsumer).get("valueDeserializer");

        if (keyDeserializer != null) {
            config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer.getClass());
        }
        if (valueDeserializer != null) {
            config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer.getClass());
        }
        config.put(ConsumerConfig.CLIENT_ID_CONFIG,
                Pradar.addClusterTestPrefix(Reflect.on(bizConsumer).get("clientId")));
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, consumerMetaData.getBootstrapServers());
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, (getAllowMaxLag() * 2 * 3) + "");
        config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,
                (getAllowMaxLag() * 2 + 5000) + "");
        config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, (getAllowMaxLag() * 3) + "");
        putSlience(config, ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, bizConsumer, "requestTimeoutMs");
        putSlience(config, ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, bizConsumer, "retryBackoffMs");

        /*
         * 认证配置
         */
        Object selector = Reflect.on(kafkaClient).get("selector");
        Object channelBuilder = Reflect.on(selector).get("channelBuilder");
        if ("org.apache.kafka.common.network.SaslChannelBuilder".equals(channelBuilder.getClass().getName())) {
            String clientSaslMechanism = Reflect.on(channelBuilder).get("clientSaslMechanism");
            config.put(SaslConfigs.SASL_MECHANISM, clientSaslMechanism);
            config.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG,
                    Reflect.on(channelBuilder).get("securityProtocol").toString());
            if (clientSaslMechanism != null && !"".equals(clientSaslMechanism)) {
                Map jaasContexts = ReflectUtil.reflectSlience(channelBuilder, "jaasContexts");
                if (jaasContexts == null) {
                    throw new RuntimeException("未支持的kafka版本，无法获取jaasContexts");
                }
                JaasContext jaasContext = (JaasContext) jaasContexts.get(clientSaslMechanism);
                if (jaasContext != null) {
                    String password = jaasContext.dynamicJaasConfig().value();
                    config.put(SaslConfigs.SASL_JAAS_CONFIG, password);
                } else {
                    LOGGER.warn("business kafka consumer using sasl but jaasContext not found jaasContexts is : {}",
                            jaasContexts);
                }
            } else {
                LOGGER.warn("business kafka consumer using sasl but clientSaslMechanism is blank");
            }
        }

        Object interceptors = ReflectUtil.reflectSlience(bizConsumer, "interceptors");
//        if (interceptors != null) {
//            List list = ReflectUtil.reflectSlience(interceptors, "interceptors");
//            if (list != null && list.size() > 0) {
//                List classList = new ArrayList();
//                for (Object o : list) {
//                    classList.add(o.getClass());
//                }
//                putSlience(config, ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, classList);
//            }
//        }

        putSlience(config, ConsumerConfig.METADATA_MAX_AGE_CONFIG, metadata, "metadataExpireMs");

        putSlience(config, ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, kafkaClient, "reconnectBackoffMs");
        putSlience(config, ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, kafkaClient, "reconnectBackoffMax");
        putSlience(config, ConsumerConfig.SEND_BUFFER_CONFIG, kafkaClient, "socketSendBuffer");
        putSlience(config, ConsumerConfig.RECEIVE_BUFFER_CONFIG, kafkaClient, "socketReceiveBuffer");
        putSlience(config, ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, kafkaClient, "requestTimeoutMs");

        Object subscriptions = ReflectUtil.reflectSlience(bizConsumer, "subscriptions");
        if (subscriptions != null) {
            Object defaultResetStrategy = ReflectUtil.reflectSlience(subscriptions, "defaultResetStrategy");
            if (defaultResetStrategy != null) {
                putSlience(config, ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                        defaultResetStrategy.toString().toLowerCase(Locale.ROOT));
            }
        }

        copyHeartbeatConfig(config, coordinator);

        config.put(ConsumerConfig.GROUP_ID_CONFIG, Pradar.addClusterTestPrefix(consumerMetaData.getGroupId()));
        putSlience(config, ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, coordinator, "sessionTimeoutMs");
        putSlience(config, ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, coordinator, "autoCommitEnabled");
        putSlience(config, ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, coordinator, "autoCommitIntervalMs");
        putSlience(config, ConsumerConfig.EXCLUDE_INTERNAL_TOPICS_CONFIG, coordinator, "excludeInternalTopics");

        putSlience(config, ConsumerConfig.FETCH_MIN_BYTES_CONFIG, fetcher, "minBytes");
        putSlience(config, ConsumerConfig.FETCH_MAX_BYTES_CONFIG, fetcher, "maxBytes");
        putSlience(config, ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, fetcher, "maxWaitMs");
        putSlience(config, ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, fetcher, "fetchSize");
        putSlience(config, ConsumerConfig.MAX_POLL_RECORDS_CONFIG, fetcher, "maxPollRecords");
        putSlience(config, ConsumerConfig.CHECK_CRCS_CONFIG, fetcher, "checkCrcs");

        KafkaConsumer kafkaConsumer;
        try {
            kafkaConsumer = new KafkaConsumer(config, (Deserializer) keyDeserializer, (Deserializer) valueDeserializer);
        } catch (Exception e) {
            kafkaConsumer = new KafkaConsumer(config);
        }

        Object ptInterceptors = Reflect.on(kafkaConsumer).get("interceptors");
        List list = null;
        if (Reflect.on(ptInterceptors).existsField("interceptors")) {
            list = Reflect.on(ptInterceptors).get("interceptors");
        }
        if ((list == null || list.isEmpty()) && interceptors != null) {
            LOGGER.info("set kafka biz interceptors to pt consumer:{}", interceptors);
            Reflect.on(kafkaConsumer).set("interceptors", interceptors);
        }

        ApacheKafkaHandler.addKafkaConsumerWorkWithOther(bizConsumer, false);
        ApacheKafkaHandler.addKafkaConsumerWorkWithOther(kafkaConsumer, true);
        return kafkaConsumer;
    }

    private static long getAllowMaxLag() {
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

    private static void putSlience(Properties config, String configStr, Object value) {
        try {
            config.put(configStr, value.toString());
        } catch (ReflectException ignore) {
        }
    }

    private static void putSlience(Properties config, String configStr, Object obj, String name) {
        try {
            Object value = Reflect.on(obj).get(name).toString();
            config.put(configStr, value);
        } catch (ReflectException ignore) {
        }
    }

    private static void copyHeartbeatConfig(Properties config, Object coordinator) {
        try {
            Object heartbeat = Reflect.on(coordinator).get("heartbeat");
            if (Reflect.on(heartbeat).existsField("rebalanceConfig")) {
                heartbeat = Reflect.on(heartbeat).get("rebalanceConfig");
            }
            config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, Reflect.on(heartbeat).get("sessionTimeoutMs"));
            config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, Reflect.on(heartbeat).get("rebalanceTimeoutMs"));
            config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, Reflect.on(heartbeat).get("heartbeatIntervalMs"));
        } catch (Exception e) {
            //
        }
    }
}
