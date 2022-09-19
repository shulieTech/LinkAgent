package com.pamirs.attach.plugin.shadow.preparation.command.processor;

import com.alibaba.fastjson.JSON;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.attach.plugin.shadow.preparation.command.CommandExecuteResult;
import com.pamirs.attach.plugin.shadow.preparation.command.JdbcPreCheckCommand;
import com.pamirs.attach.plugin.shadow.preparation.constants.JdbcTypeEnum;
import com.pamirs.attach.plugin.shadow.preparation.entity.jdbc.DataSourceEntity;
import com.pamirs.attach.plugin.shadow.preparation.entity.jdbc.JdbcTableColumnInfos;
import com.pamirs.attach.plugin.shadow.preparation.jdbc.JdbcDataSourceFetcher;
import com.pamirs.attach.plugin.shadow.preparation.utils.JdbcTypeFetcher;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;
import io.shulie.agent.management.client.model.Command;
import io.shulie.agent.management.client.model.CommandAck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class JdbcPreCheckCommandProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(JdbcPreCheckCommandProcessor.class.getName());

    /**
     * 处理表信息读取命令
     *
     * @param command
     * @param callback
     */
    public static void processPreCheckCommand(Command command, Consumer<CommandAck> callback) {
        LOGGER.info("[shadow-preparation] accept shadow datasource precheck command, content:{}", command.getArgs());
        CommandAck ack = new CommandAck();
        ack.setCommandId(command.getId());
        CommandExecuteResult result = new CommandExecuteResult();

        JdbcPreCheckCommand entity = null;
        try {
            // 服务器上发现有JSON类加载不到的问题
            entity = JSON.parseObject(command.getArgs(), JdbcPreCheckCommand.class);
        } catch (Exception e) {
            LOGGER.error("[shadow-preparation] parse jdbc precheck command occur exception", e);
            result.setSuccess(false);
            result.setResponse("解析校验命令失败");
            ack.setResponse(JSON.toJSONString(result));
            callback.accept(ack);
            return;
        }
        JdbcDataSourceFetcher.refreshDataSources();

        DataSourceEntity bizDataSource = entity.getBizDataSource();
        String driverClassName = fetchDriverClassName(bizDataSource);
        if (driverClassName == null) {
            LOGGER.error("[shadow-preparation] can`t find biz datasource to extract driver className.");
            result.setSuccess(false);
            result.setResponse("读取业务数据源驱动失败");
            ack.setResponse(JSON.toJSONString(result));
            callback.accept(ack);
            return;
        }

        // 0-未设置 1-影子库 2-影子库/影子表 3-影子表
        Integer shadowType = entity.getShadowType();

        if ((shadowType == 1 || shadowType == 2) && entity.getShadowDataSource() == null) {
            LOGGER.error("[shadow-preparation] ds type is shadow database or shadow database table, but shadow datasource is null");
            result.setSuccess(false);
            result.setResponse("影子库/影子库影子表模式时影子数据源不能未空");
            ack.setResponse(JSON.toJSONString(result));
            callback.accept(ack);
            return;
        }

        bizDataSource.setDriverClassName(driverClassName);
        DataSourceEntity shadowDataSource = entity.getShadowDataSource();
        if (shadowDataSource != null) {
            shadowDataSource.setDriverClassName(driverClassName);
        }

        Class<?> bizDataSourceClass = extractBizClassForClassLoader(bizDataSource);

        List<String> tables = entity.getTables();
        List<String> shadowTables = new ArrayList<String>();


        Map<String, List<JdbcTableColumnInfos>> bizInfos, shadowInfos;
        // 1-影子库 2-影子库/影子表 3-影子表
        switch (shadowType) {
            case 1:
                // 如果表为空，执行此步骤会填充业务表名称
                bizInfos = fetchBizTableInfo(command, callback, bizDataSource, tables);
                if (bizInfos == null) {
                    return;
                }
                // 业务表不存在
                if (bizInfos.size() != tables.size()) {
                    achWithBizTableNotExists(command, callback, bizInfos.keySet(), tables);
                    return;
                }
                shadowInfos = fetchShadowTableInfo(bizDataSourceClass, command, callback, shadowDataSource, tables);
                if (shadowInfos == null) {
                    return;
                }
                break;
            case 2:
                bizInfos = fetchBizTableInfo(command, callback, bizDataSource, tables);
                if (bizInfos == null) {
                    return;
                }
                // 业务表不存在
                if (bizInfos.size() != tables.size()) {
                    achWithBizTableNotExists(command, callback, bizInfos.keySet(), tables);
                    return;
                }
                for (String table : tables) {
                    shadowTables.add(Pradar.addClusterTestPrefix(table));
                }
                shadowInfos = fetchShadowTableInfo(bizDataSourceClass, command, callback, shadowDataSource, shadowTables);
                if (shadowInfos == null) {
                    return;
                }
            case 3:
                bizInfos = fetchBizTableInfo(command, callback, bizDataSource, tables);
                if (bizInfos == null) {
                    return;
                }
                // 业务表不存在
                if (bizInfos.size() != tables.size()) {
                    achWithBizTableNotExists(command, callback, bizInfos.keySet(), tables);
                    return;
                }
                for (String table : tables) {
                    shadowTables.add(Pradar.addClusterTestPrefix(table));
                }
                shadowInfos = fetchBizTableInfo(command, callback, bizDataSource, shadowTables);
                if (shadowInfos == null) {
                    return;
                }
                break;
            default:
                LOGGER.error("[shadow-preparation] unknown shadow ds type {}", shadowType);
                result.setSuccess(false);
                result.setResponse("未知的隔离类型");
                ack.setResponse(JSON.toJSONString(result));
                callback.accept(ack);
                return;
        }

        // 校验表结构
        Map<String, String> compareResult = compareTableStructures(shadowType, bizInfos, shadowInfos);
        if (!compareResult.isEmpty()) {
            ackWithFailed(command, callback, compareResult);
            return;
        }
        // 校验表操作权限
        Map<String, String> availableResult = null;
        // 影子表不校验
        if (shadowType != 3) {
            availableResult = checkTableOperationAvailable(bizDataSourceClass, shadowDataSource, shadowTables);
        }
        if (availableResult != null && !availableResult.isEmpty()) {
            ackWithFailed(command, callback, availableResult);
            return;
        }
        LOGGER.info("[shadow-preparation] shadow datasource config check passed!");
        result.setSuccess(true);
        ack.setResponse(JSON.toJSONString(result));
        callback.accept(ack);
    }

    private static void ackWithFailed(Command command, Consumer<CommandAck> callback, Map<String, String> values) {
        ackWithFailed(command, callback, values.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.joining(";\r\n")));
    }

    private static void ackWithFailed(Command command, Consumer<CommandAck> callback, String msg) {
        CommandAck ack = new CommandAck();
        ack.setCommandId(command.getId());
        CommandExecuteResult result = new CommandExecuteResult();
        result.setSuccess(false);
        result.setResponse(msg);
        LOGGER.error("[shadow-preparation] 影子配置校验结不通过，结果> {}", result.getResponse());
        ack.setResponse(JSON.toJSONString(result));
        callback.accept(ack);
    }

    private static void achWithBizTableNotExists(Command command, Consumer<CommandAck> callback, Set<String> infoTables, List<String> checkTables) {
        checkTables.removeAll(infoTables);
        String msg = String.format("业务表:%s不存在", JSON.toJSONString(checkTables));
        ackWithFailed(command, callback, msg);
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
     *
     * @param command
     * @param callback
     * @param entity
     */
    private static Map<String, List<JdbcTableColumnInfos>> fetchBizTableInfo(Command command, Consumer<CommandAck> callback, DataSourceEntity entity, List<String> tables) {
        String key = DbUrlUtils.getKey(entity.getUrl(), entity.getUserName());
        DataSource dataSource = JdbcDataSourceFetcher.getBizDataSource(key);
        CommandAck ack = new CommandAck();
        ack.setCommandId(command.getId());
        CommandExecuteResult result = new CommandExecuteResult();

        if (dataSource == null) {
            LOGGER.error("[shadow-preparation] can`t find biz datasource with url:{}, username:{}", entity.getUrl(), entity.getUserName());
            result.setSuccess(false);
            result.setResponse(String.format("应用内部找不到指定的业务数据源, url:%s, username:%s", entity.getUrl(), entity.getUserName()));
            ack.setResponse(JSON.toJSONString(result));
            callback.accept(ack);
            return null;
        }
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            return processReadingTableInfo(connection, command, callback, entity, tables);
        } catch (Exception e) {
            LOGGER.error("[shadow-preparation] fetch table info for biz datasource failed, url:{}, username:{}", entity.getUrl(), entity.getUserName(), e);
            result.setSuccess(false);
            result.setResponse(String.format("读取业务表结构信息时发生异常，异常信息:%s", e.getMessage()));
            callback.accept(ack);
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

    private static Map<String, List<JdbcTableColumnInfos>> fetchShadowTableInfo(Class bizClass, Command command, Consumer<CommandAck> callback, DataSourceEntity entity, List<String> tables) {
        CommandAck ack = new CommandAck();
        ack.setCommandId(command.getId());
        CommandExecuteResult result = new CommandExecuteResult();
        Connection connection;

        LOGGER.info("[shadow-preparation] get shadow connection by DriverManager, url:{}, userName:{}", entity.getUrl(), entity.getUserName());
        try {
            connection = getConnection(bizClass, entity);
        } catch (Exception e) {
            LOGGER.error("[shadow-preparation] get shadow connection by DriverManager failed, url:{}, userName:{}", entity.getUrl(), entity.getUserName(), e);
            result.setSuccess(false);
            result.setResponse("读取影子表结构信息时发生异常，创建连接失败");
            ack.setResponse(JSON.toJSONString(result));
            callback.accept(ack);
            return null;
        }

        try {
            return processReadingTableInfo(connection, command, callback, entity, tables);
        } catch (Exception e) {
            LOGGER.error("[shadow-preparation] fetch table info for biz datasource failed, url:{}, username:{}", entity.getUrl(), entity.getUserName(), e);
            result.setSuccess(false);
            result.setResponse(String.format("读取业务表结构信息时发生异常，异常信息:%s", e.getMessage()));
            ack.setResponse(JSON.toJSONString(result));
            callback.accept(ack);
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

    private static Map<String, List<JdbcTableColumnInfos>> processReadingTableInfo(Connection connection, Command command, Consumer<CommandAck> callback, DataSourceEntity entity, List<String> tables) throws Exception {
        JdbcTypeEnum typeEnum = JdbcTypeFetcher.fetchJdbcType(entity.getDriverClassName());
        if (typeEnum == null) {
            LOGGER.error("[shadow-preparation] do not support database type:{}, url{}, username:{}", typeEnum.name(), entity.getUrl(), entity.getUserName());
            CommandAck ack = new CommandAck();
            ack.setCommandId(command.getId());
            CommandExecuteResult result = new CommandExecuteResult();
            result.setSuccess(false);
            result.setResponse(String.format("目前不支持读取数据库类型[%s]的表结构信息", entity.getUserName()));
            ack.setResponse(JSON.toJSONString(result));
            callback.accept(ack);
        }
        Map<String, List<JdbcTableColumnInfos>> structures = typeEnum.fetchTablesStructures(connection, tables);
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
                } catch (SQLException e) {
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
        } catch (Exception e) {
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
     * 对比表结构
     *
     * @param bizInfos
     * @param shadowInfos
     */
    private static Map<String, String> compareTableStructures(Integer shadowType, Map<String, List<JdbcTableColumnInfos>> bizInfos, Map<String, List<JdbcTableColumnInfos>> shadowInfos) {
        Map<String, String> compareResult = new HashMap<String, String>();

        for (Map.Entry<String, List<JdbcTableColumnInfos>> entry : bizInfos.entrySet()) {
            String tableName = entry.getKey();
            List<JdbcTableColumnInfos> bizColumns = entry.getValue();
            String shadowTable = tableName;
            if (shadowType > 1 && !Pradar.isClusterTestPrefix(tableName)) {
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
        return compareString(b.getColumnSize(), s.getColumnSize()) && compareString(b.getNullable(), s.getNullable()) && compareString(b.getTypeName(), s.getTypeName());
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

}
