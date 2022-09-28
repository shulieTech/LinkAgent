package com.pamirs.pradar.pressurement.agent.event.impl;

import com.pamirs.pradar.pressurement.agent.event.IEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class ShadowMqPreCheckEvent implements IEvent {

    private String type;
    private Map<String, List<String>> topicGroups;
    private Map<String, String> checkResult;
    private CountDownLatch latch;

    public ShadowMqPreCheckEvent(String type, Map<String, List<String>> topicGroups, CountDownLatch latch) {
        this.type = type;
        this.topicGroups = topicGroups;
        this.latch = latch;
    }

    public Map<String, String> getCheckResult() {
        return checkResult;
    }

    public void handlerResult(Map<String, String> checkResult) {
        this.checkResult = checkResult;
        latch.countDown();
    }

    public String getType() {
        return type;
    }

    public Map<String, List<String>> getTopicGroups() {
        return topicGroups;
    }

    @Override
    public Map<String, List<String>> getTarget() {
        return topicGroups;
    }
}
