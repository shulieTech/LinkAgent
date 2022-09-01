package com.pamirs.attach.plugin.shadow.preparation.utils;

import com.pamirs.attach.plugin.shadow.preparation.entity.JdbcTableColumnInfos;
import com.pamirs.attach.plugin.shadow.preparation.entity.JdbcTableInfos;
import com.pamirs.pradar.Pradar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

public class JdbcConnectionUtils {

    private static final Logger logger = LoggerFactory.getLogger(JdbcConnectionUtils.class);

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

    public static void populateTableInfo(Connection connection, String table, JdbcTableInfos tableInfos) throws SQLException {
        ResultSet tables = connection.getMetaData().getColumns(null, null, table, null);
        while (tables.next()) {
            tableInfos.setTableCategory(tables.getString("TABLE_CAT"));
            tableInfos.setTableSchema(tables.getString("TABLE_SCHEM"));
            return;
        }
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

    public static String buildCreateTableSql(String table, List<JdbcTableColumnInfos> columnInfos) {
        if (!Pradar.isClusterTestPrefix(table)) {
            table = Pradar.addClusterTestPrefix(table);
        }
        StringBuilder sb = new StringBuilder(String.format("CREATE TABLE `%s` (", table));
        for (int i = 0; i < columnInfos.size(); i++) {
            JdbcTableColumnInfos column = columnInfos.get(i);
            sb.append(String.format("'%s' %s(%s)", column.getColumnName(), column.getTypeName(), column.getColumnSize()));
            String nullable = column.getNullable();
            sb.append("YES".equalsIgnoreCase(nullable) ? " NULL" : " NOT NULL");
            if (i < columnInfos.size() - 1) {
                sb.append(",");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    public static String showCreateTableSql(Connection connection, String table) throws SQLException {
        String sql = null;
        Statement statement = null;
        try {
            statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(String.format("show create table %s;", table));
            while (resultSet.next()) {
                sql = resultSet.getString(2);
                break;
            }
            String ptTable = Pradar.addClusterTestPrefix(table);
            sql = sql.replaceFirst(table, ptTable);
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        return sql;
    }

}
