package com.pamirs.pradar.pressurement.agent.event.impl;

import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.event.IEvent;

import javax.sql.DataSource;
import java.util.Map;

public class ShadowDataSourceActiveEvent implements IEvent {

    private Map<ShadowDatabaseConfig, DataSource> dataSourceMap;

    public ShadowDataSourceActiveEvent(Map<ShadowDatabaseConfig, DataSource> dataSourceMap) {
        this.dataSourceMap = dataSourceMap;
    }

    @Override
    public Map<ShadowDatabaseConfig, DataSource>  getTarget() {
        return dataSourceMap;
    }
}
