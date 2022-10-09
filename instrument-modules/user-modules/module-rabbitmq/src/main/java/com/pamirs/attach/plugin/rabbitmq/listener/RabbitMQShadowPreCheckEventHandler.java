package com.pamirs.attach.plugin.rabbitmq.listener;

import com.pamirs.attach.plugin.rabbitmq.common.ChannelHolder;
import com.pamirs.attach.plugin.rabbitmq.common.ConfigCache;
import com.pamirs.attach.plugin.rabbitmq.common.ConsumerDetail;
import com.pamirs.attach.plugin.rabbitmq.common.ShadowConsumerProxy;
import com.pamirs.attach.plugin.rabbitmq.consumer.*;
import com.pamirs.attach.plugin.rabbitmq.consumer.admin.support.cache.CacheSupportFactory;
import com.pamirs.attach.plugin.rabbitmq.interceptor.ChannelNProcessDeliveryInterceptor;
import com.pamirs.attach.plugin.rabbitmq.utils.AddressUtils;
import com.pamirs.attach.plugin.rabbitmq.utils.RabbitMqUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.SyncObjectService;
import com.pamirs.pradar.bean.SyncObject;
import com.pamirs.pradar.bean.SyncObjectData;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowMqPreCheckEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.impl.AMQConnection;
import com.rabbitmq.client.impl.ChannelManager;
import com.rabbitmq.client.impl.ChannelN;
import com.rabbitmq.client.impl.SocketFrameHandler;
import com.rabbitmq.client.impl.recovery.AutorecoveringChannel;
import com.rabbitmq.client.impl.recovery.AutorecoveringConnection;
import com.shulie.druid.util.StringUtils;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.resource.SimulatorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class RabbitMQShadowPreCheckEventHandler implements PradarEventListener {

    /**
     * 检查过的成功的exchange#queue
     */
    private static Set<String> successCheckedExchangeQueues = new HashSet<String>();

    private final List<ConsumerMetaDataBuilder> consumerMetaDataBuilders = new ArrayList<ConsumerMetaDataBuilder>();

    private final static Logger LOGGER = LoggerFactory.getLogger(RabbitMQShadowPreCheckEventHandler.class.getName());

    public RabbitMQShadowPreCheckEventHandler(SimulatorConfig simulatorConfig) throws Exception {
        consumerMetaDataBuilders.add(SpringConsumerMetaDataBuilder.getInstance());
        consumerMetaDataBuilders.add(SpringConsumerDecoratorMetaDataBuilder.getInstance());
        consumerMetaDataBuilders.add(AutorecoveringChannelConsumerMetaDataBuilder.getInstance());
        consumerMetaDataBuilders.add(new AdminApiConsumerMetaDataBuilder(simulatorConfig,
                CacheSupportFactory.create(simulatorConfig)));
    }

    @Override
    public EventResult onEvent(IEvent iEvent) {
        if (!(iEvent instanceof ShadowMqPreCheckEvent)) {
            return EventResult.IGNORE;
        }
        ShadowMqPreCheckEvent event = (ShadowMqPreCheckEvent) iEvent;
        String type = event.getType();
        if (!"RABBITMQ".equals(type)) {
            return EventResult.IGNORE;
        }
        Map<String, List<String>> topicGroups = event.getTopicGroups();
        Map<String, String> result = new HashMap<String, String>();

        SyncObject syncObject = SyncObjectService.getSyncObject("com.rabbitmq.client.impl.ChannelN#basicConsume");
        if (syncObject == null) {
            LOGGER.error("[RabbitMQ] handler shadow mq precheck event failed because all business consumer doesn't exists!");
            for (Map.Entry<String, List<String>> entry : topicGroups.entrySet()) {
                String topic = entry.getKey();
                for (String group : entry.getValue()) {
                    result.put(topic + "#" + group, "请在应用启动参数内加 -Dagent.sync.module.enable=true 参数启动探针sync模块");
                }
            }
            event.handlerResult(result);
            return EventResult.success("[RabbitMQ]: handler shadow mq preCheck event success.");
        }

        for (Map.Entry<String, List<String>> entry : topicGroups.entrySet()) {
            try {
                doCheckExchangeQueues(entry.getKey(), entry.getValue(), result);
            } catch (Exception e) {

            }
        }
        event.handlerResult(result);
        return EventResult.success("[RabbitMQ]: handler shadow mq preCheck event success.");
    }

    private void doCheckExchangeQueues(String exchange, List<String> queues, Map<String, String> result) throws Exception {
        SyncObject syncObject = SyncObjectService.getSyncObject("com.rabbitmq.client.impl.ChannelN#basicConsume");
        ChannelN channelN = null;
        for (String queue : queues) {
            for (SyncObjectData data : syncObject.getDatas()) {
                ChannelN target = (ChannelN) data.getTarget();
                String q = (String) data.getArgs()[0];
                if (queue.equals(q)) {
                    channelN = target;
                    break;
                }
            }
            if (channelN == null) {
                LOGGER.error("[RabbitMQ] handler shadow mq precheck event failed because can,t find business channelN!");
                result.put(exchange + "#" + queue, String.format("exchange:%s, queue:%s 找不到对应的业务消费者", exchange, queue));
                continue;
            }
            doCheckExchangeQueue(channelN, exchange, queue, result);
        }
    }

    private void doCheckExchangeQueue(ChannelN channelN, String exchange, String queue, Map<String, String> result) throws Exception {
        String key = exchange + "#" + queue;
        if (successCheckedExchangeQueues.contains(key)) {
            result.put(key, "success");
            return;
        }

        List<ConsumerDetail> details = getAllConsumersFromConnection(channelN.getConnection());

        ConsumerDetail consumerDetail = null;
        ConsumerMetaData consumerMetaData = null;
        for (ConsumerDetail detail : details) {
            consumerMetaData = getConsumerMetaData(detail);
            if (consumerMetaData == null) {
                LOGGER.warn("[RabbitMQ] SIMULATOR: can not find consumerMetaData for channel : {}, consumerTag : {}"
                        , detail.getChannel(), detail.getConsumerTag());
                continue;
            }
            if (consumerMetaData.getQueue().equals(queue)) {
                consumerDetail = detail;
                break;
            }
        }

        if (consumerDetail == null) {
            result.put(key, "应用内部找不到对应的业务消费者");
            return;
        }

        // 检查queue
        if (!checkQueueExists(channelN, queue, exchange, result)) {
            return;
        }

        // 检查exchange
        if (!checkExchangeExists(channelN, queue, exchange, result)) {
            return;
        }

        String ptQueue = Pradar.addClusterTestPrefix(queue);
        String ptConsumerTag = consumerMetaData.getPtConsumerTag();

        LOGGER.info("[RabbitMQ] prepare create shadow consumer, queue : {} pt_queue : {} tag : {} pt_tag : {}",
                queue, ptQueue, consumerDetail.getConsumerTag(), ptConsumerTag);

        Channel channel = null;
        try {
            channel = consumeShadowQueue(channelN, consumerMetaData);
        } catch (Throwable e) {
            LOGGER.error("[RabbitMQ] consume shadow queue:{} occur exception!", ptQueue);
            result.put(key, String.format("订阅影子queue失败, 异常信息:%s", e.getMessage()));
            return;
        } finally {
            try {
                if (channel != null) {
                    closePreCheckConsumer(channel);
                }
            } catch (Exception e) {
                LOGGER.error("[RabbitMQ] close shadow channel occur exception!", e);
            }
        }
        successCheckedExchangeQueues.add(key);
        result.put(key, "success");
    }

    private boolean checkQueueExists(ChannelN channelN, String queue, String exchange, Map<String, String> result) {
        String ptQueue = Pradar.addClusterTestPrefix(queue);
        try {
            channelN.messageCount(ptQueue);
            return true;
        } catch (Throwable e) {
            LOGGER.error("[RabbitMQ] shadow rabbitmq queue {} not exists!", ptQueue);
            result.put(exchange + "#" + queue, String.format("影子queue不存在, 请先创建queue:%s, 同时检查exchange是否存在, 影子queue是否绑定影子exchange!", ptQueue));
            return false;
        }
    }

    private boolean checkExchangeExists(ChannelN channelN, String queue, String exchange, Map<String, String> result) {
        if (StringUtils.isEmpty(exchange)) {
            return true;
        }
        String ptExchange = Pradar.addClusterTestPrefix(exchange);
        try {
            channelN.exchangeDeclarePassive(ptExchange);
            return true;
        } catch (Throwable e) {
            LOGGER.error("[RabbitMQ] shadow rabbitmq exchange {} not exists!", ptExchange);
            result.put(exchange + "#" + queue, String.format("影子exchange不存在, 请先创建exchange:%s, 同时检查影子queue是否绑定影子exchange!", ptExchange));
            return false;
        }
    }


    private List<ConsumerDetail> getAllConsumersFromConnection(Connection connection) {
        List<ConsumerDetail> consumerDetails = new ArrayList<ConsumerDetail>();
        Set<Channel> channels = new HashSet<Channel>();
        if (connection instanceof AMQConnection) {
            ChannelManager _channelManager = Reflect.on(connection).get("_channelManager");
            Map<Integer, ChannelN> _channelMap = Reflect.on(_channelManager).get("_channelMap");
            channels.addAll(_channelMap.values());
        } else if (connection instanceof AutorecoveringConnection) {
            Map<Integer, AutorecoveringChannel> _channels = Reflect.on(connection).get("channels");
            channels.addAll(_channels.values());
        } else {
            LOGGER.error("[RabbitMQ] SIMULATOR unsupport rabbitmqConnection");
        }
        AMQConnection amqConnection = RabbitMqUtils.unWrapConnection(connection);
        SocketFrameHandler frameHandler = Reflect.on(amqConnection).get("_frameHandler");
        String localIp = frameHandler.getLocalAddress().getHostAddress();
        if (isLocalHost(localIp)) {
            localIp = AddressUtils.getLocalAddress();
            LOGGER.warn("[RabbitMQ] SIMULATOR get localIp from connection is localIp use {} instead", localIp);
        }
        int localPort = frameHandler.getLocalPort();
        for (Channel channel : channels) {
            ChannelN channelN = RabbitMqUtils.unWrapChannel(channel);
            Map<String, Consumer> _consumers = Reflect.on(channelN).get("_consumers");
            for (Map.Entry<String, Consumer> entry : _consumers.entrySet()) {
                consumerDetails.add(new ConsumerDetail(connection, entry.getKey(),
                        channel, entry.getValue(), localIp, localPort));
            }
        }

        return consumerDetails;
    }

    private boolean isLocalHost(String ip) {
        return "localhost".equals(ip) || "127.0.0.1".equals(ip);
    }

    private ConsumerMetaData getConsumerMetaData(ConsumerDetail deliverDetail) throws Exception {
        Channel channel = deliverDetail.getChannel();
        String consumerTag = deliverDetail.getConsumerTag();
        final int key = System.identityHashCode(channel);
        ConsumerMetaData consumerMetaData = ConfigCache.getConsumerMetaData(key, consumerTag);
        if (consumerMetaData == null) {
            synchronized (ChannelNProcessDeliveryInterceptor.class) {
                consumerMetaData = ConfigCache.getConsumerMetaData(key, consumerTag);
                if (consumerMetaData == null) {
                    consumerMetaData = buildConsumerMetaData(deliverDetail);
                    if (consumerMetaData != null) {
                        ConfigCache.putConsumerMetaData(key, consumerTag, consumerMetaData);
                    }
                }
            }
        }
        return consumerMetaData;
    }

    private ConsumerMetaData buildConsumerMetaData(ConsumerDetail deliverDetail) throws Exception {
        for (ConsumerMetaDataBuilder consumerMetaDataBuilder : consumerMetaDataBuilders) {
            ConsumerMetaData consumerMetaData = consumerMetaDataBuilder.tryBuild(deliverDetail);
            if (consumerMetaData != null) {
                return consumerMetaData;
            }
        }
        return null;
    }

    public Channel consumeShadowQueue(Channel target, ConsumerMetaData consumerMetaData) throws
            IOException {
        return consumeShadowQueue(target, consumerMetaData.getPtQueue(), consumerMetaData.isAutoAck(),
                consumerMetaData.getPtConsumerTag(), false, consumerMetaData.isExclusive(),
                consumerMetaData.getArguments(), consumerMetaData.getPrefetchCount(),
                new ShadowConsumerProxy(consumerMetaData.getConsumer()));
    }

    public Channel consumeShadowQueue(Channel target, String ptQueue, boolean autoAck,
                                      String ptConsumerTag,
                                      boolean noLocal, boolean exclusive, Map<String, Object> arguments, int prefetchCount,
                                      Consumer consumer) throws IOException {
        synchronized (ChannelHolder.class) {
            Channel shadowChannel = ChannelHolder.getOrShadowChannel(target);
            if (shadowChannel == null) {
                LOGGER.warn("[RabbitMQ] basicConsume failed. cause by shadow channel is not found. queue={}, consumerTag={}", ptQueue, ptConsumerTag);
                return null;
            }
            if (!shadowChannel.isOpen()) {
                LOGGER.warn("[RabbitMQ] basicConsume failed. cause by shadow channel is not closed. queue={}, consumerTag={}", ptQueue, ptConsumerTag);
                return null;
            }
            if (prefetchCount > 0) {
                shadowChannel.basicQos(prefetchCount);
            }
            shadowChannel.basicConsume(ptQueue, autoAck, ptConsumerTag, noLocal, exclusive, arguments, consumer);
            return shadowChannel;
        }
    }

    private void closePreCheckConsumer(Channel channel) throws IOException, TimeoutException {
        SyncObject syncObject = SyncObjectService.getSyncObject("com.rabbitmq.client.impl.ChannelN#basicConsume");
        List<SyncObjectData> datas = syncObject.getDatas();
        Iterator<SyncObjectData> iterator = datas.iterator();
        while (iterator.hasNext()) {
            SyncObjectData next = iterator.next();
            if (next.getTarget().equals(channel)) {
                iterator.remove();
            }
        }
        channel.close();
    }

    @Override
    public int order() {
        return 32;
    }
}
