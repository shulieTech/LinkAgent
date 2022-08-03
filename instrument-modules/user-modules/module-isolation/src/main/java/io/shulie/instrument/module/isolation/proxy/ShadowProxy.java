package io.shulie.instrument.module.isolation.proxy;

import io.shulie.instrument.module.isolation.common.ResourceInit;
import io.shulie.instrument.module.isolation.enhance.EnhanceClass;
import io.shulie.instrument.module.isolation.enhance.EnhanceMethod;
import io.shulie.instrument.module.isolation.exception.IsolationRuntimeException;
import io.shulie.instrument.module.isolation.proxy.impl.RouteShadowMethodProxy;
import io.shulie.instrument.module.isolation.resource.ShadowResourceLifecycle;
import io.shulie.instrument.module.isolation.resource.ShadowResourceProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Licey
 * @date 2022/7/26
 */
public class ShadowProxy {
    private static final Logger logger = LoggerFactory.getLogger(ShadowProxy.class);
    public static final ResourceInit<ShadowMethodProxy> ROUTE_SHADOW_METHOD_PROXY = new ResourceInit<ShadowMethodProxy>() {
        @Override
        public ShadowMethodProxy init() {
            return new RouteShadowMethodProxy();
        }
    };

    private String module;
    private EnhanceClass enhanceClass;
    private EnhanceMethod enhanceMethod;
    private ShadowMethodProxy methodProxy;
    private ShadowResourceProxyFactory resourceProxyFactory;

    private Map<Object, ShadowResourceLifecycle> shadowTargetMap;

    public ShadowProxy(String module, EnhanceClass enhanceClass, EnhanceMethod enhanceMethod) {
        shadowTargetMap = new ConcurrentHashMap<Object, ShadowResourceLifecycle>();
        this.module = module;
        this.enhanceClass = enhanceClass;
        this.enhanceMethod = enhanceMethod;
        this.methodProxy = enhanceMethod.getMethodProxyInit().init();
        this.resourceProxyFactory = enhanceClass.getFactoryResourceInit().init();

    }

    public Object executeMethod(Object bizTarget, String method, Object... args) {
        ShadowResourceLifecycle o = shadowTargetMap.get(bizTarget);
        if (o == null) {
            o = fetchShadowTarget(bizTarget);
        }
        return methodProxy.executeMethod(o.getTarget(), method, args);
    }

    private synchronized ShadowResourceLifecycle fetchShadowTarget(Object bizTarget) {
        ShadowResourceLifecycle o = shadowTargetMap.get(bizTarget);
        if (o == null) {
            ShadowResourceLifecycle shadowResource = resourceProxyFactory.createShadowResource(bizTarget);
            if (shadowResource != null) {
                if (!shadowResource.isRunning()) {
                    shadowResource.start();
                }
                if (shadowResource.isRunning()) {
                    shadowTargetMap.put(bizTarget, shadowResource);
                }else {
                    throw new IsolationRuntimeException("can not start shadowResource with class:" + enhanceClass.getClassName() + " method:" + enhanceMethod.getMethod());
                }
            }
        }
        if (o == null) {
            throw new IsolationRuntimeException("can not init shadowResource with class:" + enhanceClass.getClassName() + " method:" + enhanceMethod.getMethod());
        }
        return o;
    }

    public Map<Object, ShadowResourceLifecycle> getShadowTargetMap() {
        return shadowTargetMap;
    }

    public void setShadowTargetMap(Map<Object, ShadowResourceLifecycle> shadowTargetMap) {
        this.shadowTargetMap = shadowTargetMap;
    }

    public EnhanceClass getEnhanceClass() {
        return enhanceClass;
    }

    public void setEnhanceClass(EnhanceClass enhanceClass) {
        this.enhanceClass = enhanceClass;
    }

    public EnhanceMethod getEnhanceMethod() {
        return enhanceMethod;
    }

    public void setEnhanceMethod(EnhanceMethod enhanceMethod) {
        this.enhanceMethod = enhanceMethod;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }
}
