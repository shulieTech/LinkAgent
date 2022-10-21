package com.pamirs.pradar.pressurement.agent.event.impl.preparation;

import com.pamirs.pradar.pressurement.agent.event.IEvent;

import java.util.concurrent.CountDownLatch;

public class ShadowEsActiveEvent implements IEvent {

    private Object restClient;
    private String bizNodes;
    private CountDownLatch latch;
    private String result;

    public ShadowEsActiveEvent(Object restClient, String bizNodes, CountDownLatch latch) {
        this.restClient = restClient;
        this.bizNodes = bizNodes;
        this.latch = latch;
    }

    public void handlerResult(String result) {
        this.result = result;
        this.latch.countDown();
    }

    public Object getRestClient() {
        return restClient;
    }

    public String getBizNodes() {
        return bizNodes;
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    public String getResult() {
        return result;
    }

    @Override
    public Object getTarget() {
        return bizNodes;
    }
}
