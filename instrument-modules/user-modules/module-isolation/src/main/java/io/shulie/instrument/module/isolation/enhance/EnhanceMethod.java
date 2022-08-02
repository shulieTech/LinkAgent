package io.shulie.instrument.module.isolation.enhance;

import io.shulie.instrument.module.isolation.proxy.ShadowMethodProxy;

/**
 * @author Licey
 * @date 2022/8/1
 */
public class EnhanceMethod {
    private String method;
    private Class[] argTypes;
    private ShadowMethodProxy methodProxy;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Class[] getArgTypes() {
        return argTypes;
    }

    public void setArgTypes(Class[] argTypes) {
        this.argTypes = argTypes;
    }

    public ShadowMethodProxy getMethodProxy() {
        return methodProxy;
    }

    public void setMethodProxy(ShadowMethodProxy methodProxy) {
        this.methodProxy = methodProxy;
    }
}
