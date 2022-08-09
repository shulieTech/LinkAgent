package io.shulie.instrument.module.isolation.register;

import io.shulie.instrument.module.isolation.enhance.EnhanceClass;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Licey
 * @date 2022/8/1
 */
public class ShadowProxyConfig {
    private List<EnhanceClass> enhanceClassList;
    private String moduleName;
    private String scope;

    public ShadowProxyConfig(String moduleName) {
        this.moduleName = moduleName;
        enhanceClassList = new ArrayList<EnhanceClass>();
    }

    public ShadowProxyConfig addEnhance(EnhanceClass enhanceClass) {
        enhanceClassList.add(enhanceClass);
        return this;
    }

    public List<EnhanceClass> getEnhanceClassList() {
        return enhanceClassList;
    }

    public void setEnhanceClassList(List<EnhanceClass> enhanceClassList) {
        this.enhanceClassList = enhanceClassList;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
