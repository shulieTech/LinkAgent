package io.shulie.instrument.module.isolation.proxy.impl;

import com.pamirs.pradar.exception.PressureMeasureError;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import io.shulie.instrument.module.isolation.proxy.ShadowMethodProxy;

import java.lang.reflect.InvocationTargetException;

/**
 * @author Licey
 * @date 2022/8/1
 */
public class RouteShadowMethodProxy implements ShadowMethodProxy {
    @Override
    public Object executeMethod(Object shadowTarget, String method, Object... args) {
        //todo@langyi 优化性能. 每次都反射，性能低
        Class[] argsClass = null;
        if (args != null) {
            argsClass = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                argsClass[i] = args[i].getClass();
            }
        }
        try {
            return Reflect.on(shadowTarget).exactMethod(method, argsClass).invoke(shadowTarget, args);
        }  catch (Throwable e) {
            throw new PressureMeasureError("can not execute target method!", e);
        }
    }
}
