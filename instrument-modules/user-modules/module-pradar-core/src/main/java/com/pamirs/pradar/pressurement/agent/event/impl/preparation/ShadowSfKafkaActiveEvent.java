package com.pamirs.pradar.pressurement.agent.event.impl.preparation;

import com.pamirs.pradar.pressurement.agent.event.IEvent;

import java.util.concurrent.CountDownLatch;

public class ShadowSfKafkaActiveEvent implements IEvent {

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
        this.result = result;
        this.latch.countDown();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTopicTokens() {
        return topicTokens;
    }

    public void setTopicTokens(String topicTokens) {
        this.topicTokens = topicTokens;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getProducerBroker() {
        return producerBroker;
    }

    public void setProducerBroker(String producerBroker) {
        this.producerBroker = producerBroker;
    }

    public String getSystemIdToken() {
        return systemIdToken;
    }

    public void setSystemIdToken(String systemIdToken) {
        this.systemIdToken = systemIdToken;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getMonitorUrl() {
        return monitorUrl;
    }

    public void setMonitorUrl(String monitorUrl) {
        this.monitorUrl = monitorUrl;
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

    public CountDownLatch getLatch() {
        return latch;
    }

    public void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    @Override
    public Object getTarget() {
        return String.format("sf-kafka:topic:%s#group:%s", topic, group);
    }
}
