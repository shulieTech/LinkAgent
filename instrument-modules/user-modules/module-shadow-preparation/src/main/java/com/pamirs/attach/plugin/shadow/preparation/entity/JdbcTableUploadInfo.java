package com.pamirs.attach.plugin.shadow.preparation.entity;

import java.util.List;

public class JdbcTableUploadInfo {

    private String appName;
    private List<JdbcTableInfos> tableInfos;
    private JdbcDataSourceEntity dataSourceEntity;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public List<JdbcTableInfos> getTableInfos() {
        return tableInfos;
    }

    public void setTableInfos(List<JdbcTableInfos> tableInfos) {
        this.tableInfos = tableInfos;
    }

    public JdbcDataSourceEntity getDataSourceEntity() {
        return dataSourceEntity;
    }

    public void setDataSourceEntity(JdbcDataSourceEntity dataSourceEntity) {
        this.dataSourceEntity = dataSourceEntity;
    }

}
