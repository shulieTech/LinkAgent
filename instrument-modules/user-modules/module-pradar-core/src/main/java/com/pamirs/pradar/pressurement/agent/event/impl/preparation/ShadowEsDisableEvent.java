package com.pamirs.pradar.pressurement.agent.event.impl.preparation;

import com.pamirs.pradar.pressurement.agent.event.IEvent;

import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class ShadowEsDisableEvent implements IEvent {

    private Set<String> shadowHosts;
    private ClassLoader bizClassLoader;
    private CountDownLatch latch;

    public ShadowEsDisableEvent(Set<String> shadowHosts, ClassLoader bizClassLoader, CountDownLatch latch) {
        this.shadowHosts = shadowHosts;
        this.bizClassLoader = bizClassLoader;
        this.latch = latch;
    }

    public Set<String> getShadowHosts() {
        return shadowHosts;
    }

    public ClassLoader getBizClassLoader() {
        return bizClassLoader;
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    @Override
    public Object getTarget() {
        return shadowHosts;
    }
}
