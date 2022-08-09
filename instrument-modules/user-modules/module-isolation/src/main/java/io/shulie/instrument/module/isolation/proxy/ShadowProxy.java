package io.shulie.instrument.module.isolation.proxy;

import com.pamirs.pradar.exception.PressureMeasureError;
import com.shulie.instrument.simulator.api.util.BehaviorDescriptor;
import io.shulie.instrument.module.isolation.common.ResourceInit;
import io.shulie.instrument.module.isolation.enhance.EnhanceClass;
import io.shulie.instrument.module.isolation.enhance.EnhanceMethod;
import io.shulie.instrument.module.isolation.exception.IsolationRuntimeException;
import io.shulie.instrument.module.isolation.proxy.impl.RouteShadowMethodProxy;
import io.shulie.instrument.module.isolation.resource.ShadowResourceLifecycle;
import io.shulie.instrument.module.isolation.resource.ShadowResourceProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
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

    private Map<Object, ShadowResourceLifecycleModule> shadowTargetMap;

    private Map<String, Method> methodMap;

    public ShadowProxy(String module, EnhanceClass enhanceClass, EnhanceMethod enhanceMethod) {
        shadowTargetMap = new ConcurrentHashMap<Object, ShadowResourceLifecycleModule>();
        methodMap = new ConcurrentHashMap<String, Method>();
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
        ShadowResourceLifecycleModule o = shadowTargetMap.get(bizTarget);
        if (o == null) {
            o = fetchShadowTarget(bizTarget);
        }
        try {
            return methodProxy.executeMethod(o.shadowResourceLifecycle.getTarget(), o.fetchMethod(method, methodDesc), args);
        } catch (Exception e) {
            throw new PressureMeasureError("can not execute target method! target: " + bizTarget + ", methodDesc:" + methodDesc + ", args:" + Arrays.toString(args), e);
        }
    }

    private synchronized ShadowResourceLifecycleModule fetchShadowTarget(Object bizTarget) {
        ShadowResourceLifecycleModule lifecycleModule = shadowTargetMap.get(bizTarget);
        if (lifecycleModule == null) {
            ShadowResourceLifecycle shadowResource = resourceProxyFactory.createShadowResource(bizTarget);
            if (shadowResource != null) {
                if (!shadowResource.isRunning()) {
                    shadowResource.start();
                }
                if (shadowResource.isRunning()) {
                    lifecycleModule = new ShadowResourceLifecycleModule(shadowResource);
                    shadowTargetMap.put(bizTarget, lifecycleModule);
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

    static class ShadowResourceLifecycleModule {
        ShadowResourceLifecycle shadowResourceLifecycle;
        Map<String, Method> methodMap;

        public ShadowResourceLifecycleModule(ShadowResourceLifecycle shadowResourceLifecycle) {
            this.shadowResourceLifecycle = shadowResourceLifecycle;
            methodMap = new ConcurrentHashMap<String, Method>();
        }

        Method fetchMethod(String method, String methodDesc) {
            //todo@langyi 优化性能和内存.
            Object ptTarget = shadowResourceLifecycle.getTarget();
            String key = toString(ptTarget);
            String keyTemp = keyOfMethod(method, methodDesc);
            String methodKey = key + keyTemp;
            Method m = methodMap.get(methodKey);
            if (m == null) {
                prepareMethodMap(key, ptTarget.getClass());
                m = methodMap.get(methodKey);
            }
            if (m == null) {
                throw new IsolationRuntimeException("[isolation]can not found method {}" + methodKey + " in " + ptTarget);
            }
            return m;
        }

        private void prepareMethodMap(String key, Class c) {
            if (c == null) {
                return;
            }
            for (final Method temp : c.getDeclaredMethods()) {
                temp.setAccessible(true);
                methodMap.put(key + keyOfMethod(temp.getName(), new BehaviorDescriptor(temp).getDescriptor()), temp);
            }
            prepareMethodMap(key, c.getSuperclass());
        }

        private String keyOfMethod(String method, String methodDesc) {
            return ":" + method + methodDesc;
        }

        public String toString(Object obj) {
            return obj.getClass().getName() + "@" + Integer.toHexString(obj.hashCode());
        }
    }


}
