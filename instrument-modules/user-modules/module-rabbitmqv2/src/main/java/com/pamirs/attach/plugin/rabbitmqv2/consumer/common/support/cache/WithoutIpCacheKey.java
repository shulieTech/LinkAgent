package com.pamirs.attach.plugin.rabbitmqv2.consumer.common.support.cache;


/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/01/11 3:49 PM
 */
public class WithoutIpCacheKey implements CacheSupport.CacheKey {

    private final String consumerTag;

    private final int channelNumber;

    public WithoutIpCacheKey(String consumerTag, int channelNumber) {
        this.consumerTag = consumerTag;
        this.channelNumber = channelNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (!(o instanceof WithoutIpCacheKey)) {return false;}

        WithoutIpCacheKey
            cacheKey = (WithoutIpCacheKey)o;

        if (channelNumber != cacheKey.channelNumber) {return false;}
        return consumerTag.equals(cacheKey.consumerTag);
    }

    @Override
    public int hashCode() {
        int result = consumerTag.hashCode();
        result = 31 * result + channelNumber;
        return result;
    }

}
