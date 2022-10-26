package com.pamirs.attach.plugin.shadow.preparation.command;

import com.pamirs.attach.plugin.shadow.preparation.jdbc.entity.DataSourceConfig;

import java.util.List;

public class JdbcConfigPushCommand {
    private List<DataSourceConfig> data;

    public List<DataSourceConfig> getData() {
        return data;
    }

    public void setData(List<DataSourceConfig> data) {
        this.data = data;
    }
}
