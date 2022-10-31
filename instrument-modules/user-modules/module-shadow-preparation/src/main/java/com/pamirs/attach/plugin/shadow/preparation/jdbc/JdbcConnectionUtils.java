package com.pamirs.attach.plugin.shadow.preparation.jdbc;

import com.pamirs.attach.plugin.shadow.preparation.jdbc.constants.JdbcTypeEnum;
import com.pamirs.attach.plugin.shadow.preparation.jdbc.entity.JdbcTableColumnInfos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

public class JdbcConnectionUtils {

    private static final Logger logger = LoggerFactory.getLogger(JdbcConnectionUtils.class);

    /**
     * 查询sqlserver表结构的sql语句
     */
    private static final String SQLSERVER_QUERY_TABLE_STRUCTURE_SQL =
            "SELECT col.name AS COLUMN_NAME , ISNULL(ep.[value], '') AS REMARKS , t.name AS TYPE_NAME , col.length AS COLUMN_SIZE  ,\n" +
                    "    CASE\n" +
                    "        WHEN col.isnullable = 1 THEN\n" +
                    "            'Y'\n" +
                    "        ELSE 'N'\n" +
                    "        END AS IS_NULLABLE FROM dbo.syscolumns col\n" +
                    "    LEFT JOIN dbo.systypes t\n" +
                    "ON col.xtype = t.xusertype\n" +
                    "    INNER JOIN dbo.sysobjects obj\n" +
                    "    ON col.id = obj.id\n" +
                    "    AND obj.xtype = 'U'\n" +
                    "    AND obj.status >= 0\n" +
                    "    LEFT JOIN dbo.syscomments comm\n" +
                    "    ON col.cdefault = comm.id\n" +
                    "    LEFT JOIN sys.extended_properties ep\n" +
                    "    ON col.id = ep.major_id\n" +
                    "    AND col.colid = ep.minor_id\n" +
                    "    AND ep.name = 'MS_Description'\n" +
                    "    LEFT JOIN sys.extended_properties epTwo\n" +
                    "    ON obj.id = epTwo.major_id\n" +
                    "    AND epTwo.minor_id = 0\n" +
                    "    AND epTwo.name = 'MS_Description'WHERE obj.name = '%s'";

    /**
     * 获取多张表结构
     *
     * @param connection
     * @param tables
     * @param typeEnum
     * @return
     * @throws Exception
     */
    public static Map<String, List<JdbcTableColumnInfos>> fetchTablesStructures(Connection connection, String database, List<String> tables, JdbcTypeEnum typeEnum) throws Exception {
        if (tables.isEmpty()) {
            List<String> fetchedTables;
            switch (typeEnum) {
                case MYSQL:
                case GBASE:
                    fetchedTables = fetchAllTablesByJdbc(connection);
                    break;
                case ORACLE:
                    fetchedTables = fetchAllTablesForOracle(connection);
                    break;
                case SQLSERVER:
                    fetchedTables = fetchAllTablesForSqlServer(connection);
                    break;
                default:
                    return null;
            }
            tables.addAll(fetchedTables);
        }

        Map<String, List<JdbcTableColumnInfos>> structures = new HashMap<String, List<JdbcTableColumnInfos>>();
        for (String table : tables) {
            List<JdbcTableColumnInfos> columnInfos;
            switch (typeEnum) {
                case MYSQL:
                case GBASE:
                    columnInfos = fetchColumnInfosByJDBC(connection, database, table);
                    break;
                case ORACLE:
                    columnInfos = fetchColumnInfosForOracle(connection, table);
                    break;
                case SQLSERVER:
                    columnInfos = fetchColumnInfosForSqlServer(connection, table);
                    break;
                default:
                    return null;
            }
            if (!columnInfos.isEmpty()) {
                structures.put(table, columnInfos);
            }
        }
        return structures;
    }

