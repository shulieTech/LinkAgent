package com.pamirs.attach.plugin.shadow.preparation.command.processor;

import com.alibaba.fastjson.JSON;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.attach.plugin.shadow.preparation.command.JdbcPreCheckCommand;
import com.pamirs.attach.plugin.shadow.preparation.jdbc.constants.JdbcTypeEnum;
import com.pamirs.attach.plugin.shadow.preparation.jdbc.entity.DataSourceEntity;
import com.pamirs.attach.plugin.shadow.preparation.jdbc.entity.JdbcTableColumnInfos;
import com.pamirs.attach.plugin.shadow.preparation.jdbc.JdbcDataSourceFetcher;
import com.pamirs.attach.plugin.shadow.preparation.jdbc.JdbcTypeFetcher;
import com.pamirs.attach.plugin.shadow.preparation.jdbc.entity.ConfigPreCheckResult;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;
import com.shulie.instrument.simulator.api.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

public class JdbcPreCheckCommandProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(JdbcPreCheckCommandProcessor.class.getName());

    /**
     * 处理表信息读取命令
     *
     * @param entity
     * @param result
     */
    public static void processPreCheckCommand(JdbcPreCheckCommand entity, ConfigPreCheckResult result) {

        JdbcDataSourceFetcher.refreshDataSources();

        DataSourceEntity bizDataSource = entity.getBizDataSource();
        String driverClassName = fetchDriverClassName(bizDataSource);

        boolean isMongoDataSource = bizDataSource.getUrl().startsWith("mongodb://");
        // 如果是mongo数据源
        if (isMongoDataSource) {
            MongoPreCheckCommandProcessor.processPreCheckCommand(null, entity, null);
            return;
        }

        if (driverClassName == null) {
            LOGGER.error("[shadow-preparation] can`t find biz datasource to extract driver className.");
            result.setSuccess(false);
            result.setErrorMsg("业务数据源不存在");
            return;
        }

        // 0:影子库 1:影子表 2:影子库+影子表
        Integer shadowType = entity.getShadowType();

        if ((shadowType == 0 || shadowType == 2) && entity.getShadowDataSource() == null) {
            LOGGER.error("[shadow-preparation] ds type is shadow database or shadow database table, but shadow datasource is null");
            result.setSuccess(false);
            result.setErrorMsg("影子库/影子库影子表模式时影子数据源不能为空");
            return;
        }

        if(shadowType == 1 && CollectionUtils.isEmpty(entity.getTables())){
            LOGGER.error("[shadow-preparation] ds type is shadow table, but not biz tables is selected");
            result.setSuccess(false);
            result.setErrorMsg("影子表模式时需要选择有业务表");
            return;
        }

        bizDataSource.setDriverClassName(driverClassName);
        DataSourceEntity shadowDataSource = entity.getShadowDataSource();
        if (shadowDataSource != null) {
            shadowDataSource.setDriverClassName(driverClassName);
        }

        Class<?> bizDataSourceClass = extractBizClassForClassLoader(bizDataSource);

        List<String> tables = entity.getTables() != null ? entity.getTables() : new ArrayList<>();
        List<String> shadowTables = new ArrayList<String>();


        Map<String, List<JdbcTableColumnInfos>> bizInfos, shadowInfos;
        // 0:影子库 1:影子表 2:影子库+影子表
        switch (shadowType) {
            case 0:
                // 如果表为空，执行此步骤会填充业务表名称
                bizInfos = fetchBizTableInfo(bizDataSource, tables, result);
                if (bizInfos == null) {
                    return;
                }
                // 业务表不存在
                if (bizInfos.size() != tables.size()) {
                    ackWithBizTableNotExists(bizInfos.keySet(), tables, result);
                    return;
                }
                shadowInfos = fetchShadowTableInfo(bizDataSourceClass, shadowDataSource, tables, result);
                if (shadowInfos == null) {
                    return;
                }
                break;
            case 1:
                bizInfos = fetchBizTableInfo(bizDataSource, tables, result);
                if (bizInfos == null) {
                    return;
                }
                // 业务表不存在
                if (bizInfos.size() != tables.size()) {
                    ackWithBizTableNotExists(bizInfos.keySet(), tables, result);
                    return;
                }
                for (String table : tables) {
                    shadowTables.add(Pradar.addClusterTestPrefix(table));
                }
                shadowInfos = fetchBizTableInfo(bizDataSource, shadowTables, result);
                if (shadowInfos == null) {
                    return;
                }
                break;
            case 2:
                bizInfos = fetchBizTableInfo(bizDataSource, tables, result);
                if (bizInfos == null) {
                    return;
                }
                // 业务表不存在
                if (bizInfos.size() != tables.size()) {
                    ackWithBizTableNotExists(bizInfos.keySet(), tables, result);
                    return;
                }
                for (String table : tables) {
                    shadowTables.add(Pradar.addClusterTestPrefix(table));
                }
                shadowInfos = fetchShadowTableInfo(bizDataSourceClass, shadowDataSource, shadowTables, result);
                if (shadowInfos == null) {
                    return;
                }
            default:
                LOGGER.error("[shadow-preparation] unknown shadow ds type {}", shadowType);
                result.setSuccess(false);
                result.setErrorMsg("未知的隔离类型");
                return;
        }

        // 校验表结构
        Map<String, String> compareResult = shadowType == 0 ? compareTableStructuresForShadowDatabase(shadowInfos, bizInfos) : compareTableStructuresForShadowTable(bizInfos, shadowInfos);
        if (!compareResult.isEmpty()) {
            checkedWithValues(compareResult, result);
            return;
        }
        // 校验表操作权限
        Map<String, String> availableResult = null;
        // 影子表不校验
        if (shadowType != 1) {
            availableResult = checkTableOperationAvailable(bizDataSourceClass, shadowDataSource, shadowTables);
        }
        if (availableResult != null && !availableResult.isEmpty()) {
            checkedWithValues(availableResult, result);
            return;
        }
        LOGGER.info("[shadow-preparation] shadow datasource config check passed!");
        result.setSuccess(true);
    }

    private static void checkedWithValues(Map<String, String> compareResult, ConfigPreCheckResult result) {
        String errorMsg = compareResult.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.joining("; "));
        result.setSuccess(false);
        result.setErrorMsg(errorMsg);
        LOGGER.error("[shadow-preparation] shadow datasource config check failed，result:> {}", errorMsg);
    }


    private static void ackWithBizTableNotExists(Set<String> infoTables, List<String> checkTables, ConfigPreCheckResult result) {
        checkTables.removeAll(infoTables);
        LOGGER.error("[shadow-preparation] shadow datasource config check failed, shadow table not exists: ", checkTables);
        String msg = String.format("业务表:%s不存在", JSON.toJSONString(checkTables));
        result.setSuccess(false);
        result.setErrorMsg(msg);
    }

    private static String fetchDriverClassName(DataSourceEntity entity) {
        String key = DbUrlUtils.getKey(entity.getUrl(), entity.getUserName());
        DataSource dataSource = JdbcDataSourceFetcher.getBizDataSource(key);
        if (dataSource == null) {
            return null;
        }
        return JdbcDataSourceFetcher.fetchDriverClassName(dataSource);
    }

    private static Class extractBizClassForClassLoader(DataSourceEntity entity) {
        String key = DbUrlUtils.getKey(entity.getUrl(), entity.getUserName());
        DataSource dataSource = JdbcDataSourceFetcher.getBizDataSource(key);
        if (dataSource == null) {
            return null;
        }
        return dataSource.getClass();
    }

    /**
     * 读取业务数据源的表结构
     */
    private static Map<String, List<JdbcTableColumnInfos>> fetchBizTableInfo(DataSourceEntity entity, List<String> tables, ConfigPreCheckResult result) {
        String key = DbUrlUtils.getKey(entity.getUrl(), entity.getUserName());
        DataSource dataSource = JdbcDataSourceFetcher.getBizDataSource(key);

        if (dataSource == null) {
            LOGGER.error("[shadow-preparation] can`t find biz datasource with url:{}, username:{}", entity.getUrl(), entity.getUserName());
            result.setSuccess(false);
            result.setErrorMsg(String.format("应用内部找不到指定的业务数据源, url:%s, username:%s", entity.getUrl(), entity.getUserName()));
            return null;
        }
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            return processReadingTableInfo(connection, entity, tables, result);
        } catch (Throwable e) {
            if (e instanceof UndeclaredThrowableException) {
                e = ((UndeclaredThrowableException) e).getUndeclaredThrowable();
            }
            LOGGER.error("[shadow-preparation] fetch table info for biz datasource failed, url:{}, username:{}", entity.getUrl(), entity.getUserName(), e);
            result.setSuccess(false);
            result.setErrorMsg(String.format("读取业务表结构信息时发生异常，异常信息:%s", e.getMessage()));
            return null;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {

                }
            }
        }
    }

    private static Map<String, List<JdbcTableColumnInfos>> fetchShadowTableInfo(Class bizClass, DataSourceEntity entity, List<String> tables, ConfigPreCheckResult result) {
        Connection connection;
        try {
            connection = getConnection(bizClass, entity);
        } catch (Throwable e) {
            if (e instanceof UndeclaredThrowableException) {
                e = ((UndeclaredThrowableException) e).getUndeclaredThrowable();
            }
            LOGGER.error("[shadow-preparation] get shadow connection by DriverManager failed, url:{}, userName:{}", entity.getUrl(), entity.getUserName(), e);
            result.setSuccess(false);
            result.setErrorMsg("连接影子数据库失败，请检查配置信息确保数据源可用，异常信息:" + e.getMessage());
            return null;
        }

        try {
            return processReadingTableInfo(connection, entity, tables, result);
        } catch (Throwable e) {
            if (e instanceof UndeclaredThrowableException) {
                e = ((UndeclaredThrowableException) e).getUndeclaredThrowable();
            }
            LOGGER.error("[shadow-preparation] fetch table info for biz datasource failed, url:{}, username:{}", entity.getUrl(), entity.getUserName(), e);
            result.setSuccess(false);
            result.setErrorMsg(String.format("读取业务表结构信息时发生异常，异常信息:%s", e.getMessage()));
            return null;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {

                }
            }
        }
    }

    private static Map<String, List<JdbcTableColumnInfos>> processReadingTableInfo(Connection connection, DataSourceEntity entity, List<String> tables, ConfigPreCheckResult result) throws Exception {
        JdbcTypeEnum typeEnum = JdbcTypeFetcher.fetchJdbcType(entity.getDriverClassName());
        if (typeEnum == null) {
            LOGGER.error("[shadow-preparation] do not support database type:{}, url{}, username:{}", typeEnum.name(), entity.getUrl(), entity.getUserName());
            result.setSuccess(false);
            result.setErrorMsg(String.format("目前不支持读取数据库类型[%s]的表结构信息", entity.getUserName()));
            return null;
        }
        String database = extractDatabaseFromUrl(entity.getUrl());
        Map<String, List<JdbcTableColumnInfos>> structures = typeEnum.fetchTablesStructures(connection, database, tables);
        return structures;
    }

    private static Map<String, String> checkTableOperationAvailable(Class bizClass, DataSourceEntity entity, List<String> tables) {
        Map<String, String> result = new HashMap<String, String>();
        Connection connection = null;
        try {
            connection = getConnection(bizClass, entity);
            for (String table : tables) {
                Statement statement = null;
                try {
                    statement = connection.createStatement();
                    statement.execute(String.format("select 1 from %s", table));
                } catch (Throwable e) {
                    if (e instanceof UndeclaredThrowableException) {
                        e = ((UndeclaredThrowableException) e).getUndeclaredThrowable();
                    }
                    LOGGER.error("[shadow-preparation] check jdbc shadow datasource available failed, table:{}", table, e);
                    result.put(table, e.getMessage());
                } finally {
                    if (statement != null) {
                        try {
                            statement.close();
                        } catch (Exception e) {
                        }
                    }
                }
            }
        } catch (Throwable e) {
            if (e instanceof UndeclaredThrowableException) {
                e = ((UndeclaredThrowableException) e).getUndeclaredThrowable();
            }
            LOGGER.error("[shadow-preparation] get shadow connection by DriverManager failed, ignore table operation access check, url:{}, userName:{}", entity.getUrl(), entity.getUserName(), e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {

                }
            }
        }
        return result;
    }

    /**
     * 对比表结构, 针对影子库模式, 只对比影子库内存在的表
     *
     * @param shadowInfos
     * @param bizInfos
     */
    private static Map<String, String> compareTableStructuresForShadowDatabase(Map<String, List<JdbcTableColumnInfos>> shadowInfos, Map<String, List<JdbcTableColumnInfos>> bizInfos) {
        Map<String, String> compareResult = new HashMap<String, String>();

        for (Map.Entry<String, List<JdbcTableColumnInfos>> entry : shadowInfos.entrySet()) {
            String shadowTable = entry.getKey();
            List<JdbcTableColumnInfos> shadowColumns = entry.getValue();
            String tableName = shadowTable;
            if (Pradar.isClusterTestPrefix(shadowTable)) {
                tableName = shadowTable.substring(Pradar.getClusterTestPrefix().length());
            }
            List<JdbcTableColumnInfos> bizColumns = bizInfos.get(tableName);
            if (bizColumns == null) {
                compareResult.put(tableName, "业务表不存在");
                continue;
            }
            if (shadowColumns.size() != bizColumns.size()) {
                compareResult.put(tableName, "业务表字段和影子表字段个数不一致");
                continue;
            }
            String ret = compareColumnInfos(toMap(bizColumns), toMap(shadowColumns));
            if (ret != null) {
                compareResult.put(tableName, ret);
            }
        }
        return compareResult;
    }

    /**
     * 对比表结构，针对影子表模式
     *
     * @param bizInfos
     * @param shadowInfos
     */
    private static Map<String, String> compareTableStructuresForShadowTable(Map<String, List<JdbcTableColumnInfos>> bizInfos, Map<String, List<JdbcTableColumnInfos>> shadowInfos) {
        Map<String, String> compareResult = new HashMap<String, String>();

        for (Map.Entry<String, List<JdbcTableColumnInfos>> entry : bizInfos.entrySet()) {
            String tableName = entry.getKey();
            List<JdbcTableColumnInfos> bizColumns = entry.getValue();
            String shadowTable = tableName;
            if (!Pradar.isClusterTestPrefix(tableName)) {
                shadowTable = Pradar.addClusterTestPrefix(tableName);
            }
            List<JdbcTableColumnInfos> shadowColumns = shadowInfos.get(shadowTable);
            if (shadowColumns == null) {
                compareResult.put(tableName, "影子表不存在");
                continue;
            }
            if (bizColumns.size() != shadowColumns.size()) {
                compareResult.put(tableName, "业务表字段和影子表字段个数不一致");
                continue;
            }
            String ret = compareColumnInfos(toMap(bizColumns), toMap(shadowColumns));
            if (ret != null) {
                compareResult.put(tableName, ret);
            }
        }
        return compareResult;
    }


    private static Map<String, JdbcTableColumnInfos> toMap(List<JdbcTableColumnInfos> infos) {
        Map<String, JdbcTableColumnInfos> infosMap = new HashMap<String, JdbcTableColumnInfos>();
        for (JdbcTableColumnInfos info : infos) {
            infosMap.put(info.getColumnName(), info);
        }
        return infosMap;
    }


    private static String compareColumnInfos(Map<String, JdbcTableColumnInfos> bizInfos, Map<String, JdbcTableColumnInfos> shadowInfos) {
        for (Map.Entry<String, JdbcTableColumnInfos> entry : bizInfos.entrySet()) {
            String column = entry.getKey();
            if (!shadowInfos.containsKey(column)) {
                return String.format("影子表字段[%s]不存在", column);
            }
            boolean b = compareColumn(entry.getValue(), shadowInfos.get(column));
            if (!b) {
                return String.format("字段[%s]结构不一致", column);
            }
        }
        return null;
    }

    private static boolean compareColumn(JdbcTableColumnInfos b, JdbcTableColumnInfos s) {
        return compareString(b.getColumnSize(), s.getColumnSize()) && compareString(b.getNullable(), s.getNullable()) && compareString(b.getTypeName(), s.getTypeName()) && compareString(b.getColumnType(), s.getColumnType());
    }

    private static boolean compareString(String b, String s) {
        if ((b == null && s != null) || (b != null && s == null)) {
            return false;
        }
        if (b == null && s == null) {
            return true;
        }
        return b.equals(s);
    }

    /**
     * 获取connection时如果当前class不是业务线程，会有驱动加载不到的问题，绕过去
     *
     * @param clazz
     * @param entity
     * @return
     */
    private static Connection getConnection(Class clazz, DataSourceEntity entity) {
        Method method = ReflectionUtils.findMethod(DriverManager.class, "getConnection", String.class, Properties.class, Class.class);
        Properties info = new java.util.Properties();
        info.put("user", entity.getUserName());
        info.put("password", entity.getPassword());
        return (Connection) ReflectionUtils.invokeMethod(method, null, entity.getUrl(), info, clazz);
    }

    private static String extractDatabaseFromUrl(String url) {
        String database = url.substring(url.lastIndexOf("/") + 1);
        if (database.contains("?")) {
            database = database.substring(0, database.indexOf("?"));
        }
        return database;
    }

}
