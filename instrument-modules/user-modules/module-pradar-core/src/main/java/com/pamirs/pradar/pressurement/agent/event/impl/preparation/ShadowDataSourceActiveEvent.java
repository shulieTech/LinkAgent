package com.pamirs.pradar.pressurement.agent.event.impl.preparation;

import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.event.IEvent;

import javax.sql.DataSource;
import java.util.Map;

public class ShadowDataSourceActiveEvent implements IEvent {

    private Map.Entry<ShadowDatabaseConfig, DataSource> dataSource;

    public ShadowDataSourceActiveEvent(Map.Entry<ShadowDatabaseConfig, DataSource> dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Map.Entry<ShadowDatabaseConfig, DataSource>  getTarget() {
        return dataSource;
    }
}
