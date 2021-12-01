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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pamirs.pradar.exception.PradarException;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/11/18 7:02 下午
 */
public abstract class AbstractCacheSupport implements CacheSupport {

    protected Map<CacheKey, ConsumerApiResult> group(List<ConsumerApiResult> consumerApiResults) {
        Map<CacheKey, ConsumerApiResult> result = new HashMap<CacheKey, ConsumerApiResult>();
        for (ConsumerApiResult consumerApiResult : consumerApiResults) {
            result.put(toCacheKey(consumerApiResult), consumerApiResult);
        }
        return result;
    }

    protected CacheKey toCacheKey(ConsumerApiResult consumerApiResult) {
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
        return new CacheKey(
            consumerApiResult.getConsumer_tag(),
            consumerApiResult.getChannel_details().getNumber(),
            connectionLocalIp,
            connectionLocalPort
        );
    }
}
