package io.shulie.instrument.module.isolation.enhance;

import io.shulie.instrument.module.isolation.proxy.ShadowMethodProxy;
import io.shulie.instrument.module.isolation.resource.ShadowResourceProxyFactory;

import java.util.ArrayList;
import java.util.List;

import static io.shulie.instrument.module.isolation.proxy.ShadowProxy.ROUTE_SHADOW_METHOD_PROXY;

/**
 * @author Licey
 * @date 2022/8/1
 */
public class EnhanceClass {
    private String className;
    private List<EnhanceMethod> methodList;

    private ShadowResourceProxyFactory proxyFactory;

    private boolean isConvertImpl;

    public EnhanceClass(String className) {
        this.className = className;
        methodList = new ArrayList<EnhanceMethod>();
    }

    public EnhanceClass addEnhanceMethod(String method, ShadowMethodProxy methodProxy, Class... args) {
        EnhanceMethod enhanceMethod = new EnhanceMethod();
        enhanceMethod.setMethod(method);
        enhanceMethod.setArgTypes(args);
        enhanceMethod.setMethodProxy(methodProxy);
        methodList.add(enhanceMethod);
        return this;
    }

    public ShadowResourceProxyFactory getProxyFactory() {
        return proxyFactory;
    }

    public EnhanceClass setProxyFactory(ShadowResourceProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
        return this;
    }

    public EnhanceClass addEnhanceMethod(String method, Class... args) {
        return addEnhanceMethod(method, ROUTE_SHADOW_METHOD_PROXY, args);
    }

    public EnhanceClass addEnhanceMethods(String... method) {
        if (method != null) {
            for (String s : method) {
                addEnhanceMethod(s, (Class[]) null);
            }
        }
        return this;
    }

    public EnhanceClass addEnhanceMethods(ShadowMethodProxy methodProxy,String... method) {
        if (method != null) {
            for (String s : method) {
                addEnhanceMethod(s, methodProxy, (Class[]) null);
            }
        }
        return this;
    }

    public boolean isConvertImpl() {
        return isConvertImpl;
    }

    public EnhanceClass setConvertImpl(boolean convertImpl) {
        isConvertImpl = convertImpl;
        return this;
    }

    public String getClassName() {
        return className;
    }

    public EnhanceClass setClassName(String className) {
        this.className = className;
        return this;
    }

    public List<EnhanceMethod> getMethodList() {
        return methodList;
    }

}
