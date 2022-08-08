package io.shulie.instrument.module.messaging.consumer.isolation;

import io.shulie.instrument.module.isolation.resource.ShadowResourceLifecycle;
import io.shulie.instrument.module.isolation.resource.ShadowResourceProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Licey
 * @date 2022/8/8
 */
public class ConsumerIsolationProxyFactory implements ShadowResourceProxyFactory {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerIsolationProxyFactory.class);

    private ConsumerIsolationLifecycle lifecycle;

    public ConsumerIsolationProxyFactory(ConsumerIsolationLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public ShadowResourceLifecycle createShadowResource(Object bizTarget) {
        return lifecycle;
    }
}
