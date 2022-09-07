package com.pamirs.pradar.pressurement.agent.event.impl;

import com.pamirs.pradar.pressurement.agent.event.IEvent;

import java.util.Set;

public class ShadowDataSourceDisableEvent implements IEvent {

    private Set<String> shadowDataSources;

    public ShadowDataSourceDisableEvent(Set<String> shadowDataSources) {
        this.shadowDataSources = shadowDataSources;
    }

    @Override
    public Set<String>  getTarget() {
        return shadowDataSources;
    }
}
