package com.pamirs.attach.plugin.shadow.preparation.entity;

import java.util.List;

public class JdbcTableInfos {

    /**
     * 表类别
     */
    private String tableCategory;
    /**
     * 表模式
     */
    private String tableSchema;
    /**
     * 表名称
     */
    private String tableName;

    private String createTableSql;

    private List<JdbcTableColumnInfos> columns;

    public String getTableCategory() {
        return tableCategory;
    }

    public void setTableCategory(String tableCategory) {
        this.tableCategory = tableCategory;
    }

    public String getTableSchema() {
        return tableSchema;
    }

    public void setTableSchema(String tableSchema) {
        this.tableSchema = tableSchema;
    }

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

    public String getCreateTableSql() {
        return createTableSql;
    }

    public void setCreateTableSql(String createTableSql) {
        this.createTableSql = createTableSql;
    }
}
