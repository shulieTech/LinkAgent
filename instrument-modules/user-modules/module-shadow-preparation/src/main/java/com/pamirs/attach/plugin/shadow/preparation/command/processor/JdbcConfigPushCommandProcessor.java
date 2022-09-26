package com.pamirs.attach.plugin.shadow.preparation.command.processor;

import com.alibaba.fastjson.JSON;
import com.pamirs.attach.plugin.shadow.preparation.command.JdbcConfigPushCommand;
import com.pamirs.attach.plugin.shadow.preparation.jdbc.entity.DataSourceConfig;
import com.pamirs.attach.plugin.shadow.preparation.jdbc.JdbcDataSourceFetcher;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowDataSourceActiveEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowDataSourceDisableEvent;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;
import com.shulie.instrument.simulator.api.executors.ExecutorServiceFactory;
import com.shulie.instrument.simulator.api.util.CollectionUtils;
import com.shulie.instrument.simulator.api.util.StringUtil;
import io.shulie.agent.management.client.constant.ConfigResultEnum;
import io.shulie.agent.management.client.model.Config;
import io.shulie.agent.management.client.model.ConfigAck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


public class JdbcConfigPushCommandProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(JdbcPreCheckCommandProcessor.class.getName());

    private static Future future;

    public static void processConfigPushCommand(final Config config, final Consumer<ConfigAck> callback) {
        LOGGER.info("[shadow-preparation] accept shadow datasource push command, content:{}", config.getParam());
        //先刷新数据源
        JdbcDataSourceFetcher.refreshDataSources();
        ConfigAck ack = new ConfigAck();
        ack.setType(config.getType());
        ack.setVersion(config.getVersion());

        JdbcConfigPushCommand cmd = null;
        try {
            cmd = JSON.parseObject(config.getParam(), JdbcConfigPushCommand.class);
        } catch (Exception e) {
            LOGGER.error("[shadow-preparation] parse jdbc config push command occur exception", e);
            ack.setResultCode(ConfigResultEnum.FAIL.getCode());
            ack.setResultDesc("解析数据源下发命令失败");
            callback.accept(ack);
        }
        if (CollectionUtils.isEmpty(cmd.getData())) {
            ack.setResultCode(ConfigResultEnum.FAIL.getCode());
            ack.setResultDesc("未拉取到数据源配置");
            callback.accept(ack);
            return;
        }

        final List<ShadowDatabaseConfig> configs = toShadowDatabaseConfig(cmd.getData());
        GlobalConfig.getInstance().setShadowDatabaseConfigs(toMap(configs), true);

        Object[] compareResult = compareShadowDataSource(configs);
        Set<String> needClosed = (Set<String>) compareResult[0];

        // 有需要关闭的影子数据源
        if (!needClosed.isEmpty()) {
            publishShadowDataSourceDisableEvents(needClosed);
            JdbcDataSourceFetcher.removeShadowDataSources(needClosed);
        }

        Set<ShadowDatabaseConfig> needAdd = (Set<ShadowDatabaseConfig>) compareResult[1];
        if (!needAdd.isEmpty()) {
            publishShadowDataSourceActiveEvent(needAdd);
        }

        if (needAdd.isEmpty()) {
            // 数据已生效
            ack.setResultCode(ConfigResultEnum.SUCC.getCode());
            ack.setResultDesc("数据配置已生效");
            callback.accept(ack);
            return;
        }
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
        // 注册一个延时任务30s后查看数据源是否生效
        future = ExecutorServiceFactory.getFactory().schedule(() -> validateShadowConfigActivation(config, callback, configs), 30, TimeUnit.SECONDS);
    }

    private static List<ShadowDatabaseConfig> toShadowDatabaseConfig(List<DataSourceConfig> data) {
        List<ShadowDatabaseConfig> values = new ArrayList<ShadowDatabaseConfig>();
        for (DataSourceConfig c : data) {
            ShadowDatabaseConfig config = new ShadowDatabaseConfig();
            config.setUrl(c.getUrl());
            config.setUsername(c.getUsername());
            int shadowType = c.getShadowType();
            Integer dsType = shadowType == 1 ? 0 : shadowType == 2 ? 2 : shadowType == 3 ? 1 : null;
            if (dsType == null) {
                LOGGER.error("[shadow-preparation] illegal shadow type {}, config:{}", shadowType, JSON.toJSONString(c));
                continue;
            }
            config.setDsType(dsType);
            if ((dsType == 1)) {
                // 一张影子表都没有
                if (CollectionUtils.isEmpty(c.getBizTables())) {
                    config.setBusinessShadowTables(new HashMap<>());
                    LOGGER.info("[shadow-preparation] disable shadow table config with no tables! config: {}", JSON.toJSONString(c));
                } else {
                    Map<String, String> businessShadowTables = new HashMap<>();
                    for (String table : c.getBizTables()) {
                        businessShadowTables.put(table, Pradar.addClusterTestPrefix(table));
                    }
                    config.setBusinessShadowTables(businessShadowTables);
                }
            } else {
                // 非影子表模式
                config.setShadowUrl(c.getShadowUrl());
                config.setShadowUsername(c.getShadowUsername());
                config.setShadowPassword(c.getShadowPassword());
            }
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
            // 影子表模式直接加，反正不用创建数据源
            if (config.getDsType() == 1) {
                needAdd.add(config);
                continue;
            }
            String shadowKey = DbUrlUtils.getKey(config.getShadowUrl(), config.getShadowUsername());
            // 当前影子数据源存在
            if (needClosed.remove(shadowKey)) {
                continue;
            }
            needAdd.add(config);
        }
        // 遇到特殊情况, 多个业务数据源的影子数据源是一样的, 需要禁用其中一个
        if (needClosed.isEmpty() && data.size() < JdbcDataSourceFetcher.getShadowDataSourceNum()) {
            // 因为没有保存业务数据源和影子数据源的映射关系，所以清除所有影子数据源，重新构建
            return new Object[]{new HashSet<String>(JdbcDataSourceFetcher.getShadowKeys()), data};
        }
        return new Object[]{needClosed, needAdd};
    }

    private static void publishShadowDataSourceDisableEvents(Set<String> urlUsernames) {
        for (String key : urlUsernames) {
            String dataSourceClass = getShadowDataSourceClass(key);
            if (dataSourceClass == null) {
                LOGGER.error("[shadow-preparation] can`t find shadow datasource class for key:{}, ignore disable it.", key);
                continue;
            }
            EventRouter.router().publish(new ShadowDataSourceDisableEvent(new AbstractMap.SimpleEntry<>(dataSourceClass, key)));
            LOGGER.info("[shadow-preparation] publish ShadowDataSourceDisableEvent, key:{}", key);
        }
    }

    private static String getShadowDataSourceClass(String shadowKey) {
        DataSource dataSource = JdbcDataSourceFetcher.getShadowDataSource(shadowKey);
        return dataSource == null ? null : dataSource.getClass().getName();
    }

    private static void publishShadowDataSourceActiveEvent(Set<ShadowDatabaseConfig> databaseConfigs) {
        for (ShadowDatabaseConfig config : databaseConfigs) {
            String key = DbUrlUtils.getKey(config.getUrl(), config.getUsername());
            DataSource dataSource = JdbcDataSourceFetcher.getBizDataSource(key);
            if (dataSource == null) {
                LOGGER.error("[shadow-preparation] active shadow datasource failed with reason can`t find biz datasource with key:{}, ignore datasource", key);
                continue;
            }
            LOGGER.info("[shadow-preparation] publish ShadowDataSourceActiveEvent, key:{}", key);
            EventRouter.router().publish(new ShadowDataSourceActiveEvent(new AbstractMap.SimpleEntry<>(config, dataSource)));
        }
    }

    /**
     * 校验配置是否生效
     *
     * @param callback
     * @param configs
     */
    private static void validateShadowConfigActivation(Config cfg, Consumer<ConfigAck> callback, List<ShadowDatabaseConfig> configs) {
        JdbcDataSourceFetcher.refreshDataSources();
        Set<String> shadowKeys = JdbcDataSourceFetcher.getShadowKeys();
        StringBuilder sb = new StringBuilder();

        for (ShadowDatabaseConfig config : configs) {
            // 影子表模式不校验
            if (config.getDsType() == 1) {
                LOGGER.info("[shadow-preparation] shadow datasource config with url:{}, username:{} active success", config.getUrl(), config.getUsername());
                continue;
            }
            String key = DbUrlUtils.getKey(config.getShadowUrl(), config.getShadowUsername());
            if (!shadowKeys.contains(key)) {
                LOGGER.error("[shadow-preparation] shadow datasource config with url:{}, username:{} not activated", config.getUrl(), config.getUsername());
                sb.append(String.format("影子配置 url:%s, username:%s 未生效", config.getUrl(), config.getUsername())).append(",");
            } else {
                LOGGER.info("[shadow-preparation] shadow datasource config with url:{}, username:{} active success", config.getUrl(), config.getUsername());
            }
        }
        String info = sb.toString();

        ConfigAck ack = new ConfigAck();
        ack.setType(cfg.getType());
        ack.setVersion(cfg.getVersion());
        if (StringUtil.isEmpty(info)) {
            ack.setResultCode(ConfigResultEnum.SUCC.getCode());
        } else {
            ack.setResultCode(ConfigResultEnum.FAIL.getCode());
            ack.setResultDesc(info);
        }
        callback.accept(ack);
    }

}
