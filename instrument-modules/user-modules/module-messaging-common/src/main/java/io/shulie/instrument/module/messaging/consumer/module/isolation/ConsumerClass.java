package io.shulie.instrument.module.messaging.consumer.module.isolation;

import io.shulie.instrument.module.isolation.common.ResourceInit;
import io.shulie.instrument.module.isolation.enhance.EnhanceClass;
import io.shulie.instrument.module.isolation.enhance.EnhanceMethod;
import io.shulie.instrument.module.isolation.proxy.ShadowMethodProxy;
import io.shulie.instrument.module.isolation.resource.ShadowResourceProxyFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Licey
 * @date 2022/8/8
 */
public class ConsumerClass {
    private String className;

    private List<EnhanceMethod> methodList;

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

    public void setConvertImpl(boolean convertImpl) {
        isConvertImpl = convertImpl;
    }
}
