package com.pamirs.pradar.pressurement.agent.event.impl;

import com.pamirs.pradar.pressurement.agent.event.IEvent;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ShadowMongoPreCheckEvent implements IEvent {

    private boolean isV4;
    private Integer dsType;
    private String bizUrl;
    private String shadowUrl;
    private List<String> tables;
    private Object mongoClient;

    private String result;
    private CountDownLatch latch;

    public ShadowMongoPreCheckEvent(boolean isV4, Integer dsType, String bizUrl, String shadowUrl, List<String> tables, Object mongoClient, CountDownLatch latch) {
        this.isV4 = isV4;
        this.dsType = dsType;
        this.bizUrl = bizUrl;
        this.shadowUrl = shadowUrl;
        this.tables = tables;
        this.mongoClient = mongoClient;
        this.latch = latch;
    }

    public boolean isV4() {
        return isV4;
    }

    public List<String> getTables() {
        return tables;
    }

    public String getResult() {
        return result;
    }

    public Integer getDsType() {
        return dsType;
    }

    public String getBizUrl() {
        return bizUrl;
    }

    public String getShadowUrl() {
        return shadowUrl;
    }

    public Object getMongoClient() {
        return mongoClient;
    }

    public void handlerResult(String result){
        this.result = result;
        this.latch.countDown();
    }

    @Override
    public Object getTarget() {
        return String.format("%d>%s>%s", dsType, bizUrl, shadowUrl == null ? "" : shadowUrl);
    }
}
