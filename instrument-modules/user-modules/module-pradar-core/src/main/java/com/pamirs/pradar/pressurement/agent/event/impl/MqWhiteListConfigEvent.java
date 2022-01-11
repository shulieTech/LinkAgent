package com.pamirs.pradar.pressurement.agent.event.impl;

import com.pamirs.pradar.pressurement.agent.event.IEvent;

import java.util.Set;

public class MqWhiteListConfigEvent implements IEvent {
    private Set<String> mqWhiteList;

    public MqWhiteListConfigEvent(Set<String> mqWhiteList) {
        this.mqWhiteList = mqWhiteList;
    }

    @Override
    public Object getTarget() {
        return mqWhiteList;
    }
}
