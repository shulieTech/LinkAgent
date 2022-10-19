package com.pamirs.pradar.pressurement.agent.event.impl.preparation;

import com.pamirs.pradar.pressurement.agent.event.IEvent;

import java.util.concurrent.CountDownLatch;

public class ShadowSfKafkaPreCheckEvent implements IEvent {

    private String key;
    private String topic;
    private String topicTokens;
    private String group;
    private String producerBroker;
    private String systemIdToken;
    private String clusterName;
    private String monitorUrl;
    private Integer poolSize;
    private Integer messageConsumeThreadCount;
    private CountDownLatch latch;
    private String result;

    public void handlerResult(String result) {
        this.handlerResult(result, true);
    }

    public void handlerResult(String result, boolean fireCount) {
        this.result = result;
        if (fireCount) {
            this.latch.countDown();
        }
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setTopicTokens(String topicTokens) {
        this.topicTokens = topicTokens;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setSystemIdToken(String systemIdToken) {
        this.systemIdToken = systemIdToken;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public void setMonitorUrl(String monitorUrl) {
        this.monitorUrl = monitorUrl;
    }

    public String getTopic() {
        return topic;
    }

    public String getTopicTokens() {
        return topicTokens;
    }

    public String getGroup() {
        return group;
    }

    public String getSystemIdToken() {
        return systemIdToken;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getMonitorUrl() {
        return monitorUrl;
    }

    public String getResult() {
        return result;
    }

    public void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }

    public Integer getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(Integer poolSize) {
        this.poolSize = poolSize;
    }

    public Integer getMessageConsumeThreadCount() {
        return messageConsumeThreadCount;
    }

    public void setMessageConsumeThreadCount(Integer messageConsumeThreadCount) {
        this.messageConsumeThreadCount = messageConsumeThreadCount;
    }

    public String getProducerBroker() {
        return producerBroker;
    }

    public void setProducerBroker(String producerBroker) {
        this.producerBroker = producerBroker;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public Object getTarget() {
        return topicTokens;
    }
}
