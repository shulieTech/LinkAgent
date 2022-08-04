package io.shulie.instrument.module.isolation.proxy.impl;

import com.pamirs.pradar.exception.PressureMeasureError;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import io.shulie.instrument.module.isolation.proxy.ShadowMethodProxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Licey
 * @date 2022/8/1
 */
public class RouteShadowMethodProxy implements ShadowMethodProxy {
    @Override
    public Object executeMethod(Object shadowTarget, Method method, Object... args) {
        try {
            return method.invoke(shadowTarget, args);
        }  catch (Throwable e) {
            throw new PressureMeasureError("can not execute target method!", e);
        }
    }
}
