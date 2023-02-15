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

package com.pamirs.attach.plugin.apache.kafkav2.consumer;

import com.pamirs.attach.plugin.apache.kafkav2.constant.KafkaConstants;
import com.pamirs.attach.plugin.apache.kafkav2.consumer.config.KafkaShadowConsumerConfig;
import com.pamirs.attach.plugin.apache.kafkav2.consumer.server.KafkaShadowConsumerServer;
import com.pamirs.attach.plugin.apache.kafkav2.util.KafkaUtil;
import com.pamirs.attach.plugin.apache.kafkav2.util.ReflectUtil;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.bean.SyncObjectData;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowConsumerExecute;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfigWithData;
import io.shulie.instrument.module.messaging.utils.ShadowConsumerPrefixUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.JaasContext;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/2 14:31
 */
public class KafkaExecute implements ShadowConsumerExecute {

    private static final Logger logger = LoggerFactory.getLogger(KafkaExecute.class);

    @Override
    public List<ConsumerConfig> prepareConfig(SyncObjectData syncObjectData) {
        KafkaConsumer bizConsumer = (KafkaConsumer) syncObjectData.getTarget();
        Set<String> topics = bizConsumer.subscription();
        String bootstrapServers = KafkaUtil.getBootstrapServers(bizConsumer);
        String group = getGroup(bizConsumer);
        List<ConsumerConfig> configs = new ArrayList<>();
        for (String topic : topics) {
            configs.add(new KafkaShadowConsumerConfig(topic, group, bootstrapServers, bizConsumer));
        }
        return configs;
    }

    @Override
    public ShadowServer fetchShadowServer(List<ConsumerConfigWithData> configList) {
        return new KafkaShadowConsumerServer(createShadowConsumer((KafkaShadowConsumerConfig) configList.get(0).getConsumerConfig()));
    }

    /**
     * 获取consumer对应的groupId
     *
     * @param bizConsumer 业务consumer
     * @return group
     */
    private String getGroup(KafkaConsumer bizConsumer) {
        try {
            Object coordinator = Reflect.on(bizConsumer).get(KafkaConstants.REFLECT_FIELD_COORDINATOR);
            Object groupId = ReflectUtil.reflectSlience(bizConsumer, KafkaConstants.REFLECT_FIELD_GROUP_ID);
            if (groupId == null) {
                groupId = ReflectUtil.reflectSlience(coordinator, KafkaConstants.REFLECT_FIELD_GROUP_ID);
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

    /**
     * 创建影子kafka consumer
     *
     * @param kafkaConfig 配置信息
     * @return KafkaConsumer
     */
    private KafkaConsumer createShadowConsumer(KafkaShadowConsumerConfig kafkaConfig) {
        KafkaConsumer bizConsumer = kafkaConfig.getKafkaConsumer();
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
                Pradar.addClusterTestPrefix(String.valueOf(Reflect.on(bizConsumer).get("clientId"))));
        config.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootstrapServers());
        config.put(org.apache.kafka.clients.consumer.ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, (getAllowMaxLag() * 2 * 3) + "");
        config.put(org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,
                (getAllowMaxLag() * 2 + 5000) + "");
        config.put(org.apache.kafka.clients.consumer.ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, (getAllowMaxLag() * 3) + "");
        putSlience(config, org.apache.kafka.clients.consumer.ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, bizConsumer, "requestTimeoutMs");
        putSlience(config, org.apache.kafka.clients.consumer.ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, bizConsumer, "retryBackoffMs");

        /**
         * 认证配置
         */
        Object selector = Reflect.on(kafkaClient).get("selector");
        Object channelBuilder = Reflect.on(selector).get("channelBuilder");
        if (channelBuilder.getClass().getName().equals("org.apache.kafka.common.network.SaslChannelBuilder")) {
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
                    logger.warn("business kafka consumer using sasl but jaasContext not found jaasContexts is : {}",
                            jaasContexts);
                }
            } else {
                logger.warn("business kafka consumer using sasl but clientSaslMechanism is blank");
            }
        }

        Object interceptors = ReflectUtil.reflectSlience(bizConsumer, "interceptors");
        if (interceptors != null) {
            List list = ReflectUtil.reflectSlience(interceptors, "interceptors");
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

        Object subscriptions = ReflectUtil.reflectSlience(bizConsumer, "subscriptions");
        if (subscriptions != null) {
            Object defaultResetStrategy = ReflectUtil.reflectSlience(subscriptions, "defaultResetStrategy");
            if (defaultResetStrategy != null) {
                putSlience(config, org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                        defaultResetStrategy.toString().toLowerCase(Locale.ROOT));
            }
        }

        copyHeartbeatConfig(config, coordinator);

        config.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, ShadowConsumerPrefixUtils.getShadowGroup(kafkaConfig.getTopic(), kafkaConfig.getGroupId()));
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
            logger.info("set kafka biz interceptors to pt consumer:{}", interceptors);
            Reflect.on(kafkaConsumer).set("interceptors", interceptors);
        }

        kafkaConsumer.subscribe(Collections.singletonList(Pradar.addClusterTestPrefix(kafkaConfig.getTopic())));
        return kafkaConsumer;
    }
}
