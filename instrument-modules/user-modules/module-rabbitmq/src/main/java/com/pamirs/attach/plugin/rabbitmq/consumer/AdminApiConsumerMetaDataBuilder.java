package com.pamirs.attach.plugin.rabbitmq.consumer;

import java.util.List;

import com.alibaba.fastjson.JSON;

import com.pamirs.attach.plugin.rabbitmq.common.DeliverDetail;
import com.pamirs.attach.plugin.rabbitmq.consumer.admin.support.CacheSupport;
import com.pamirs.attach.plugin.rabbitmq.consumer.admin.support.CacheSupport.CacheKey;
import com.pamirs.attach.plugin.rabbitmq.consumer.admin.support.ConsumerApiResult;
import com.pamirs.attach.plugin.rabbitmq.utils.AdminAccessInfo;
import com.pamirs.attach.plugin.rabbitmq.utils.HttpUtils;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.resource.SimulatorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/11/17 4:35 下午
 */
public class AdminApiConsumerMetaDataBuilder implements ConsumerMetaDataBuilder {

    private final Logger logger = LoggerFactory.getLogger(AdminApiConsumerMetaDataBuilder.class);

    private final SimulatorConfig simulatorConfig;

    private final CacheSupport cacheSupport;

    public AdminApiConsumerMetaDataBuilder(SimulatorConfig simulatorConfig, CacheSupport cacheSupport) {
        this.simulatorConfig = simulatorConfig;
        this.cacheSupport = cacheSupport;
    }

    @Override
    public ConsumerMetaData tryBuild(DeliverDetail deliverDetail) {
        Channel channel = deliverDetail.getChannel();
        try {
            Connection connection = Reflect.on(channel).get("_connection");
            String virtualHost = Reflect.on(connection).get("_virtualHost");
            AdminAccessInfo adminAccessInfo = resolveAdminAccessInfoByConfig(virtualHost);
            if (adminAccessInfo == null) {
                adminAccessInfo = resolveAdminAccessInfoByConnection(connection, virtualHost);
            }
            final AdminAccessInfo finalAdminAccessInfo = adminAccessInfo;
            ConsumerApiResult consumerApiResults = cacheSupport
                .computeIfAbsent(new CacheKey(deliverDetail.getConsumerTag(), channel.getChannelNumber()),
                    new CacheSupport.Supplier() {
                        @Override
                        public List<ConsumerApiResult> get() {
                            return getAllConsumers(finalAdminAccessInfo);
                        }
                    });
            return consumerApiResults.toConsumerMetaData(deliverDetail.getConsumer());
        } catch (Throwable e) {
            logger.warn("get queue from web admin fail!", e);
        }
        return null;
    }

    private AdminAccessInfo resolveAdminAccessInfoByConfig(String virtualHost) {
        String username = simulatorConfig.getProperty("rabbitmq.admin.username");
        String password = simulatorConfig.getProperty("rabbitmq.admin.password");
        String host = simulatorConfig.getProperty("rabbitmq.admin.host");
        Integer port = simulatorConfig.getIntProperty("rabbitmq.admin.port");
        boolean complete = true;
        if (host == null) {
            logger.warn("[RabbitMQ] missing rabbitmq.admin.host config");
            complete = false;
        }
        if (port == null) {
            logger.warn("[RabbitMQ] missing rabbitmq.admin.port config");
            complete = false;
        }
        if (username == null) {
            logger.warn("[RabbitMQ] missing rabbitmq.admin.username config");
            complete = false;
        }
        if (password == null) {
            logger.warn("[RabbitMQ] missing rabbitmq.admin.password config");
            complete = false;
        }
        return complete ? new AdminAccessInfo(host, port, username, password, virtualHost) : null;
    }

    private AdminAccessInfo resolveAdminAccessInfoByConnection(Connection connection, String virtualHost) {
        return AdminAccessInfo.solveByConnection(connection);
    }

    private List<ConsumerApiResult> getAllConsumers(AdminAccessInfo adminAccessInfo) {
        String url = String.format("/api/consumers/%s", adminAccessInfo.getVirtualHostEncode());
        String response = HttpUtils.doGet(adminAccessInfo, url).getResult();
        return JSON.parseArray(response, ConsumerApiResult.class);
    }
}
