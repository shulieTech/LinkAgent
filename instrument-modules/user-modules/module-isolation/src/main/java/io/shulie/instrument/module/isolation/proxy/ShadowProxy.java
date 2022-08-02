package io.shulie.instrument.module.isolation.proxy;

import io.shulie.instrument.module.isolation.enhance.EnhanceClass;
import io.shulie.instrument.module.isolation.enhance.EnhanceMethod;
import io.shulie.instrument.module.isolation.exception.IsolationRuntimeException;
import io.shulie.instrument.module.isolation.proxy.impl.RouteShadowMethodProxy;
import io.shulie.instrument.module.isolation.resource.ShadowResourceLifecycle;
import io.shulie.instrument.module.isolation.resource.ShadowResourceProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Licey
 * @date 2022/7/26
 */
public class ShadowProxy {
    private static final Logger logger = LoggerFactory.getLogger(ShadowProxy.class);
    public static final ShadowMethodProxy ROUTE_SHADOW_METHOD_PROXY = new RouteShadowMethodProxy();

    private String module;
    private EnhanceClass enhanceClass;
    private EnhanceMethod enhanceMethod;

    private Map<Object,Object> shadowTargetMap;

    public ShadowProxy(String module, EnhanceClass enhanceClass, EnhanceMethod enhanceMethod) {
        this.module = module;
        this.enhanceClass = enhanceClass;
        this.enhanceMethod = enhanceMethod;
    }

    public Object executeMethod(Object bizTarget, String method, Object... args) {
        Object o = shadowTargetMap.get(bizTarget);
        if (o == null) {
            o = fetchShadowTarget(bizTarget);
        }
        //todo@langyi 代理执行方法
        return null;
    }

    private synchronized Object fetchShadowTarget(Object bizTarget){
        Object o = shadowTargetMap.get(bizTarget);
        if (o == null) {
            ShadowResourceLifecycle shadowResource = resourceProxyFactory.createShadowResource(bizTarget);
            if (shadowResource != null) {
                shadowTargetMap.put(bizTarget, shadowResource);
            }
        }
        if (o == null) {
            throw new IsolationRuntimeException("can not init shadowResource with class:" + " method:"+);
        }
        return o;
    }

    public Map<Object, Object> getShadowTargetMap() {
        return shadowTargetMap;
    }

    public void setShadowTargetMap(Map<Object, Object> shadowTargetMap) {
        this.shadowTargetMap = shadowTargetMap;
    }

    public ShadowMethodProxy getMethodProxy() {
        return methodProxy;
    }

    public void setMethodProxy(ShadowMethodProxy methodProxy) {
        this.methodProxy = methodProxy;
    }

}
