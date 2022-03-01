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
package com.pamirs.attach.plugin.rabbitmq.consumer.admin.support.cache;

import com.pamirs.attach.plugin.rabbitmq.common.ConsumerDetail;
import com.pamirs.attach.plugin.rabbitmq.consumer.admin.support.ConsumerApiResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/11/18 7:02 下午
 */
public abstract class AbstractCacheSupport implements CacheSupport {

    protected final CacheKeyBuilder cacheKeyBuilder;

    protected AbstractCacheSupport(CacheKeyBuilder cacheKeyBuilder) {this.cacheKeyBuilder = cacheKeyBuilder;}

    @Override
    public ConsumerApiResult computeIfAbsent(ConsumerDetail consumerDetail, Supplier supplier) {
        return computeIfAbsent(cacheKeyBuilder.build(consumerDetail), supplier);
    }

    protected Map<CacheKey, ConsumerApiResult> group(List<ConsumerApiResult> consumerApiResults) {
        Map<CacheKey, ConsumerApiResult> result = new HashMap<CacheKey, ConsumerApiResult>();
        for (ConsumerApiResult consumerApiResult : consumerApiResults) {
            result.put(cacheKeyBuilder.build(consumerApiResult), consumerApiResult);
        }
        return result;
    }

    public abstract ConsumerApiResult computeIfAbsent(CacheKey cacheKey, Supplier supplier);

}
