package com.pamirs.attach.plugin.shadow.preparation.jdbc;

import com.alibaba.fastjson.JSON;
import com.pamirs.attach.plugin.shadow.preparation.constants.JdbcTypeEnum;
import com.pamirs.attach.plugin.shadow.preparation.entity.CommandExecuteResult;
import com.pamirs.attach.plugin.shadow.preparation.entity.command.JdbcPrecheckCommand;
import com.pamirs.attach.plugin.shadow.preparation.entity.jdbc.*;
import com.pamirs.attach.plugin.shadow.preparation.utils.JdbcTypeFetcher;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;
import io.shulie.agent.management.client.listener.CommandCallback;
import io.shulie.agent.management.client.model.Command;
import io.shulie.agent.management.client.model.CommandAck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JdbcCommandProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(JdbcCommandProcessor.class.getName());

    /**
     * 处理表信息读取命令
     *
     * @param command
     * @param callback
     */
    public static void executePrecheckCommand(Command command, CommandCallback callback) {
        JdbcPrecheckCommand entity = JSON.parseObject(command.getArgs(), JdbcPrecheckCommand.class);

        DataSourceEntity bizDataSource = entity.getBizDataSource();
        String driverClassName = fetchDriverClassName(bizDataSource);
        if (driverClassName == null) {
            CommandAck ack = new CommandAck();
            ack.setCommandId(command.getId());
            CommandExecuteResult result = new CommandExecuteResult();
            result.setSuccess(false);
            result.setErrorMsg("读取业务数据源驱动失败");
            ack.setResponse(JSON.toJSONString(result));
            callback.ack(ack);
        }

        bizDataSource.setDriverClassName(driverClassName);
        DataSourceEntity shadowDataSource = entity.getShadowDataSource();
        shadowDataSource.setDriverClassName(driverClassName);

        // 数据源类型 0:影子库 1:影子表 2:影子库+影子表
        Integer shadowType = entity.getShadowType();
        List<String> tables = entity.getTables();
        List<String> shadowTables = new ArrayList<String>();
        if (shadowType > 0) {
            for (String table : tables) {
                shadowTables.add(Pradar.addClusterTestPrefix(table));
            }
        }

        Map<String, List<JdbcTableColumnInfos>> bizInfos = null, shadowInfos = null;
        switch (shadowType) {
            case 0:
                bizInfos = fetchBizTableInfo(command, callback, bizDataSource, tables);
                if (bizInfos == null) {
                    return;
                }
                shadowInfos = fetchShadowTableInfo(command, callback, shadowDataSource, tables);
                if (shadowInfos == null) {
                    return;
                }
                break;
            case 1:
                bizInfos = fetchBizTableInfo(command, callback, bizDataSource, tables);
                if (bizInfos == null) {
                    return;
                }
                shadowInfos = fetchBizTableInfo(command, callback, bizDataSource, shadowTables);
                if (shadowInfos == null) {
                    return;
                }
                break;
            case 2:
                bizInfos = fetchBizTableInfo(command, callback, bizDataSource, tables);
                if (bizInfos == null) {
                    return;
                }
                shadowInfos = fetchShadowTableInfo(command, callback, shadowDataSource, shadowTables);
                if (shadowInfos == null) {
                    return;
                }
        }

        CommandAck ack = new CommandAck();
        ack.setCommandId(command.getId());
        CommandExecuteResult result = new CommandExecuteResult();

        Map<String, String> values = compareTableStructures(shadowType, bizInfos, shadowInfos);
        if (values.isEmpty()) {
            result.setSuccess(true);
        } else {
            result.setSuccess(false);
            result.setErrorMsg(JSON.toJSONString(values));
        }

        ack.setResponse(JSON.toJSONString(result));
        callback.ack(ack);

    }

    private static String fetchDriverClassName(DataSourceEntity entity) {
        String key = DbUrlUtils.getKey(entity.getUrl(), entity.getUserName());
        DataSource dataSource = JdbcDataSourceFetcher.getBizDataSource(key);
        if (dataSource == null) {
            return null;
        }
        return JdbcDataSourceFetcher.fetchDriverClassName(dataSource);
    }

    /**
     * 读取业务数据源的表结构
     *
     * @param command
     * @param callback
     * @param entity
     */
    private static Map<String, List<JdbcTableColumnInfos>> fetchBizTableInfo(Command command, CommandCallback callback, DataSourceEntity entity, List<String> tables) {
        String key = DbUrlUtils.getKey(entity.getUrl(), entity.getUserName());
        DataSource dataSource = JdbcDataSourceFetcher.getBizDataSource(key);
        CommandAck ack = new CommandAck();
        ack.setCommandId(command.getId());
        CommandExecuteResult result = new CommandExecuteResult();

        if (dataSource == null) {
            LOGGER.error("[shadow-preparation] can`t find biz datasource with url:{}, username:{}", entity.getUrl(), entity.getUserName());
            result.setSuccess(false);
            result.setErrorMsg(String.format("应用内部找不到指定的业务数据源, url:%s, username:%s", entity.getUrl(), entity.getUserName()));
            ack.setResponse(JSON.toJSONString(result));
            callback.ack(ack);
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
            callback.ack(ack);
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

    private static Map<String, List<JdbcTableColumnInfos>> fetchShadowTableInfo(Command command, CommandCallback callback, DataSourceEntity entity, List<String> tables) {
        String key = DbUrlUtils.getKey(entity.getUrl(), entity.getUserName());
        DataSource dataSource = JdbcDataSourceFetcher.getShadowDataSource(key);
        CommandAck ack = new CommandAck();
        ack.setCommandId(command.getId());
        CommandExecuteResult result = new CommandExecuteResult();
        Connection connection = null;

        if (dataSource == null) {
            LOGGER.info("[shadow-preparation] get shadow connection by DriverManager, url:{}, userName:{}", entity.getUrl(), entity.getUserName());
            try {
                Class.forName(entity.getDriverClassName());
                connection = DriverManager.getConnection(entity.getUrl(), entity.getUserName(), entity.getPassword());
            } catch (Exception e) {
                LOGGER.error("[shadow-preparation] get shadow connection by DriverManager failed, url:{}, userName:{}", entity.getUrl(), entity.getUserName(), e);
                result.setSuccess(false);
                result.setResponse(String.format("读取影子表结构信息时发生异常，创建连接失败，异常信息:%s", e.getMessage()));
                callback.ack(ack);
                return null;
            }
        }

        try {
            if (connection == null) {
                connection = dataSource.getConnection();
            }
            return processReadingTableInfo(connection, command, callback, entity, tables);
        } catch (Exception e) {
            LOGGER.error("[shadow-preparation] fetch table info for biz datasource failed, url:{}, username:{}", entity.getUrl(), entity.getUserName(), e);
            result.setSuccess(false);
            result.setResponse(String.format("读取业务表结构信息时发生异常，异常信息:%s", e.getMessage()));
            callback.ack(ack);
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

    private static Map<String, List<JdbcTableColumnInfos>> processReadingTableInfo(Connection connection, Command command, CommandCallback callback, DataSourceEntity entity, List<String> tables) throws Exception {
        CommandAck ack = new CommandAck();
        ack.setCommandId(command.getId());
        CommandExecuteResult result = new CommandExecuteResult();

        JdbcTypeEnum typeEnum = JdbcTypeFetcher.fetchJdbcType(entity.getDriverClassName());
        Map<String, List<JdbcTableColumnInfos>> structures = typeEnum.fetchTablesStructures(connection, tables);
        if (structures == null) {
            LOGGER.error("[shadow-preparation] do not support database type:{}, url{}, username:{}", typeEnum.name(), entity.getUrl(), entity.getUserName());
            result.setSuccess(false);
            result.setErrorMsg(String.format("目前不支持读取数据库类型[%s]的表结构信息", entity.getUserName()));
            ack.setResponse(JSON.toJSONString(result));
            callback.ack(ack);
        }
        return structures;
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
            if (shadowType > 0 && !Pradar.isClusterTestPrefix(tableName)) {
                shadowTable = Pradar.addClusterTestPrefix(tableName);
            }
            List<JdbcTableColumnInfos> shadowColumns = shadowInfos.get(shadowTable);
            if (shadowColumns == null) {
                compareResult.put(tableName, "影子表不存在");
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
        if (bizInfos.size() != shadowInfos.size()) {
            return "业务表字段和影子表字段个数不一致";
        }
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
        return b.equals(s);
    }

}
