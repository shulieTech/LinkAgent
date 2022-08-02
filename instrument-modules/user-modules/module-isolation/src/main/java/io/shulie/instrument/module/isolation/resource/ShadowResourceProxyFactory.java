package io.shulie.instrument.module.isolation.resource;

/**
 * @author Licey
 * @date 2022/8/1
 */
public interface ShadowResourceProxyFactory {
    ShadowResourceLifecycle createShadowResource(Object bizTarget);

}
