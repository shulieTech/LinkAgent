package com.pamirs.pradar.pressurement.agent.event.impl.preparation;

import com.pamirs.pradar.pressurement.agent.event.IEvent;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ShadowEsPreCheckEvent implements IEvent {

    private Object restClient;
    private Integer shadowType;
    private List<String> indices;
    private String businessNodes;
    private String performanceTestNodes;
    private String businessClusterName;
    private String performanceClusterName;
    private String ptUserName;
    private String ptPassword;

    private CountDownLatch latch;

    private String result;

    public ShadowEsPreCheckEvent(Object restClient, Integer shadowType, List<String> indices, String businessNodes,
                                 String performanceTestNodes, String businessClusterName, String performanceClusterName,
                                 String ptUserName, String ptPassword, CountDownLatch latch) {
        this.restClient = restClient;
        this.shadowType = shadowType;
        this.indices = indices;
        this.businessNodes = businessNodes;
        this.performanceTestNodes = performanceTestNodes;
        this.businessClusterName = businessClusterName;
        this.performanceClusterName = performanceClusterName;
        this.ptUserName = ptUserName;
        this.ptPassword = ptPassword;
        this.latch = latch;
    }

    public void handlerResult(String result) {
        this.result = result;
        this.latch.countDown();
    }

    public Object getRestClient() {
        return restClient;
    }

    public Integer getShadowType() {
        return shadowType;
    }

    public List<String> getIndices() {
        return indices;
    }

    public String getBusinessNodes() {
        return businessNodes;
    }

    public String getPerformanceTestNodes() {
        return performanceTestNodes;
    }

    public String getBusinessClusterName() {
        return businessClusterName;
    }

    public String getPerformanceClusterName() {
        return performanceClusterName;
    }

    public String getPtUserName() {
        return ptUserName;
    }

    public String getPtPassword() {
        return ptPassword;
    }

    public String getResult() {
        return result;
    }



    @Override
    public Object getTarget() {
        return "esPreCheckEvent:" + businessNodes;
    }
}
