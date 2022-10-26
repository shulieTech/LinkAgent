package io.shulie.instrument.module.messaging.consumer.module.isolation;

import io.shulie.instrument.module.isolation.common.ResourceInit;
import io.shulie.instrument.module.isolation.enhance.EnhanceMethod;
import io.shulie.instrument.module.isolation.proxy.ShadowMethodProxy;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Licey
 * @date 2022/8/8
 */
public class ConsumerClass {
    private String className;

    private List<EnhanceMethod> methodList;

    /**
     * 是否重写所有子类的方法
     */
    private boolean isConvertImpl;

    public ConsumerClass(String className) {
        this.className = className;
        methodList = new ArrayList<>();
    }

    public ConsumerClass addEnhanceMethod(String method, ResourceInit<ShadowMethodProxy> methodProxyInit, String... args) {
        EnhanceMethod enhanceMethod = new EnhanceMethod();
        enhanceMethod.setMethod(method);
        enhanceMethod.setArgTypes(args);
        enhanceMethod.setMethodProxyInit(methodProxyInit);
        methodList.add(enhanceMethod);
        return this;
    }

    public ConsumerClass addEnhanceMethod(String method, String scope, ResourceInit<ShadowMethodProxy> methodProxyInit, String... args) {
        EnhanceMethod enhanceMethod = new EnhanceMethod();
        enhanceMethod.setMethod(method);
        enhanceMethod.setArgTypes(args);
        enhanceMethod.setMethodProxyInit(methodProxyInit);
        enhanceMethod.setScope(scope);
        methodList.add(enhanceMethod);
        return this;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<EnhanceMethod> getMethodList() {
        return methodList;
    }

    public void setMethodList(List<EnhanceMethod> methodList) {
        this.methodList = methodList;
    }

    public boolean isConvertImpl() {
        return isConvertImpl;
    }

    public ConsumerClass setConvertImpl(boolean convertImpl) {
        isConvertImpl = convertImpl;
        return this;
    }
}
