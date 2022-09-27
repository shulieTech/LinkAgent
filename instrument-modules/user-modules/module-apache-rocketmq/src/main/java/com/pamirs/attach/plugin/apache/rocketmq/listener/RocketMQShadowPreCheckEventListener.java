package com.pamirs.attach.plugin.apache.rocketmq.listener;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.SyncObjectService;
import com.pamirs.pradar.bean.SyncObject;
import com.pamirs.pradar.bean.SyncObjectData;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowMqPreCheckEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.MessageListener;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.impl.MQAdminImpl;
import org.apache.rocketmq.client.impl.MQClientAPIImpl;
import org.apache.rocketmq.client.impl.MQClientManager;
import org.apache.rocketmq.common.UtilAll;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.body.SubscriptionGroupWrapper;
import org.apache.rocketmq.common.protocol.heartbeat.SubscriptionData;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.subscription.SubscriptionGroupConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class RocketMQShadowPreCheckEventListener implements PradarEventListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(RocketMQShadowPreCheckEventListener.class.getName());

    /**
     * 检查过的成功的topic#group
     */
    private static Set<String> successCheckedTopicGroups = new HashSet<String>();

    @Override
    public EventResult onEvent(IEvent iEvent) {
        if (!(iEvent instanceof ShadowMqPreCheckEvent)) {
            return EventResult.IGNORE;
        }
        ShadowMqPreCheckEvent event = (ShadowMqPreCheckEvent) iEvent;
        String type = event.getType();
        if (!"ROCKETMQ".equals(type)) {
            return EventResult.IGNORE;
        }

        Map<String, List<String>> topicGroups = event.getTopicGroups();
        Map<String, String> result = new HashMap<String, String>();

        SyncObject syncObject = SyncObjectService.getSyncObject("org.apache.rocketmq.client.consumer.DefaultMQPushConsumer#start");
        if (syncObject == null) {
            LOGGER.error("[apache-rocketmq] handler shadow mq precheck event failed because all business consumer doesn't exists!");
            for (Map.Entry<String, List<String>> entry : topicGroups.entrySet()) {
                String topic = entry.getKey();
                for (String group : entry.getValue()) {
                    result.put(topic + "#" + group, "请在应用启动参数内加 -Dagent.sync.module.enable=true 参数启动探针sync模块");
                }
            }
            event.handlerEvent(result);
            return EventResult.success("[apache-rocketmq]: handler shadow mq preCheck event success.");
        }

        for (Map.Entry<String, List<String>> entry : topicGroups.entrySet()) {
            doCheckTopicGroups(entry.getKey(), entry.getValue(), result);
        }
        event.handlerEvent(result);
        return EventResult.success("[apache-rocketmq]: handler shadow mq preCheck event success.");
    }

    private void doCheckTopicGroups(String topic, List<String> groups, Map<String, String> result) {
        SyncObject syncObject = SyncObjectService.getSyncObject("org.apache.rocketmq.client.consumer.DefaultMQPushConsumer#start");
        DefaultMQPushConsumer consumer = null;
        for (String group : groups) {
            for (SyncObjectData data : syncObject.getDatas()) {
                DefaultMQPushConsumer target = (DefaultMQPushConsumer) data.getTarget();
                if (group.equals(target.getConsumerGroup())) {
                    consumer = target;
                    break;
                }
            }
            if (consumer == null) {
                LOGGER.error("[apache-rocketmq] handler shadow mq precheck event failed because can,t find business consumer!");
                result.put(topic + "#" + group, String.format("topic:%s, group:%s 找不到对应的业务消费者", topic, group));
                continue;
            }
            doCheckTopicGroup(consumer, topic, group, result);
        }
    }

    private void doCheckTopicGroup(DefaultMQPushConsumer consumer, String topic, String group, Map<String, String> result) {

        String key = topic + "#" + group;
        if (successCheckedTopicGroups.contains(key)) {
            result.put(key, "success");
            return;
        }

        String ptTopic = Pradar.addClusterTestPrefix(topic);
        String ptGroup = Pradar.addClusterTestPrefix(group);

        DefaultMQPushConsumer preCheckConsumer = buildPreCheckConsumer(topic, consumer);
        if (preCheckConsumer == null) {
            result.put(key, String.format("影子消费者订阅topic:[%s]失败", ptTopic));
            return;
        }

        MQAdminImpl mqAdmin = ReflectionUtils.getFieldValues(consumer, "defaultMQPushConsumerImpl", "mQClientFactory", "mQAdminImpl");
        try {
            List<MessageQueue> messageQueues = mqAdmin.fetchPublishMessageQueues(ptTopic);
            if (messageQueues == null || messageQueues.isEmpty()) {
                result.put(key, String.format("影子topic:[%s]自动创建失败", ptTopic));
                closePreCheckConsumer(preCheckConsumer);
                return;
            }
        } catch (MQClientException e) {
            LOGGER.error("[apache-rocketmq] fetch publish message queues for topic :{} occur exception", ptTopic, e);
            result.put(key, String.format("影子topic:[%s]自动创建失败", ptTopic));
            closePreCheckConsumer(preCheckConsumer);
            return;
        }

        try {
            MQClientAPIImpl mqClientAPI = ReflectionUtils.getFieldValues(consumer, "defaultMQPushConsumerImpl", "rebalanceImpl", "mQClientFactory", "mQClientAPIImpl");
            ClusterInfo clusterInfo = mqClientAPI.getBrokerClusterInfo(3000);
            HashMap<String, BrokerData> addrTable = clusterInfo.getBrokerAddrTable();
            for (BrokerData brokerData : addrTable.values()) {
                Collection<String> values = brokerData.getBrokerAddrs().values();
                for (String brokerAddr : values) {
                    SubscriptionGroupWrapper subscriptionGroup = mqClientAPI.getAllSubscriptionGroup(brokerAddr, 3000);
                    ConcurrentMap<String, SubscriptionGroupConfig> groupTable = subscriptionGroup.getSubscriptionGroupTable();
                    if (groupTable != null && groupTable.containsKey(ptGroup)) {
                        result.put(key, "success");
                        successCheckedTopicGroups.add(key);
                        return;
                    }
                }
            }
            LOGGER.error("[apache-rocketmq] create consumer group:{} failed!", ptGroup);
            result.put(key, String.format("创建影子消费者Group:%s失败", ptGroup));
        } catch (Exception e) {
            LOGGER.error("[apache-rocketmq] check if group:{} auto created occur exception", ptGroup, e);
            result.put(key, String.format("查看影子消费组:%s 是否创建过程中出现异常", ptGroup));
        } finally {
            closePreCheckConsumer(preCheckConsumer);
        }
    }

    @Override
    public int order() {
        return 30;
    }

    private void closePreCheckConsumer(DefaultMQPushConsumer consumer) {
        SyncObject syncObject = SyncObjectService.getSyncObject("org.apache.rocketmq.client.consumer.DefaultMQPushConsumer#start");
        List<SyncObjectData> datas = syncObject.getDatas();
        Iterator<SyncObjectData> iterator = datas.iterator();
        while (iterator.hasNext()) {
            SyncObjectData next = iterator.next();
            if (next.getTarget().equals(consumer)) {
                iterator.remove();
            }
        }
        consumer.shutdown();
    }

    /**
     * 构建 DefaultMQPushConsumer
     * 如果后续支持影子 server 模式，则直接修改此方法即可
     *
     * @param businessConsumer 业务消费者
     * @return 返回注册的影子消费者，如果初始化失败会返回 null
     */
    private synchronized static DefaultMQPushConsumer buildPreCheckConsumer(String topic, DefaultMQPushConsumer businessConsumer) {
        DefaultMQPushConsumer defaultMQPushConsumer = new DefaultMQPushConsumer();

        String instanceName = getInstanceName();
        if (instanceName != null && !instanceName.equals("DEFAULT")) {
            defaultMQPushConsumer.setInstanceName(Pradar.CLUSTER_TEST_PREFIX + instanceName);
        } else {
            defaultMQPushConsumer.setInstanceName(
                    Pradar.addClusterTestPrefix(businessConsumer.getConsumerGroup() + instanceName));
        }

        Object bizMQPushConsumerImpl = businessConsumer.getDefaultMQPushConsumerImpl();
        if (Reflect.on(bizMQPushConsumerImpl).existsField("rpcHook")) {
            Object bizRpcHook = Reflect.on(bizMQPushConsumerImpl).get("rpcHook");
            if (bizRpcHook != null) {
                Reflect.on(defaultMQPushConsumer.getDefaultMQPushConsumerImpl()).set("rpcHook", bizRpcHook);
            }
        }

        defaultMQPushConsumer.setNamesrvAddr(businessConsumer.getNamesrvAddr());
        defaultMQPushConsumer.setConsumerGroup(Pradar.addClusterTestPrefix(businessConsumer.getConsumerGroup()));
        defaultMQPushConsumer.setConsumeFromWhere(businessConsumer.getConsumeFromWhere());
        defaultMQPushConsumer.setPullThresholdForQueue(businessConsumer.getPullThresholdForQueue());
        final List<String> missFields = new ArrayList<String>();
        try {
            defaultMQPushConsumer.setPullThresholdSizeForTopic(businessConsumer.getPullThresholdSizeForTopic());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setPullThresholdSizeForQueue(businessConsumer.getPullThresholdSizeForQueue());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setPullBatchSize(businessConsumer.getPullBatchSize());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setConsumeMessageBatchMaxSize(businessConsumer.getConsumeMessageBatchMaxSize());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setConsumeThreadMax(businessConsumer.getConsumeThreadMax());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setConsumeThreadMin(businessConsumer.getConsumeThreadMin());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setInstanceName(Pradar.addClusterTestPrefix(businessConsumer.getInstanceName()));
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setAdjustThreadPoolNumsThreshold(businessConsumer.getAdjustThreadPoolNumsThreshold());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setAllocateMessageQueueStrategy(businessConsumer.getAllocateMessageQueueStrategy());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setConsumeConcurrentlyMaxSpan(businessConsumer.getConsumeConcurrentlyMaxSpan());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setConsumeTimestamp(businessConsumer.getConsumeTimestamp());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setMessageModel(businessConsumer.getMessageModel());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setMessageListener(businessConsumer.getMessageListener());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setOffsetStore(businessConsumer.getOffsetStore());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setPullInterval(businessConsumer.getPullInterval());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setSubscription(businessConsumer.getSubscription());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setUnitMode(businessConsumer.isUnitMode());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setClientCallbackExecutorThreads(businessConsumer.getClientCallbackExecutorThreads());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setClientIP(businessConsumer.getClientIP());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setHeartbeatBrokerInterval(businessConsumer.getHeartbeatBrokerInterval());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setPersistConsumerOffsetInterval(businessConsumer.getPersistConsumerOffsetInterval());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setPostSubscriptionWhenPull(businessConsumer.isPostSubscriptionWhenPull());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setUnitName(businessConsumer.getUnitName());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setUnitMode(businessConsumer.isUnitMode());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setMaxReconsumeTimes(businessConsumer.getMaxReconsumeTimes());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setSuspendCurrentQueueTimeMillis(businessConsumer.getSuspendCurrentQueueTimeMillis());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setConsumeTimeout(businessConsumer.getConsumeTimeout());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setUseTLS(businessConsumer.isUseTLS());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setLanguage(businessConsumer.getLanguage());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }
        try {
            defaultMQPushConsumer.setVipChannelEnabled(businessConsumer.isVipChannelEnabled());
        } catch (Throwable e) {
            missFields.add(e.getMessage());
        }

        MessageListener messageListener = businessConsumer.getMessageListener();
        if (messageListener != null) {
            if (messageListener instanceof MessageListenerConcurrently) {
                defaultMQPushConsumer.registerMessageListener((MessageListenerConcurrently) messageListener);
            } else if (messageListener instanceof MessageListenerOrderly) {
                defaultMQPushConsumer.registerMessageListener((MessageListenerOrderly) messageListener);
            }
        }

        ConcurrentMap<String, SubscriptionData> map = businessConsumer.getDefaultMQPushConsumerImpl().getSubscriptionInner();

        boolean hasSubscribe = false;
        if (map != null) {
            for (Map.Entry<String, SubscriptionData> entry : map.entrySet()) {
                SubscriptionData subscriptionData = entry.getValue();
                if (!topic.equals(entry.getKey())) {
                    continue;
                }

                String subString = subscriptionData.getSubString();
                String filterClassSource = subscriptionData.getFilterClassSource();
                if (filterClassSource != null) {
                    try {
                        defaultMQPushConsumer.subscribe(Pradar.addClusterTestPrefix(topic), subString, filterClassSource);
                        defaultMQPushConsumer.start();
                    } catch (MQClientException e) {
                        LOGGER.error("Apache-RocketMQ: subscribe shadow DefaultMQPushConsumer err! topic:{} fullClassName:{} "
                                        + "filterClassSource:{}",
                                topic, subString, filterClassSource, e);
                        defaultMQPushConsumer.shutdown();
                        return null;
                    }
                } else {
                    try {
                        defaultMQPushConsumer.subscribe(Pradar.addClusterTestPrefix(topic), subString);
                        defaultMQPushConsumer.start();
                    } catch (MQClientException e) {
                        LOGGER.error(
                                "Apache-RocketMQ: subscribe shadow DefaultMQPushConsumer err! topic:{} subExpression:{}",
                                topic, subString, e);
                        defaultMQPushConsumer.shutdown();
                        return null;
                    }
                }
                hasSubscribe = true;
            }

            if (hasSubscribe) {
                return defaultMQPushConsumer;
            }
        }
        return null;
    }

    private static String getInstanceName() {
        String instanceName = System.getProperty("rocketmq.client.name", "DEFAULT");
        if (instanceName.equals("DEFAULT")) {
            instanceName = String.valueOf(UtilAll.getPid());
        }
        return instanceName;
    }

}
