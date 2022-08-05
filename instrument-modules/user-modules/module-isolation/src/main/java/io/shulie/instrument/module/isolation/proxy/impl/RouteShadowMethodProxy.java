package io.shulie.instrument.module.isolation.proxy.impl;

import io.shulie.instrument.module.isolation.proxy.ShadowMethodProxy;

import java.lang.reflect.Method;

/**
 * @author Licey
 * @date 2022/8/1
 */
public class RouteShadowMethodProxy implements ShadowMethodProxy {
    @Override
    public Object executeMethod(Object shadowTarget, Method method, Object... args) throws Exception {
        return method.invoke(shadowTarget, args);
    }
}
