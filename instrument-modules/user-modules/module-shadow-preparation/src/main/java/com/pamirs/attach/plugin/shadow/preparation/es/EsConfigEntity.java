package com.pamirs.attach.plugin.shadow.preparation.es;

import java.util.List;

public class EsConfigEntity {

    // 原始数据源类型 0:影子库 1:影子表 2:影子库+影子表
    // 0-未设置 1-影子库 2-影子库/影子表 3-影子表
    private Integer shadowType;
    private List<String> indices;
    private String businessNodes;
    private String performanceTestNodes;
    private String businessClusterName;
    private String performanceClusterName;
    private String ptUserName;
    private String ptPassword;

    public Integer getShadowType() {
        return shadowType;
    }

    public void setShadowType(Integer shadowType) {
        this.shadowType = shadowType;
    }

    public List<String> getIndices() {
        return indices;
    }

    public void setIndices(List<String> indices) {
        this.indices = indices;
    }

    public String getBusinessNodes() {
        return businessNodes;
    }

    public void setBusinessNodes(String businessNodes) {
        this.businessNodes = businessNodes;
    }

    public String getPerformanceTestNodes() {
        return performanceTestNodes;
    }

    public void setPerformanceTestNodes(String performanceTestNodes) {
        this.performanceTestNodes = performanceTestNodes;
    }

    public String getBusinessClusterName() {
        return businessClusterName;
    }

    public void setBusinessClusterName(String businessClusterName) {
        this.businessClusterName = businessClusterName;
    }

    public String getPerformanceClusterName() {
        return performanceClusterName;
    }

    public void setPerformanceClusterName(String performanceClusterName) {
        this.performanceClusterName = performanceClusterName;
    }

    public String getPtUserName() {
        return ptUserName;
    }

    public void setPtUserName(String ptUserName) {
        this.ptUserName = ptUserName;
    }

    public String getPtPassword() {
        return ptPassword;
    }

    public void setPtPassword(String ptPassword) {
        this.ptPassword = ptPassword;
    }
}
