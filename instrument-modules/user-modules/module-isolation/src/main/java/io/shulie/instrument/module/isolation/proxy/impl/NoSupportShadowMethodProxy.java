package io.shulie.instrument.module.isolation.proxy.impl;

import io.shulie.instrument.module.isolation.proxy.ShadowMethodProxy;

import java.lang.reflect.Method;

/**
 * @author Licey
 * @date 2022/8/8
 */
public class NoSupportShadowMethodProxy implements ShadowMethodProxy {
    @Override
    public Object executeMethod(Object shadowTarget, Method method, Object... args) throws Exception {
        throw new RuntimeException("not support route method " + method.getName() + " in class:" + shadowTarget.getClass());
    }
}
