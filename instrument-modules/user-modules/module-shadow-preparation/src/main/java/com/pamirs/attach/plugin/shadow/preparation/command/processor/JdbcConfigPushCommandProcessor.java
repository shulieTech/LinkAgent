package com.pamirs.attach.plugin.shadow.preparation.command.processor;

import com.alibaba.fastjson.JSON;
import com.pamirs.attach.plugin.shadow.preparation.command.CommandExecuteResult;
import com.pamirs.attach.plugin.shadow.preparation.command.JdbcConfigPushCommand;
import com.pamirs.attach.plugin.shadow.preparation.entity.jdbc.DataSourceConfig;
import com.pamirs.attach.plugin.shadow.preparation.jdbc.JdbcDataSourceFetcher;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowDataSourceActiveEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowDataSourceDisableEvent;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;
import com.shulie.instrument.simulator.api.executors.ExecutorServiceFactory;
import com.shulie.instrument.simulator.api.util.CollectionUtils;
import com.shulie.instrument.simulator.api.util.StringUtil;
import io.shulie.agent.management.client.listener.CommandCallback;
import io.shulie.agent.management.client.model.Command;
import io.shulie.agent.management.client.model.CommandAck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class JdbcConfigPushCommandProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(JdbcPrecheckCommandProcessor.class.getName());

    private static Future future;

    public static void processConfigPushCommand(final Command command, final CommandCallback callback) {
        //先刷新数据源
        JdbcDataSourceFetcher.refreshDataSources();

        JdbcConfigPushCommand cmd = JSON.parseObject(command.getArgs(), JdbcConfigPushCommand.class);
        if (CollectionUtils.isEmpty(cmd.getData())) {
            CommandAck ack = new CommandAck();
            ack.setCommandId(command.getId());
            CommandExecuteResult result = new CommandExecuteResult();
            result.setSuccess(false);
            result.setResponse("未拉取到数据源配置");
            ack.setResponse(JSON.toJSONString(result));
            callback.ack(ack);
            return;
        }

        final List<ShadowDatabaseConfig> configs = toShadowDatabaseConfig(cmd.getData());
        Object[] compareResult = compareShadowDataSource(configs);
        Set<String> needClosed = (Set<String>) compareResult[0];
        // 有需要关闭的影子数据源
        if (!needClosed.isEmpty()) {
            publishShadowDataSourceDisableEvent(needClosed);
            JdbcDataSourceFetcher.removeShadowDataSources(needClosed);
        }

        Set<ShadowDatabaseConfig> needAdd = (Set<ShadowDatabaseConfig>) compareResult[1];
        if (!needAdd.isEmpty()) {
            publishShadowDataSourceActiveEvent(needAdd);
        }

        if (needAdd.isEmpty()) {
            // 数据已生效
            CommandAck ack = new CommandAck();
            ack.setCommandId(command.getId());
            CommandExecuteResult result = new CommandExecuteResult();
            result.setSuccess(true);
            ack.setResponse(JSON.toJSONString(result));
            callback.ack(ack);
            return;
        }
        GlobalConfig.getInstance().setShadowDatabaseConfigs(toMap(configs), true);
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
        // 注册一个延时任务一分钟后查看数据源是否生效
        future = ExecutorServiceFactory.getFactory().schedule(new Runnable() {
            @Override
            public void run() {
                validateShadowConfigActivation(command, callback, configs);
            }
        }, 10, TimeUnit.SECONDS);
    }

    private static List<ShadowDatabaseConfig> toShadowDatabaseConfig(List<DataSourceConfig> data) {
        List<ShadowDatabaseConfig> values = new ArrayList<ShadowDatabaseConfig>();
        for (DataSourceConfig c : data) {
            ShadowDatabaseConfig config = new ShadowDatabaseConfig();
            config.setUrl(c.getUrl());
            config.setUsername(c.getUsername());
            config.setShadowUrl(c.getShadowUrl());
            config.setShadowUsername(c.getShadowUsername());
            config.setShadowPassword(c.getShadowPassword());
            config.setDsType(c.getDsType());
            values.add(config);
        }
        return values;
    }

    private static Map<String, ShadowDatabaseConfig> toMap(List<ShadowDatabaseConfig> data) {
        Map<String, ShadowDatabaseConfig> map = new HashMap<String, ShadowDatabaseConfig>(data.size());
        for (ShadowDatabaseConfig config : data) {
            map.put(DbUrlUtils.getKey(config.getUrl(), config.getUsername()), config);
        }
        return map;
    }

    /**
     * 返回新增的和需要陪关闭的影子数据源
     *
     * @param data
     * @return
     */
    private static Object[] compareShadowDataSource(List<ShadowDatabaseConfig> data) {
        // 需要被关闭的影子数据源
        Set<String> needClosed = new HashSet<String>(JdbcDataSourceFetcher.getShadowKeys());
        // 新增的数据源
        Set<ShadowDatabaseConfig> needAdd = new HashSet<ShadowDatabaseConfig>();

        for (ShadowDatabaseConfig config : data) {
            String shadowKey = DbUrlUtils.getKey(config.getShadowUrl(), config.getShadowUsername());
            // 当前影子数据源存在
            if (needClosed.remove(shadowKey)) {
                continue;
            }
            needAdd.add(config);
        }
        return new Object[]{needClosed, needAdd};
    }

    private static void publishShadowDataSourceDisableEvent(Set<String> keys) {
        EventRouter.router().publish(new ShadowDataSourceDisableEvent(keys));
        LOGGER.info("[shadow-preparation] publish ShadowDataSourceDisableEvent, keys:{}", keys);
    }

    private static void publishShadowDataSourceActiveEvent(Set<ShadowDatabaseConfig> databaseConfigs) {
        StringBuilder info = new StringBuilder();
        Map<ShadowDatabaseConfig, DataSource> dataSourceMap = new HashMap<ShadowDatabaseConfig, DataSource>();
        for (ShadowDatabaseConfig config : databaseConfigs) {
            String key = DbUrlUtils.getKey(config.getUrl(), config.getUsername());
            DataSource dataSource = JdbcDataSourceFetcher.getBizDataSource(key);
            if (dataSource == null) {
                LOGGER.error("[shadow-preparation] active shadow datasource failed with reason can`t find biz datasource with key:{}, ignore datasource", key);
                continue;
            }
            info.append("key").append(",");
            dataSourceMap.put(config, dataSource);
        }
        if (dataSourceMap.isEmpty()) {
            return;
        }
        LOGGER.info("[shadow-preparation] publish ShadowDataSourceActiveEvent, keys:{}", info);
        EventRouter.router().publish(new ShadowDataSourceActiveEvent(dataSourceMap));
    }

    /**
     * 校验配置是否生效
     *
     * @param command
     * @param callback
     * @param configs
     */
    private static void validateShadowConfigActivation(Command command, CommandCallback callback, List<ShadowDatabaseConfig> configs) {
        JdbcDataSourceFetcher.refreshDataSources();
        Set<String> shadowKeys = JdbcDataSourceFetcher.getShadowKeys();
        StringBuilder sb = new StringBuilder();

        for (ShadowDatabaseConfig config : configs) {
            String key = DbUrlUtils.getKey(config.getShadowUrl(), config.getShadowUsername());
            if (!shadowKeys.contains(key)) {
                LOGGER.error("[shadow-preparation] shadow datasource config with url:{}, username:{} not activated", config.getShadowUrl(), config.getUsername());
                sb.append(String.format("影子配置 url:%s, username:%s 未生效", config.getUrl(), config.getUsername())).append(",");
            } else {
                LOGGER.info("[shadow-preparation] shadow datasource config with url:{}, username:{} active success", config.getShadowUrl(), config.getUsername());
            }
        }
        String info = sb.toString();

        CommandAck ack = new CommandAck();
        ack.setCommandId(command.getId());
        CommandExecuteResult result = new CommandExecuteResult();
        if (StringUtil.isEmpty(info)) {
            result.setSuccess(true);
        } else {
            result.setSuccess(false);
            result.setResponse(info);
        }
        ack.setResponse(JSON.toJSONString(result));
        callback.ack(ack);
    }

}
