package com.pamirs.attach.plugin.shadow.preparation.constants;

import com.pamirs.attach.plugin.shadow.preparation.entity.JdbcTableColumnInfos;
import com.pamirs.attach.plugin.shadow.preparation.utils.JdbcConnectionUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public enum JdbcTypeEnum {

    MYSQL(){
        @Override
        List<JdbcTableColumnInfos> fetchColumnINfo(Connection connection, String table) throws Exception {
            return JdbcConnectionUtils.fetchColumnInfos(connection, table);
        }

        @Override
        String showCreateTableSql(Connection connection, String table) throws SQLException {
            return JdbcConnectionUtils.showCreateTableSql(connection, table);
        }
    },

    ORACLE(){
        @Override
        List<JdbcTableColumnInfos> fetchColumnINfo(Connection connection, String table) throws Exception{
            return null;
        }

        @Override
        String showCreateTableSql(Connection connection, String table) {
            return null;
        }
    },

    SQLSERVER(){
        @Override
        List<JdbcTableColumnInfos> fetchColumnINfo(Connection connection, String table) throws Exception{
            return null;
        }

        @Override
        String showCreateTableSql(Connection connection, String table) {
            return null;
        }
    },

    GBASE(){
        @Override
        List<JdbcTableColumnInfos> fetchColumnINfo(Connection connection, String table) throws Exception{
            return JdbcConnectionUtils.fetchColumnInfos(connection, table);
        }

        @Override
        String showCreateTableSql(Connection connection, String table) throws SQLException {
            return JdbcConnectionUtils.showCreateTableSql(connection, table);
        }
    };

    abstract List<JdbcTableColumnInfos> fetchColumnINfo(Connection connection, String table) throws Exception;

    abstract String showCreateTableSql(Connection connection, String table) throws SQLException;

}
