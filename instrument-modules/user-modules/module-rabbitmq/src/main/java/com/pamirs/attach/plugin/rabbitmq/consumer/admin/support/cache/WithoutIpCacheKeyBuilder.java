package com.pamirs.attach.plugin.rabbitmq.consumer.admin.support.cache;

import com.pamirs.attach.plugin.rabbitmq.common.ConsumerDetail;
import com.pamirs.attach.plugin.rabbitmq.consumer.admin.support.ConsumerApiResult;
import com.pamirs.attach.plugin.rabbitmq.consumer.admin.support.cache.CacheSupport.CacheKey;
import com.pamirs.attach.plugin.rabbitmq.consumer.admin.support.cache.CacheSupport.CacheKeyBuilder;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/01/11 3:49 PM
 */
public class WithoutIpCacheKeyBuilder implements CacheKeyBuilder {

    @Override
    public CacheKey build(ConsumerDetail consumerDetail) {
        return new WithoutIpCacheKey(consumerDetail.getConsumerTag(), consumerDetail.getChannel().getChannelNumber());
    }

    @Override
    public CacheKey build(ConsumerApiResult consumerApiResult) {
        return new WithoutIpCacheKey(consumerApiResult.getConsumer_tag(),
            consumerApiResult.getChannel_details().getNumber());
    }
}
