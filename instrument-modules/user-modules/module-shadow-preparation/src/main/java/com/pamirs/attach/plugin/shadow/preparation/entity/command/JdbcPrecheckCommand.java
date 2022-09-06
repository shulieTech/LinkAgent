package com.pamirs.attach.plugin.shadow.preparation.entity.command;

import com.pamirs.attach.plugin.shadow.preparation.entity.jdbc.DataSourceEntity;

import java.util.List;

public class JdbcPrecheckCommand {

    // 数据源类型 0:影子库 1:影子表 2:影子库+影子表
    private Integer shadowType;
    private List<String> tables;
    private DataSourceEntity bizDataSource;
    private DataSourceEntity shadowDataSource;

    public Integer getShadowType() {
        return shadowType;
    }

    public void setShadowType(Integer shadowType) {
        this.shadowType = shadowType;
    }

    public List<String> getTables() {
        return tables;
    }

    public void setTables(List<String> tables) {
        this.tables = tables;
    }

    public DataSourceEntity getBizDataSource() {
        return bizDataSource;
    }

    public void setBizDataSource(DataSourceEntity bizDataSource) {
        this.bizDataSource = bizDataSource;
    }

    public DataSourceEntity getShadowDataSource() {
        return shadowDataSource;
    }

    public void setShadowDataSource(DataSourceEntity shadowDataSource) {
        this.shadowDataSource = shadowDataSource;
    }
}
