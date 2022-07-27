package io.shulie.instrument.module.messaging.consumer.execute;

/**
 * @author Licey
 * @date 2022/7/27
 */
public interface ShadowServer {

    void start();

    boolean isRunning();

    void stop();
}
