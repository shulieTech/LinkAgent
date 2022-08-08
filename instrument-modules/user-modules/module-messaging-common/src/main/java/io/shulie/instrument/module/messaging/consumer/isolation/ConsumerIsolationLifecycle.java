package io.shulie.instrument.module.messaging.consumer.isolation;

import io.shulie.instrument.module.isolation.resource.ShadowResourceLifecycle;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;

/**
 * @author Licey
 * @date 2022/8/8
 */
public class ConsumerIsolationLifecycle implements ShadowResourceLifecycle {

    private ShadowServer shadowServer;

    public ConsumerIsolationLifecycle(ShadowServer shadowServer) {
        this.shadowServer = shadowServer;
    }

    @Override
    public Object getTarget() {
        return shadowServer;
    }

    @Override
    public boolean isRunning() {
        return shadowServer.isRunning();
    }

    @Override
    public void start() {
        shadowServer.start();
    }

    @Override
    public void destroy(long timeout) {
        shadowServer.stop();
    }

    public ShadowServer getShadowServer() {
        return shadowServer;
    }

    public void setShadowServer(ShadowServer shadowServer) {
        this.shadowServer = shadowServer;
    }
}
