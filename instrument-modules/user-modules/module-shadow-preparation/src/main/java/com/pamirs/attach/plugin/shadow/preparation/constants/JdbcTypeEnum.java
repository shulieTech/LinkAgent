package com.pamirs.attach.plugin.shadow.preparation.constants;

import com.pamirs.attach.plugin.shadow.preparation.entity.jdbc.JdbcTableColumnInfos;
import com.pamirs.attach.plugin.shadow.preparation.utils.JdbcConnectionUtils;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

public enum JdbcTypeEnum {

    MYSQL() {
        @Override
        public Map<String, List<JdbcTableColumnInfos>> fetchTablesStructures(Connection connection, List<String> tables) throws Exception {
            return JdbcConnectionUtils.fetchTablesStructures(connection, tables);
        }
    },

    ORACLE() {
        @Override
        public Map<String, List<JdbcTableColumnInfos>> fetchTablesStructures(Connection connection, List<String> tables) throws Exception {
            return null;
        }
    },

    SQLSERVER() {
        @Override
        public Map<String, List<JdbcTableColumnInfos>> fetchTablesStructures(Connection connection, List<String> tables) throws Exception {
            return null;
        }
    },

    GBASE() {
        @Override
        public Map<String, List<JdbcTableColumnInfos>> fetchTablesStructures(Connection connection, List<String> tables) throws Exception {
            return JdbcConnectionUtils.fetchTablesStructures(connection, tables);
        }
    };

    public abstract Map<String, List<JdbcTableColumnInfos>> fetchTablesStructures(Connection connection, List<String> tables) throws Exception;

}
