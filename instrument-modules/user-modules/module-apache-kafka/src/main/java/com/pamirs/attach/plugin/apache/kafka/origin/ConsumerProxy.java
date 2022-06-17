/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.apache.kafka.origin;

import com.pamirs.attach.plugin.apache.kafka.ConfigCache;
import com.pamirs.attach.plugin.apache.kafka.KafkaConstants;
import com.pamirs.attach.plugin.apache.kafka.origin.selector.PollConsumerSelector;
import com.pamirs.attach.plugin.apache.kafka.origin.selector.RecordsRatioPollSelector;
import com.pamirs.attach.plugin.apache.kafka.util.ReflectUtil;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.consumer.internals.Heartbeat;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.JaasContext;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/05/12 4:25 下午
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ConsumerProxy<K, V> implements Consumer<K, V> {

    private final Consumer bizConsumer;

    private Consumer ptConsumer;

    private Heartbeat bizHeartbeat;
    private Heartbeat ptHeartbeat;

    private final PollConsumerSelector consumerSelector;

    private ConsumerMetaData topicAndGroup;

    private final Logger log = LoggerFactory.getLogger(ConsumerProxy.class);

    private final long ptMaxPollTimeout = 500;

    private long lag = 0;

    private final long allowMaxLag;

    private final long currentPollTime;

    private static boolean singleThreadConsumer = System.getProperty(KafkaConstants.SINGLE_THREAD_CONSUMER) != null;

    public ConsumerProxy(KafkaConsumer consumer, ConsumerMetaData topicAndGroup, long maxLagMillSecond, long timeout) {
        this(consumer, topicAndGroup, maxLagMillSecond, new RecordsRatioPollSelector(), timeout);
    }

    public ConsumerProxy(KafkaConsumer consumer, ConsumerMetaData topicAndGroup,
                         long maxLagMillSecond, PollConsumerSelector consumerSelector, long timeout) {
        this.bizConsumer = consumer;
        this.allowMaxLag = maxLagMillSecond;
        this.currentPollTime = timeout;
        this.ptConsumer = createPtConsumer(consumer, topicAndGroup);
        this.topicAndGroup = topicAndGroup;
        this.consumerSelector = consumerSelector;
        this.bizHeartbeat = extractHeartbeat(bizConsumer);
        this.ptHeartbeat = extractHeartbeat(ptConsumer);
    }

    private Heartbeat extractHeartbeat(Consumer consumer) {
        if (consumer instanceof WithTryCatchConsumerProxy) {
            consumer = Reflect.on(consumer).get("consumer");
        }
        Object coordinator = Reflect.on(consumer).get("coordinator");
        if (coordinator == null) {
            return null;
        }
        return Reflect.on(coordinator).get("heartbeat");
    }

    public Consumer getPtConsumer() {
        return ptConsumer;
    }

    public Consumer getBizConsumer() {
        return bizConsumer;
    }

    @Override
    public Set<TopicPartition> assignment() {
        Set<TopicPartition> set1 = this.bizConsumer.assignment();
        Set<TopicPartition> set2 = this.ptConsumer.assignment();
        Set<TopicPartition> result = new HashSet();
        result.addAll(set1);
        result.addAll(set2);
        return result;
    }

    @Override
    public Set<String> subscription() {
        Set<String> set1 = this.bizConsumer.subscription();
        Set<String> set2 = this.ptConsumer.subscription();
        Set<String> result = new HashSet();
        result.addAll(set1);
        result.addAll(set2);
        return result;
    }

    @Override
    public void assign(Collection collection) {
        log.warn("未支持的kafka方法调用: assign");
        this.bizConsumer.assign(collection);
    }

    @Override
    public void subscribe(Pattern pattern, ConsumerRebalanceListener callback) {
        this.bizConsumer.subscribe(pattern, callback);
        this.topicAndGroup = ConsumerMetaData.build((KafkaConsumer) bizConsumer);
        this.ptConsumer.subscribe(this.topicAndGroup.getShadowTopics());
    }

    @Override
    public void unsubscribe() {
        this.bizConsumer.unsubscribe();
        this.ptConsumer.unsubscribe();
    }

    @Override
    public void subscribe(Collection topics, ConsumerRebalanceListener callback) {
        this.bizConsumer.subscribe(topics, callback);
        this.topicAndGroup = ConsumerMetaData.build((KafkaConsumer) bizConsumer);
        this.ptConsumer.subscribe(this.topicAndGroup.getShadowTopics());
    }

    @Override
    public void subscribe(Collection topics) {
        this.bizConsumer.subscribe(topics);
        this.topicAndGroup = ConsumerMetaData.build((KafkaConsumer) bizConsumer);
        this.ptConsumer.subscribe(this.topicAndGroup.getShadowTopics());
    }

    /**
     * 优化原生kafka
     *
     * @return
     */
    boolean isBiz() {
        return ConfigCache.isBiz();
    }

    @Override
    public ConsumerRecords<K, V> poll(long timeout) {
        if (consumerSelector.select() == PollConsumerSelector.ConsumerType.SHADOW) {
            return doShadowPoll(timeout);
        } else {
            return doBizPoll(timeout);
        }
    }

    private ConsumerRecords doShadowPoll(long timeout) {
        try {
            ConsumerRecords consumerRecords = ptConsumer.poll(Math.min(timeout, ptMaxPollTimeout));
            Pradar.setClusterTest(true);
            ((RecordsRatioPollSelector) consumerSelector).addPtRecordCounts(consumerRecords.count());
            heartbeatPoll();
            return consumerRecords;
        } catch (Exception e) {
            log.error("shadow consumer poll fail!", e);
            return ConsumerRecords.empty();
        }
    }

    private void heartbeatPoll() {
        this.ptHeartbeat.poll(System.currentTimeMillis());
        this.bizHeartbeat.poll(System.currentTimeMillis());
    }

    private ConsumerRecords doBizPoll(long timeout) {
        ConsumerRecords consumerRecords = bizConsumer.poll(timeout);
        logDetection(consumerRecords);
        Pradar.setClusterTest(false);
        ((RecordsRatioPollSelector) consumerSelector).addBizRecordCounts(consumerRecords.count());
        heartbeatPoll();
        return consumerRecords;
    }

    private void logDetection(ConsumerRecords consumerRecords) {
        if (consumerRecords.isEmpty()) {
            lag = 0L;
            return;
        }
        long earliest = getEarliestRecordTime(consumerRecords);
        if (earliest <= 0) {
            return;
        }
        lag = System.currentTimeMillis() - earliest;
    }

    private long getEarliestRecordTime(ConsumerRecords<K, V> consumerRecords) {
        long earliest = Long.MAX_VALUE;
        for (ConsumerRecord consumerRecord : consumerRecords) {
            earliest = Math.min(consumerRecord.timestamp(), earliest);
        }
        return earliest;
    }

    @Override
    public void commitAsync() {
        commitAsync(null);
    }

    @Override
    public void commitAsync(OffsetCommitCallback callback) {
        if (Pradar.isClusterTest()) {
            ptConsumer.commitAsync(wrapShadowCommitCallback(callback));
        } else {
            bizConsumer.commitAsync(callback);
        }
    }

    @Override
    public void commitAsync(Map offsets, OffsetCommitCallback callback) {
        if (Pradar.isClusterTest()) {
            checkIfTopicMix(offsets, false);
            ptConsumer.commitAsync(offsets, wrapShadowCommitCallback(callback));
        } else {
            checkIfTopicMix(offsets, true);
            bizConsumer.commitAsync(offsets, callback);
        }
    }

    @Override
    public void commitSync() {
        Consumer consumer = chooseConsumerByContext();
        consumer.commitSync();
    }

    @Override
    public void commitSync(Map offsets) {
        if (Pradar.isClusterTest()) {
            checkIfTopicMix(offsets, false);
            ptConsumer.commitSync(offsets);
        } else {
            checkIfTopicMix(offsets, true);
            bizConsumer.commitSync(offsets);
        }
    }

    @Override
    public void seek(TopicPartition partition, long offset) {
        String topic = partition.topic();
        if (Pradar.isClusterTestPrefix(topic)) {
            this.ptConsumer.seek(partition, offset);
        } else {
            this.bizConsumer.seek(partition, offset);
        }
    }

    @Override
    public void seekToEnd(Collection<TopicPartition> partitions) {
        TopicPartitions topicPartitions = TopicPartitions.split(partitions);
        this.bizConsumer.seekToEnd(topicPartitions.bizCollection);
        this.ptConsumer.seekToEnd(topicPartitions.ptCollection);
    }

    @Override
    public void seekToBeginning(Collection<TopicPartition> partitions) {
        TopicPartitions topicPartitions = TopicPartitions.split(partitions);
        this.bizConsumer.seekToBeginning(topicPartitions.bizCollection);
        this.ptConsumer.seekToBeginning(topicPartitions.ptCollection);
    }

    @Override
    public void close() {
        bizConsumer.close();
        ptConsumer.close();
    }

    /**
     * 影子消费者是否存活
     *
     * @return
     */
    private boolean isAlive() {
        return alive;
    }

    private boolean alive = true;

    public void closePtConsumer() {
        ptConsumer.unsubscribe();
        ptConsumer.close();
        alive = false;
    }

    @Override
    public void close(long timeout, TimeUnit unit) {
        bizConsumer.close(timeout, unit);
        ptConsumer.close(timeout, unit);
    }

    @Override
    public void wakeup() {
        this.bizConsumer.wakeup();
        this.ptConsumer.wakeup();
    }

    @Override
    public long position(TopicPartition partition) {
        String topic = partition.topic();
        return Pradar.isClusterTestPrefix(topic) ? ptConsumer.position(partition) : bizConsumer.position(partition);
    }

    @Override
    public OffsetAndMetadata committed(TopicPartition partition) {
        String topic = partition.topic();
        return Pradar.isClusterTestPrefix(topic) ? ptConsumer.committed(partition) : bizConsumer.committed(partition);
    }

    @Override
    public Map<MetricName, ? extends Metric> metrics() {
        log.warn("未支持的kafka方法调用：metrics");
        return bizConsumer.metrics();
    }

    @Override
    public List<PartitionInfo> partitionsFor(String topic) {
        return Pradar.isClusterTestPrefix(topic) ? ptConsumer.partitionsFor(topic) : bizConsumer.partitionsFor(topic);
    }

    @Override
    public Map<String, List<PartitionInfo>> listTopics() {
        Map<String, List<PartitionInfo>> result = new HashMap();
        Map<String, List<PartitionInfo>> map1 = this.bizConsumer.listTopics();
        Map<String, List<PartitionInfo>> map2 = this.ptConsumer.listTopics();
        result.putAll(map1);
        result.putAll(map2);
        return result;
    }

    @Override
    public Set<TopicPartition> paused() {
        Set<TopicPartition> set1 = this.bizConsumer.paused();
        Set<TopicPartition> set2 = this.ptConsumer.paused();
        Set<TopicPartition> result = new HashSet();
        result.addAll(set1);
        result.addAll(set2);
        return result;
    }

    @Override
    public Map<TopicPartition, Long> endOffsets(Collection<TopicPartition> partitions) {
        TopicPartitions topicPartitions = TopicPartitions.split(partitions);
        Map<TopicPartition, Long> result = new HashMap();
        Map<TopicPartition, Long> map1 = this.bizConsumer.endOffsets(topicPartitions.bizCollection);
        Map<TopicPartition, Long> map2 = this.ptConsumer.endOffsets(topicPartitions.ptCollection);
        result.putAll(map1);
        result.putAll(map2);
        return result;
    }

    @Override
    public Map<TopicPartition, Long> beginningOffsets(Collection<TopicPartition> partitions) {
        TopicPartitions topicPartitions = TopicPartitions.split(partitions);
        Map<TopicPartition, Long> result = new HashMap();
        Map<TopicPartition, Long> map1 = this.bizConsumer.beginningOffsets(topicPartitions.bizCollection);
        Map<TopicPartition, Long> map2 = this.ptConsumer.beginningOffsets(topicPartitions.ptCollection);
        result.putAll(map1);
        result.putAll(map2);
        return result;
    }

    @Override
    public Map<TopicPartition, OffsetAndTimestamp> offsetsForTimes(Map<TopicPartition, Long> timestampsToSearch) {
        Map<TopicPartition, Long> bizMap = new HashMap();
        Map<TopicPartition, Long> ptMap = new HashMap();
        for (Entry<TopicPartition, Long> entry : timestampsToSearch.entrySet()) {
            if (Pradar.isClusterTestPrefix(entry.getKey().topic())) {
                ptMap.put(entry.getKey(), entry.getValue());
            } else {
                bizMap.put(entry.getKey(), entry.getValue());
            }
        }
        Map<TopicPartition, OffsetAndTimestamp> result = new HashMap();
        Map<TopicPartition, OffsetAndTimestamp> map1 = this.bizConsumer.offsetsForTimes(bizMap);
        Map<TopicPartition, OffsetAndTimestamp> map2 = this.ptConsumer.offsetsForTimes(ptMap);
        result.putAll(map1);
        result.putAll(map2);
        return result;
    }

    @Override
    public void resume(Collection<TopicPartition> partitions) {
        TopicPartitions topicPartitions = TopicPartitions.split(partitions);
        this.bizConsumer.resume(topicPartitions.bizCollection);
        this.ptConsumer.resume(topicPartitions.ptCollection);
    }

    @Override
    public void pause(Collection<TopicPartition> partitions) {
        TopicPartitions topicPartitions = TopicPartitions.split(partitions);
        this.bizConsumer.pause(topicPartitions.bizCollection);
        this.ptConsumer.pause(topicPartitions.ptCollection);
    }

    private Consumer chooseConsumerByContext() {
        return Pradar.isClusterTest() ? ptConsumer : bizConsumer;
    }

    private OffsetCommitCallback wrapShadowCommitCallback(final OffsetCommitCallback callback) {
        if (callback == null) {
            return null;
        }
        return new OffsetCommitCallback() {
            @Override
            public void onComplete(Map<TopicPartition, OffsetAndMetadata> offsets, Exception exception) {
                Pradar.setClusterTest(true);
                try {
                    callback.onComplete(offsets, exception);
                } finally {
                    Pradar.setClusterTest(false);
                }
            }
        };
    }

    private void checkIfTopicMix(Map<TopicPartition, OffsetAndMetadata> offsets, boolean isBiz) {
        for (Entry<TopicPartition, OffsetAndMetadata> entry : offsets.entrySet()) {
            String topic = entry.getKey().topic();
            if (isBiz) {
                if (Pradar.isClusterTestPrefix(topic)) {
                    throw new PressureMeasureError(String.format("提交的偏移量包含影子topic！: %s", topic));
                }
            } else {
                if (!Pradar.isClusterTestPrefix(topic)) {
                    throw new PressureMeasureError(String.format("提交的偏移量包含业务topic！: %s", topic));
                }
            }
        }
    }

    private Consumer createPtConsumer(KafkaConsumer consumer, ConsumerMetaData consumerMetaData) {
        Properties config = new Properties();
        Object coordinator = Reflect.on(consumer).get("coordinator");
        Object client = Reflect.on(consumer).get("client");
        Object kafkaClient = Reflect.on(client).get("client");
        Object fetcher = Reflect.on(consumer).get("fetcher");
        Object metadata = Reflect.on(consumer).get("metadata");
        Object keyDeserializer = Reflect.on(consumer).get("keyDeserializer");
        Object valueDeserializer = Reflect.on(consumer).get("valueDeserializer");

        if (keyDeserializer != null) {
            config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer.getClass());
        }
        if (valueDeserializer != null) {
            config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer.getClass());
        }
        config.put(ConsumerConfig.CLIENT_ID_CONFIG,
                Pradar.addClusterTestPrefix(String.valueOf(Reflect.on(consumer).get("clientId"))));
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, consumerMetaData.getBootstrapServers());
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, (this.allowMaxLag * 2 * 3) + "");
        config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,
                (Math.max(this.allowMaxLag * 2, this.currentPollTime * 2) + 5000) + "");
        config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, (this.allowMaxLag * 3) + "");
        putSlience(config, ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, consumer, "requestTimeoutMs");
        putSlience(config, ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, consumer, "retryBackoffMs");

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
                    log.warn("business kafka consumer using sasl but jaasContext not found jaasContexts is : {}",
                            jaasContexts);
                }
            } else {
                log.warn("business kafka consumer using sasl but clientSaslMechanism is blank");
            }
        }

        Object interceptors = ReflectUtil.reflectSlience(consumer, "interceptors");
        if (interceptors != null) {
            List list = ReflectUtil.reflectSlience(interceptors, "interceptors");
            if (list != null && list.size() > 0) {
                List classList = new ArrayList();
                for (Object o : list) {
                    classList.add(o.getClass());
                }
                putSlience(config, ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, classList);
            }
        }

        putSlience(config, ConsumerConfig.METADATA_MAX_AGE_CONFIG, metadata, "metadataExpireMs");

        putSlience(config, ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, kafkaClient, "reconnectBackoffMs");
        putSlience(config, ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, kafkaClient, "reconnectBackoffMax");
        putSlience(config, ConsumerConfig.SEND_BUFFER_CONFIG, kafkaClient, "socketSendBuffer");
        putSlience(config, ConsumerConfig.RECEIVE_BUFFER_CONFIG, kafkaClient, "socketReceiveBuffer");
        putSlience(config, ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, kafkaClient, "requestTimeoutMs");

        Object subscriptions = ReflectUtil.reflectSlience(consumer, "subscriptions");
        if (subscriptions != null) {
            Object defaultResetStrategy = ReflectUtil.reflectSlience(subscriptions, "defaultResetStrategy");
            if (defaultResetStrategy != null) {
                putSlience(config, ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                        defaultResetStrategy.toString().toLowerCase(Locale.ROOT));
            }
        }

        copyHeartbeatConfig(config, coordinator);

        config.put(ConsumerConfig.GROUP_ID_CONFIG, consumerMetaData.getPtGroupId());
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
            kafkaConsumer = new KafkaConsumer(config, (Deserializer) keyDeserializer,
                    (Deserializer) valueDeserializer);
        } catch (Exception e) {
            kafkaConsumer = new KafkaConsumer(config);
        }

        Object ptInterceptors = Reflect.on(kafkaConsumer).get("interceptors");
        List list = Reflect.on(ptInterceptors).get("interceptors");
        if (list == null || list.isEmpty()) {
            log.info("set kafka biz interceptors to pt consumer:{}", interceptors);
            Reflect.on(kafkaConsumer).set("interceptors", interceptors);
        }

        kafkaConsumer.subscribe(consumerMetaData.getShadowTopics());
        return new WithTryCatchConsumerProxy(kafkaConsumer);
    }

    private void copyHeartbeatConfig(Properties config, Object coordinator) {
        try {
            Object heartbeat = Reflect.on(coordinator).get("heartbeat");
            if (Reflect.on(heartbeat).existsField("rebalanceConfig")) {
                heartbeat = Reflect.on(heartbeat).get("rebalanceConfig");
            }
            config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, Reflect.on(heartbeat).get("sessionTimeoutMs"));
            config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, Reflect.on(heartbeat).get("rebalanceTimeoutMs"));
            config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, Reflect.on(heartbeat).get("heartbeatIntervalMs"));
        } catch (Exception e) {

        }
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

    private final static class TopicPartitions {
        private final Collection<TopicPartition> bizCollection;
        private final Collection<TopicPartition> ptCollection;

        private TopicPartitions(Collection<TopicPartition> bizCollection,
                                Collection<TopicPartition> ptCollection) {
            this.bizCollection = bizCollection;
            this.ptCollection = ptCollection;
        }

        private static TopicPartitions split(Collection<TopicPartition> collection) {
            Collection<TopicPartition> biz = new ArrayList();
            Collection<TopicPartition> pt = new ArrayList();
            for (TopicPartition partition : collection) {
                if (Pradar.isClusterTestPrefix(partition.topic())) {
                    pt.add(partition);
                } else {
                    biz.add(partition);
                }
            }
            return new TopicPartitions(biz, pt);
        }
    }

}
