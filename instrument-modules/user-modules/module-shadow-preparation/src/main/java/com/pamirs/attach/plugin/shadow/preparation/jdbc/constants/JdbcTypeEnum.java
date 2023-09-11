package com.pamirs.attach.plugin.shadow.preparation.jdbc.constants;

import com.pamirs.attach.plugin.shadow.preparation.jdbc.entity.JdbcTableColumnInfos;
import com.pamirs.attach.plugin.shadow.preparation.jdbc.JdbcConnectionUtils;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

public enum JdbcTypeEnum {

    MYSQL() {
        @Override
        public Map<String, List<JdbcTableColumnInfos>> fetchTablesStructures(Connection connection, String database, List<String> tables) throws Exception {
            return JdbcConnectionUtils.fetchTablesStructures(connection, database, tables, MYSQL);
        }
    },

    ORACLE() {
        @Override
        public Map<String, List<JdbcTableColumnInfos>> fetchTablesStructures(Connection connection, String database, List<String> tables) throws Exception {
            return JdbcConnectionUtils.fetchTablesStructures(connection, database, tables, ORACLE);
        }
    },

    SQLSERVER() {
        @Override
        public Map<String, List<JdbcTableColumnInfos>> fetchTablesStructures(Connection connection, String database, List<String> tables) throws Exception {
            return JdbcConnectionUtils.fetchTablesStructures(connection, database, tables, SQLSERVER);
        }
    },

    GBASE() {
        @Override
        public Map<String, List<JdbcTableColumnInfos>> fetchTablesStructures(Connection connection, String database, List<String> tables) throws Exception {
            return JdbcConnectionUtils.fetchTablesStructures(connection, database, tables, GBASE);
        }
    };

    public abstract Map<String, List<JdbcTableColumnInfos>> fetchTablesStructures(Connection connection, String database, List<String> tables) throws Exception;

}
