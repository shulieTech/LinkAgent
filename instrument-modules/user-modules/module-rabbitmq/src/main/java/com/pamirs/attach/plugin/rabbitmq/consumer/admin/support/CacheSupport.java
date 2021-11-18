package com.pamirs.attach.plugin.rabbitmq.consumer.admin.support;

import java.util.List;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/11/17 5:07 下午
 */
public interface CacheSupport {

    ConsumerApiResult computeIfAbsent(CacheKey cacheKey, Supplier supplier);

    interface Supplier {

        List<ConsumerApiResult> get();

    }

    class CacheKey {

        private final String consumerTag;

        private final int channelNumber;

        public CacheKey(String consumerTag, int channelNumber) {
            this.consumerTag = consumerTag;
            this.channelNumber = channelNumber;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {return true;}
            if (!(o instanceof CacheKey)) {return false;}

            CacheKey cacheKey = (CacheKey)o;

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
}
