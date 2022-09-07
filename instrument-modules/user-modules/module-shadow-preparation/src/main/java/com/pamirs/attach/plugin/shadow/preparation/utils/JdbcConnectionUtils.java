package com.pamirs.attach.plugin.shadow.preparation.utils;

import com.pamirs.attach.plugin.shadow.preparation.entity.jdbc.JdbcTableColumnInfos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

public class JdbcConnectionUtils {

    private static final Logger logger = LoggerFactory.getLogger(JdbcConnectionUtils.class);

    /**
     * 获取多张表结构
     *
     * @param connection
     * @param tables
     * @return
     * @throws Exception
     */
    public static Map<String, List<JdbcTableColumnInfos>> fetchTablesStructures(Connection connection, List<String> tables) throws Exception {
        if (tables == null || tables.isEmpty()) {
            tables.addAll(fetchAllTables(connection));
        }

        Map<String, List<JdbcTableColumnInfos>> structures = new HashMap<String, List<JdbcTableColumnInfos>>();
        for (String table : tables) {
            structures.put(table, fetchColumnInfos(connection, table));
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
    public static List<JdbcTableColumnInfos> fetchColumnInfos(Connection connection, String table) throws Exception {
        List<JdbcTableColumnInfos> columns = new ArrayList<JdbcTableColumnInfos>();
        ResultSet tables = connection.getMetaData().getColumns(null, null, table, null);
        tables.getMetaData();
        while (tables.next()) {
            JdbcTableColumnInfos column = new JdbcTableColumnInfos();
            column.setColumnName(tables.getString("COLUMN_NAME"));
            column.setColumnSize(tables.getString("COLUMN_SIZE"));
            column.setTypeName(tables.getString("TYPE_NAME"));
            column.setRemarks(tables.getString("REMARKS"));
            column.setNullable(tables.getString("IS_NULLABLE"));
            // mysql 8.0后会获取到系统字段，以Host字段开始
            if (column.getColumnName().equals("Host") && "CHAR".equals(column.getTypeName()) && "60".equals(column.getColumnSize())) {
                break;
            }
            columns.add(column);
        }
        return columns;
    }

    public static List<String> fetchAllTables(Connection connection) throws SQLException {
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

    /**
     * 校验连接以及表可用性
     *
     * @param driver
     * @param url
     * @param username
     * @param pwd
     * @param tables
     * @return
     */
    public static Map<String, String> checkConnectionAndTableAvailable(String driver, String url, String username, String pwd, String... tables) {

        Map<String, String> result = new HashMap<String, String>();
        Connection connection = null;
        Statement statement = null;
        try {
            Class.forName(driver);
            connection = DriverManager.getConnection(url, username, pwd);
            statement = connection.createStatement();
        } catch (ClassNotFoundException e) {
            logger.error("[shadow-preparation] can`t load jdbc driver class:{}", driver, e);
            result.put("driverClass", e.getMessage());
        } catch (SQLException e) {
            logger.error("[shadow-preparation] get connection from database with driver:{}, url:{}, username:{} failed", driver, url, username, e);
            result.put("connection", e.getMessage());
        } finally {
            if (!result.isEmpty()) {
                closeResources(connection, statement, url, username);
            }
        }
        if (!result.isEmpty()) {
            return result;
        }

        for (String table : tables) {
            try {
                statement.execute(String.format("select 1 from %s", table));
                result.put(table, "success");
            } catch (SQLException e) {
                logger.error("[shadow-preparation] check jdbc shadow datasource available failed, url:{}, username:{}, table:{}", url, username, table, e);
                result.put(table, e.getMessage());
            }
        }

        closeResources(connection, statement, url, username);
        return result;
    }

    private static void closeResources(Connection connection, Statement statement, String url, String username) {
        try {
            statement.close();
            connection.close();
        } catch (SQLException e) {
            logger.error("[shadow-preparation] close connection/statement failed, url:{}, username:{}", url, username, e);
        }
    }

}
