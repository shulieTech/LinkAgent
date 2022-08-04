package com.pamirs.attach.plugin.rabbitmqv2.consumer.common.support.cache;


/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/01/11 3:52 PM
 */
public class WithCacheKey implements CacheSupport.CacheKey {

    private final String consumerTag;

    private final int channelNumber;

    private final String connectionLocalIp;

    private final int connectionLocalPort;

    public WithCacheKey(String consumerTag, int channelNumber, String connectionLocalIp,
        int connectionLocalPort) {
        this.consumerTag = consumerTag;
        this.channelNumber = channelNumber;
        this.connectionLocalIp = connectionLocalIp;
        this.connectionLocalPort = connectionLocalPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (!(o instanceof WithCacheKey)) {return false;}

        WithCacheKey cacheKey = (WithCacheKey)o;

        if (channelNumber != cacheKey.channelNumber) {return false;}
        if (connectionLocalPort != cacheKey.connectionLocalPort) {return false;}
        if (!consumerTag.equals(cacheKey.consumerTag)) {return false;}
        return connectionLocalIp.equals(cacheKey.connectionLocalIp);
    }

    @Override
    public int hashCode() {
        int result = consumerTag.hashCode();
        result = 31 * result + channelNumber;
        result = 31 * result + connectionLocalIp.hashCode();
        result = 31 * result + connectionLocalPort;
        return result;
    }

    public String getConsumerTag() {
        return consumerTag;
    }

    public int getChannelNumber() {
        return channelNumber;
    }

    public String getConnectionLocalIp() {
        return connectionLocalIp;
    }

    public int getConnectionLocalPort() {
        return connectionLocalPort;
    }

}
