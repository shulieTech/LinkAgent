package io.shulie.instrument.module.isolation.proxy;

import com.pamirs.pradar.exception.PressureMeasureError;
import io.shulie.instrument.module.isolation.common.ResourceInit;
import io.shulie.instrument.module.isolation.common.ShadowResourceLifecycleModule;
import io.shulie.instrument.module.isolation.common.ShadowTargetCache;
import io.shulie.instrument.module.isolation.enhance.EnhanceClass;
import io.shulie.instrument.module.isolation.enhance.EnhanceMethod;
import io.shulie.instrument.module.isolation.exception.IsolationRuntimeException;
import io.shulie.instrument.module.isolation.proxy.impl.RouteShadowMethodProxy;
import io.shulie.instrument.module.isolation.resource.ShadowResourceLifecycle;
import io.shulie.instrument.module.isolation.resource.ShadowResourceProxyFactory;

import java.util.Arrays;

/**
 * @author Licey
 * @date 2022/7/26
 */
public class ShadowProxy {
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


    public ShadowProxy(String module, EnhanceClass enhanceClass, EnhanceMethod enhanceMethod) {
        this.module = module;
        this.enhanceClass = enhanceClass;
        this.enhanceMethod = enhanceMethod;
        this.methodProxy = enhanceMethod.getMethodProxyInit().init();
        this.resourceProxyFactory = enhanceClass.getFactoryResourceInit().init();
    }

    /**
     * 判断是否需要路由
     *
     * @param obj 对象
     * @return true需要路由，false不需要路由
     */
    public boolean needRoute(Object obj) {
        return resourceProxyFactory.needRoute(obj);
    }

    public Object executeMethod(Object bizTarget, String method, String methodDesc, Object... args) {
        ShadowResourceLifecycleModule o = ShadowTargetCache.get(bizTarget);
        if (o == null) {
            o = fetchShadowTarget(bizTarget);
        }
        try {
            return methodProxy.executeMethod(o.getShadowResourceLifecycle().getTarget(), o.fetchMethod(method, methodDesc), args);
        } catch (Exception e) {
            throw new PressureMeasureError("can not execute target method! target: " + bizTarget + ", methodDesc:" + methodDesc + ", args:" + Arrays.toString(args), e);
        }
    }

    private synchronized ShadowResourceLifecycleModule fetchShadowTarget(Object bizTarget) {
        ShadowResourceLifecycleModule lifecycleModule = ShadowTargetCache.get(bizTarget);
        if (lifecycleModule == null) {
            ShadowResourceLifecycle shadowResource = resourceProxyFactory.createShadowResource(bizTarget);
            if (shadowResource != null) {
                if (!shadowResource.isRunning()) {
                    shadowResource.start();
                }
                if (shadowResource.isRunning()) {
                    lifecycleModule = new ShadowResourceLifecycleModule(shadowResource);
                    ShadowTargetCache.put(bizTarget, lifecycleModule);
                } else {
                    throw new IsolationRuntimeException("can not start shadowResource with class:" + enhanceClass.getClassName() + " method:" + enhanceMethod.getMethod());
                }
            }
        }
        if (lifecycleModule == null) {
            throw new IsolationRuntimeException("can not init shadowResource with class:" + enhanceClass.getClassName() + " method:" + enhanceMethod.getMethod());
        }
        return lifecycleModule;
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
