package io.shulie.instrument.module.isolation.proxy;

/**
 * @author Licey
 * @date 2022/8/1
 */
public interface ShadowMethodProxy {
    Object executeMethod(Object shadowTarget, String method, Object... args);

}
