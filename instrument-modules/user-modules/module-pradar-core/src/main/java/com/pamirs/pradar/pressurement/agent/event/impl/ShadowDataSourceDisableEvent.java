package com.pamirs.pradar.pressurement.agent.event.impl;

import com.pamirs.pradar.pressurement.agent.event.IEvent;

import java.util.Map;
import java.util.Set;

public class ShadowDataSourceDisableEvent implements IEvent {

    // key: 数据源类名称, value: url:username
    private Map.Entry<String,String> shadowDataSource;

    public ShadowDataSourceDisableEvent(Map.Entry<String,String> shadowDataSource) {
        this.shadowDataSource = shadowDataSource;
    }

    @Override
    public Map.Entry<String,String> getTarget() {
        return shadowDataSource;
    }
}
