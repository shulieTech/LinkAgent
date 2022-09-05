package com.pamirs.attach.plugin.shadow.preparation.entity;

import java.util.List;

public class JdbcTableInfos {
    /**
     * 表名称
     */
    private String tableName;

    private List<JdbcTableColumnInfos> columns;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<JdbcTableColumnInfos> getColumns() {
        return columns;
    }

    public void setColumns(List<JdbcTableColumnInfos> columns) {
        this.columns = columns;
    }

}
