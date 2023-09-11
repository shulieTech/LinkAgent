package io.shulie.instrument.module.isolation.proxy;

import java.lang.reflect.Method;

/**
 * @author Licey
 * @date 2022/8/1
 */
public interface ShadowMethodProxy {
    Object executeMethod(Object shadowTarget, Method method, Object... args) throws Exception;

}
