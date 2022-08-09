package io.shulie.instrument.module.isolation.resource;

/**
 * @author Licey
 * @date 2022/8/1
 */
public interface ShadowResourceProxyFactory {

    /**
     * 获取影子对象
     *
     * @param bizTarget 业务对象
     * @return ShadowResourceLifecycle
     */
    ShadowResourceLifecycle createShadowResource(Object bizTarget);

    /**
     * 判断是否需要路由
     *
     * @param target 对象
     * @return true需要路由，false不需要路由
     */
    boolean needRoute(Object target);

}
