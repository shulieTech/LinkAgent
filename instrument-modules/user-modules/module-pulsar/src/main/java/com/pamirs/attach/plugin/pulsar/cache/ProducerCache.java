/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pamirs.attach.plugin.pulsar.cache;

import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.impl.ProducerImpl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/9/6 15:33
 */
public class ProducerCache {

    private static final Map<ProducerImpl, Producer> CACHE = new ConcurrentHashMap<ProducerImpl, Producer>();

    /**
     * 获取影子生产者
     *
     * @param bizProducer 业务生产者
     * @param supplier    影子生产者提供商
     * @return 影子生产者
     */
    public static Producer findShadowProducer(ProducerImpl bizProducer, Supplier supplier) throws Exception {
        Producer shadowProducer = CACHE.get(bizProducer);
        if (shadowProducer == null) {
            synchronized (CACHE) {
                shadowProducer = CACHE.get(bizProducer);
                if (shadowProducer == null) {
                    shadowProducer = supplier.get(bizProducer);
                    if (shadowProducer != null) {
                        CACHE.put(bizProducer, shadowProducer);
                    }
                }
            }
        }
        return shadowProducer;
    }

    public interface Supplier {
        /**
         * 根据业务生产者获取对应的影子生产者
         *
         * @param bizProducer 业务生产者
         * @return 影子生产者
         */
        Producer get(ProducerImpl bizProducer) throws Exception;
    }
}
