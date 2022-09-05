package com.pamirs.attach.plugin.shadow.preparation.constants;

import com.pamirs.attach.plugin.shadow.preparation.entity.JdbcTableColumnInfos;
import com.pamirs.attach.plugin.shadow.preparation.utils.JdbcConnectionUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public enum JdbcTypeEnum {

    MYSQL() {
        @Override
        Map<String, List<JdbcTableColumnInfos>> fetchTablesStructures(Connection connection, List<String> tables) throws Exception {
            return JdbcConnectionUtils.fetchTablesStructures(connection, tables);
        }
    },

    ORACLE() {
        @Override
        Map<String, List<JdbcTableColumnInfos>> fetchTablesStructures(Connection connection, List<String> tables) throws Exception {
            return null;
        }
    },

    SQLSERVER() {
        @Override
        Map<String, List<JdbcTableColumnInfos>> fetchTablesStructures(Connection connection, List<String> tables) throws Exception {
            return null;
        }
    },

    GBASE() {
        @Override
        Map<String, List<JdbcTableColumnInfos>> fetchTablesStructures(Connection connection, List<String> tables) throws Exception {
            return JdbcConnectionUtils.fetchTablesStructures(connection, tables);
        }
    };

    abstract Map<String, List<JdbcTableColumnInfos>> fetchTablesStructures(Connection connection, List<String> tables) throws Exception;

}
