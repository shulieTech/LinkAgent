package com.pamirs.attach.plugin.rabbitmq.consumer.admin.support.cache;

import com.pamirs.attach.plugin.rabbitmq.common.ConsumerDetail;
import com.pamirs.attach.plugin.rabbitmq.consumer.admin.support.ConsumerApiResult;
import com.pamirs.attach.plugin.rabbitmq.consumer.admin.support.cache.CacheSupport.CacheKey;
import com.pamirs.attach.plugin.rabbitmq.consumer.admin.support.cache.CacheSupport.CacheKeyBuilder;
import com.pamirs.pradar.exception.PradarException;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/01/11 3:52 PM
 */
public class WithIpCacheKeyBuilder implements CacheKeyBuilder {

    @Override
    public CacheKey build(ConsumerDetail consumerDetail) {
        return new WithCacheKey(consumerDetail.getConsumerTag(),
            consumerDetail.getChannel().getChannelNumber(),
            consumerDetail.getConnectionLocalIp(), consumerDetail.getConnectionLocalPort());
    }

    @Override
    public CacheKey build(ConsumerApiResult consumerApiResult) {
        String connectionName = consumerApiResult.getChannel_details().getConnection_name();
        int idx = connectionName.indexOf(" ->");
        if (idx == -1) {
            throw new PradarException("api result connection name format error " + connectionName);
        }
        connectionName = connectionName.substring(0, idx);
        idx = connectionName.indexOf(":");
        if (idx == -1) {
            throw new PradarException("api result connection name format error " + connectionName);
        }
        String[] tmp = connectionName.split(":");
        if (tmp.length != 2) {
            throw new PradarException("api result connection name format error " + connectionName);
        }
        String connectionLocalIp = tmp[0];
        int connectionLocalPort = Integer.parseInt(tmp[1]);
        return new WithCacheKey(
            consumerApiResult.getConsumer_tag(),
            consumerApiResult.getChannel_details().getNumber(),
            connectionLocalIp,
            connectionLocalPort
        );
    }
}
