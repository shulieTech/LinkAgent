package com.pamirs.attach.plugin.rabbitmqv2.consumer.common.support.cache;

import com.pamirs.attach.plugin.rabbitmqv2.consumer.common.support.ConsumerApiResult;
import com.pamirs.attach.plugin.rabbitmqv2.consumer.model.ConsumerDetail;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/01/11 3:49 PM
 */
public class WithoutIpCacheKeyBuilder implements CacheSupport.CacheKeyBuilder {

    @Override
    public CacheSupport.CacheKey build(ConsumerDetail consumerDetail) {
        return new WithoutIpCacheKey(consumerDetail.getConsumerTag(), consumerDetail.getChannel().getChannelNumber());
    }

    @Override
    public CacheSupport.CacheKey build(ConsumerApiResult consumerApiResult) {
        return new WithoutIpCacheKey(consumerApiResult.getConsumer_tag(),
                consumerApiResult.getChannel_details().getNumber());
    }
}
