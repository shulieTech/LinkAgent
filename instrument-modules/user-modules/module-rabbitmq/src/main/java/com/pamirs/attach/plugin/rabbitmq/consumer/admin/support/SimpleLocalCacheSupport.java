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
import java.util.Map;

import com.pamirs.pradar.exception.PradarException;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/11/17 5:37 下午
 */
public class SimpleLocalCacheSupport extends AbstractCacheSupport implements CacheSupport {

    private volatile Map<CacheKey, ConsumerApiResult> cache;

    private static final SimpleLocalCacheSupport INSTANCE = new SimpleLocalCacheSupport();

    private SimpleLocalCacheSupport() {}

    public static SimpleLocalCacheSupport getInstance() {return INSTANCE;}

    @Override
    public ConsumerApiResult computeIfAbsent(CacheKey cacheKey, Supplier supplier) {
        if (cache == null) {
            synchronized (SimpleLocalCacheSupport.class) {
                if (cache == null) {
                    renew(supplier);
                }
            }
        }
        return cache.get(cacheKey);
    }

    @Override
    public void destroy() {
        synchronized (SimpleLocalCacheSupport.class) {
            if (cache != null) {
                cache.clear();
                cache = null;
            }
        }
    }

    private void renew(Supplier supplier) {
        List<ConsumerApiResult> newList = supplier.get();
        if (newList == null) {
            throw new PradarException("supplier invoke but not data return!");
        }
        cache = group(newList);
    }
}
