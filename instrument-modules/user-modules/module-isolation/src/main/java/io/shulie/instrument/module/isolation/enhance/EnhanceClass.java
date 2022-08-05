package io.shulie.instrument.module.isolation.enhance;

import io.shulie.instrument.module.isolation.common.ResourceInit;
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

    private ResourceInit<ShadowResourceProxyFactory> factoryResourceInit;

    private boolean isConvertImpl;

    public EnhanceClass(String className) {
        this.className = className;
        methodList = new ArrayList<EnhanceMethod>();
    }

    public EnhanceClass addEnhanceMethod(String method, ResourceInit<ShadowMethodProxy> methodProxyInit, String... args) {
        EnhanceMethod enhanceMethod = new EnhanceMethod();
        enhanceMethod.setMethod(method);
        enhanceMethod.setArgTypes(args);
        enhanceMethod.setMethodProxyInit(methodProxyInit);
        methodList.add(enhanceMethod);
        return this;
    }

    public EnhanceClass setFactoryResourceInit(ResourceInit<ShadowResourceProxyFactory> factoryResourceInit) {
        this.factoryResourceInit = factoryResourceInit;
        return this;
    }

    public EnhanceClass addEnhanceMethod(String method, String... args) {
        return addEnhanceMethod(method, ROUTE_SHADOW_METHOD_PROXY, args);
    }

    public EnhanceClass addEnhanceMethods(String... method) {
        if (method != null) {
            for (String s : method) {
                addEnhanceMethod(s, (String[]) null);
            }
        }
        return this;
    }

    public EnhanceClass addEnhanceMethods(ResourceInit<ShadowMethodProxy> methodProxy, String... method) {
        if (method != null) {
            for (String s : method) {
                addEnhanceMethod(s, methodProxy, (String[]) null);
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

    public ResourceInit<ShadowResourceProxyFactory> getFactoryResourceInit() {
        return factoryResourceInit;
    }
}
