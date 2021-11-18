package com.pamirs.attach.plugin.rabbitmq.consumer.admin.support;

import java.util.List;
import java.util.Map;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/11/17 5:37 下午
 */
public class ZkCacheSupport implements CacheSupport {

    private volatile static Map<CacheKey, ConsumerApiResult> CACHE;

    private Lock lock;

    @Override
    public ConsumerApiResult computeIfAbsent(CacheKey cacheKey, Supplier supplier) {
        if (CACHE == null) {
            synchronized (CacheSupport.class) {
                if (CACHE == null) {
                    List<ConsumerApiResult> consumerApiResults = getFromZK();
                    if (consumerApiResults == null) {
                        renew(supplier);
                    }
                    CACHE = group(consumerApiResults);
                    ConsumerApiResult consumerApiResult = CACHE.get(cacheKey);
                    if (consumerApiResult == null) {
                        renew(supplier);
                    }
                    return CACHE.get(cacheKey);
                }
            }
        }
        return null;
    }

    private List<ConsumerApiResult> renew(Supplier supplier) {
        if (!lock.tryLock()) {
            lock.lock();
        }
        try {
            return doRenew(supplier);
        } finally {
            lock.release();
        }
    }

    private List<ConsumerApiResult> doRenew(Supplier supplier) {
        List<ConsumerApiResult> consumerApiResults = getFromZK();
        if (consumerApiResults == null) {
            consumerApiResults = supplier.get();
            putInZK(consumerApiResults);
        }
        return consumerApiResults;
    }

    private void putInZK(List<ConsumerApiResult> consumerApiResults) {

    }

    private Map<CacheKey, ConsumerApiResult> group(List<ConsumerApiResult> consumerApiResults) {
        return null;
    }

    private List<ConsumerApiResult> getFromZK() {
        return null;
    }

    private class Lock {
        private boolean tryLock() {
            return false;
        }

        private void lock() {

        }

        public void release() {

        }
    }
}