    /**
     * 获取表字段信息
     *
     * @param connection
     * @param table
     * @return
     * @throws Exception
     */
    public static List<JdbcTableColumnInfos> fetchColumnInfosByJDBC(Connection connection, String database, String table) throws Exception {
        List<JdbcTableColumnInfos> columns = new ArrayList<JdbcTableColumnInfos>();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(String.format("select COLUMN_NAME,IS_NULLABLE, COLUMN_TYPE from information_schema.COLUMNS where TABLE_SCHEMA = '%s' and  TABLE_NAME = '%s'", database, table));
        Set<String> names = new HashSet<>();
        while (resultSet.next()) {
            JdbcTableColumnInfos column = new JdbcTableColumnInfos();
            column.setColumnName(resultSet.getString("COLUMN_NAME"));
            column.setColumnType(resultSet.getString("COLUMN_TYPE"));
            column.setNullable(resultSet.getString("IS_NULLABLE"));
            // mysql 8.0后会获取到系统字段，以Host字段开始
            if (column.getColumnName().equals("Host") && "CHAR(60)".equals(column.getColumnType())) {
                break;
            }
            if (names.contains(column.getColumnName())) {
                break;
            }
            columns.add(column);
            names.add(column.getColumnName());
        }
        return columns;
    }

    public static List<JdbcTableColumnInfos> fetchColumnInfosForOracle(Connection connection, String table) throws Exception {
        List<JdbcTableColumnInfos> columns = new ArrayList<JdbcTableColumnInfos>();
        Statement statement = null;
        try {
            statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(String.format("select * from user_tab_columns where table_name='%s'", table));
            while (resultSet.next()) {
                JdbcTableColumnInfos columnInfos = new JdbcTableColumnInfos();
                columnInfos.setColumnName(resultSet.getString("COLUMN_NAME"));
                columnInfos.setTypeName(resultSet.getString("DATA_TYPE"));
                columnInfos.setColumnSize(resultSet.getString("DATA_LENGTH"));
                columnInfos.setNullable(resultSet.getString("NULLABLE"));
                columns.add(columnInfos);
            }
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        return columns;
    }

    public static List<JdbcTableColumnInfos> fetchColumnInfosForSqlServer(Connection connection, String table) throws Exception {
        List<JdbcTableColumnInfos> columns = new ArrayList<JdbcTableColumnInfos>();
        Statement statement = null;
        try {
            statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(String.format(SQLSERVER_QUERY_TABLE_STRUCTURE_SQL, table));
            while (resultSet.next()) {
                JdbcTableColumnInfos column = new JdbcTableColumnInfos();
                column.setColumnName(resultSet.getString("COLUMN_NAME"));
                column.setColumnSize(resultSet.getString("COLUMN_SIZE"));
                column.setTypeName(resultSet.getString("TYPE_NAME"));
                column.setRemarks(resultSet.getString("REMARKS"));
                column.setNullable(resultSet.getString("IS_NULLABLE"));
                columns.add(column);
            }
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        return columns;
    }

    public static List<String> fetchAllTablesByJdbc(Connection connection) throws SQLException {
        List<String> tables = new ArrayList<String>();
        Statement statement = null;
        try {
            statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(String.format("show tables;"));
            while (resultSet.next()) {
                tables.add(resultSet.getString(1));
            }
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        return tables;
    }

    public static List<String> fetchAllTablesForOracle(Connection connection) throws SQLException {
        List<String> tables = new ArrayList<>();
        Statement statement = null;
        try {
            statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM user_tables");
            while (resultSet.next()) {
                tables.add(resultSet.getString("TABLE_NAME"));
            }
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        return tables;
    }

    public static List<String> fetchAllTablesForSqlServer(Connection connection) throws SQLException {
        List<String> tables = new ArrayList<String>();
        Statement statement = null;
        try {
            statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(String.format("select name from sysobjects where xtype='u'"));
            while (resultSet.next()) {
                tables.add(resultSet.getString("name"));
            }
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        return tables;
    }

}
