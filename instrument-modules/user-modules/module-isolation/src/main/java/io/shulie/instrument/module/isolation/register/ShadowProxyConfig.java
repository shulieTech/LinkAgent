package io.shulie.instrument.module.isolation.register;

import io.shulie.instrument.module.isolation.enhance.EnhanceClass;
import io.shulie.instrument.module.isolation.proxy.ShadowMethodProxy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Licey
 * @date 2022/8/1
 */
public class ShadowProxyConfig {
    private List<EnhanceClass> enhanceClassList;
    private String moduleName;

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
}
