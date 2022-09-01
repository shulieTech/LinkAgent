package com.pamirs.attach.plugin.shadow.preparation.entity;

import com.alibaba.fastjson.JSON;

import java.util.Arrays;
import java.util.List;

public class JdbcDataSourcesUploadInfo {

    private String appName;

    private String host;

    private List<JdbcDataSourceEntity> dataSourceEntities;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public List<JdbcDataSourceEntity> getDataSourceEntities() {
        return dataSourceEntities;
    }

    public void setDataSourceEntities(List<JdbcDataSourceEntity> dataSourceEntities) {
        this.dataSourceEntities = dataSourceEntities;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
