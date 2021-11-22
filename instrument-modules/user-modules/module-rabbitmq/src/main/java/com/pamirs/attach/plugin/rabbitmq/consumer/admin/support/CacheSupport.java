/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.rabbitmq.consumer.admin.support;

import java.util.List;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/11/17 5:07 下午
 */
public interface CacheSupport {

    ConsumerApiResult computeIfAbsent(CacheKey cacheKey, Supplier supplier);

    void destroy();

    interface Supplier {

        List<ConsumerApiResult> get();

    }

    class CacheKey {

        private final String consumerTag;

        private final int channelNumber;

        private final String connectionLocalIp;

        private final int connectionLocalPort;

        public CacheKey(String consumerTag, int channelNumber, String connectionLocalIp, int connectionLocalPort) {
            this.consumerTag = consumerTag;
            this.channelNumber = channelNumber;
            this.connectionLocalIp = connectionLocalIp;
            this.connectionLocalPort = connectionLocalPort;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {return true;}
            if (!(o instanceof CacheKey)) {return false;}

            CacheKey cacheKey = (CacheKey)o;

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
}
