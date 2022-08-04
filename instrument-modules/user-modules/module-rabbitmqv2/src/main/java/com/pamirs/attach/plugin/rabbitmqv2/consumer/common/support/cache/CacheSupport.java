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
package com.pamirs.attach.plugin.rabbitmqv2.consumer.common.support.cache;


import com.pamirs.attach.plugin.rabbitmqv2.consumer.common.support.ConsumerApiResult;
import com.pamirs.attach.plugin.rabbitmqv2.consumer.model.ConsumerDetail;

import java.util.List;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/11/17 5:07 下午
 */
public interface CacheSupport {

    ConsumerApiResult computeIfAbsent(ConsumerDetail consumerDetail, Supplier supplier);

    void destroy();

    interface Supplier {

        List<ConsumerApiResult> get();

    }

    interface CacheKey {

    }

    interface CacheKeyBuilder {

        CacheKey build(ConsumerDetail consumerDetail);

        CacheKey build(ConsumerApiResult consumerApiResult);

    }

}
