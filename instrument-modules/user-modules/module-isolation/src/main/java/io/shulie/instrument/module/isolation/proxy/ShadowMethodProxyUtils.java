package io.shulie.instrument.module.isolation.proxy;

import io.shulie.instrument.module.isolation.common.ResourceInit;
import io.shulie.instrument.module.isolation.exception.IsolationRuntimeException;
import io.shulie.instrument.module.isolation.proxy.impl.AddClusterRouteShadowMethodProxy;
import io.shulie.instrument.module.isolation.proxy.impl.RouteShadowMethodProxy;

/**
 * @author Licey
 * @date 2022/8/8
 */
public class ShadowMethodProxyUtils {
    public static ResourceInit<ShadowMethodProxy> addClusterRoute(final int... argIndex){
        if (argIndex == null) {
            throw new IsolationRuntimeException("addClusterRoute 参数不能为空");
        }
        return new ResourceInit<ShadowMethodProxy>() {
            @Override
            public ShadowMethodProxy init() {
                return new AddClusterRouteShadowMethodProxy(argIndex);
            }
        };
    }

    public static ResourceInit<ShadowMethodProxy> defaultRoute(){
        return new ResourceInit<ShadowMethodProxy>() {
            @Override
            public ShadowMethodProxy init() {
                return new RouteShadowMethodProxy();
            }
        };
    }

    public static ResourceInit<ShadowMethodProxy> notSupportRoute(){
        return new ResourceInit<ShadowMethodProxy>() {
            @Override
            public ShadowMethodProxy init() {
                return new RouteShadowMethodProxy();
            }
        };
    }
}
