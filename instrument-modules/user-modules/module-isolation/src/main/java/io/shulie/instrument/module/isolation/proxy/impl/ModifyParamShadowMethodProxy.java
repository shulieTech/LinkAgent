package io.shulie.instrument.module.isolation.proxy.impl;

import java.lang.reflect.Method;

/**
 * @author Licey
 * @date 2022/8/5
 */
public abstract class ModifyParamShadowMethodProxy extends RouteShadowMethodProxy {

    @Override
    public Object executeMethod(Object shadowTarget, Method method, Object... args) {
        return super.executeMethod(shadowTarget, method, fetchParam(shadowTarget, method, args));
    }

    public abstract Object[] fetchParam(Object shadowTarget, Method method, Object... args);
}
