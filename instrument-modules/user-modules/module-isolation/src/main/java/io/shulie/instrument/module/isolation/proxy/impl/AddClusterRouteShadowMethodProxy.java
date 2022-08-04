package io.shulie.instrument.module.isolation.proxy.impl;

import com.pamirs.pradar.Pradar;
import io.shulie.instrument.module.isolation.exception.IsolationRuntimeException;

import java.lang.reflect.Method;

/**
 * @author Licey
 * @date 2022/8/2
 */
public class AddClusterRouteShadowMethodProxy extends RouteShadowMethodProxy {
    private int[] argIndex;

    public AddClusterRouteShadowMethodProxy(int... argIndex) {
        this.argIndex = argIndex;
    }

    @Override
    public Object executeMethod(Object shadowTarget, Method method, Object... args) {
        if (argIndex != null) {
            for (int index : argIndex) {
                if (index > args.length) {
                    throw new IsolationRuntimeException("index " + index + " is out of args.length:" + args.length + " in " + shadowTarget.getClass() + "#" + method);
                }
                if (args[index] instanceof String) {
                    args[index] = Pradar.addClusterTestPrefix((String) args[index]);
                }else{
                    throw new IsolationRuntimeException(shadowTarget.getClass() + "#" + method + " args[" + index + "] is not String!");
                }
            }
        }
        return super.executeMethod(shadowTarget, method, args);
    }
}
