package io.shulie.instrument.module.messaging.consumer.isolation;

import io.shulie.instrument.module.isolation.resource.ShadowResourceLifecycle;
import io.shulie.instrument.module.isolation.resource.ShadowResourceProxyFactory;
import io.shulie.instrument.module.messaging.handler.ConsumerRouteHandler;

/**
 * @author Licey
 * @date 2022/8/8
 */
public class ConsumerIsolationProxyFactory implements ShadowResourceProxyFactory {

    @Override
    public ShadowResourceLifecycle createShadowResource(Object bizTarget) {
        return ConsumerIsolationCache.get(bizTarget);
    }

    @Override
    public boolean needRoute(Object target) {
        return ConsumerRouteHandler.needRoute(target);
    }
}
