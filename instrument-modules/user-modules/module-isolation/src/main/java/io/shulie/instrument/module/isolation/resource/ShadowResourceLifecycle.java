package io.shulie.instrument.module.isolation.resource;

/**
 * @author Licey
 * @date 2022/7/26
 */
public interface ShadowResourceLifecycle {

    Object getTarget();

    boolean isRunning();

    void start();

    void destroy(long timeout);
}
