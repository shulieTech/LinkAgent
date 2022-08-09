package io.shulie.instrument.module.messaging.consumer.isolation;

import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Licey
 * @date 2022/8/8
 */
public class ConsumerIsolationCache {
    private static final Map<Object, ConsumerIsolationLifecycle> cache = new ConcurrentHashMap<>();

    public static ConsumerIsolationLifecycle put(Object bizTarget, ShadowServer shadowTarget) {
        ConsumerIsolationLifecycle lifecycle = cache.get(bizTarget);
        if (lifecycle == null) {
            synchronized (bizTarget) {
                lifecycle = cache.get(bizTarget);
                if (lifecycle == null) {
                    lifecycle = new ConsumerIsolationLifecycle(shadowTarget);
                    cache.put(bizTarget, lifecycle);
                }
            }
        } else {
            lifecycle.setShadowServer(shadowTarget);
        }
        return lifecycle;
    }

    public static ConsumerIsolationLifecycle get(Object bizTarget) {
        return cache.get(bizTarget);
    }

}
