package com.pamirs.attach.plugin.shadow.preparation.jdbc;

import com.alibaba.fastjson.JSON;
import com.pamirs.attach.plugin.shadow.preparation.entity.CommandExecuteResult;
import com.pamirs.attach.plugin.shadow.preparation.entity.command.JdbcConfigPushCommand;
import com.pamirs.attach.plugin.shadow.preparation.entity.jdbc.DataSourceConfig;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowDataSourceActiveEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowDataSourceDisableEvent;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;
import com.shulie.instrument.simulator.api.util.CollectionUtils;
import io.shulie.agent.management.client.listener.CommandCallback;
import io.shulie.agent.management.client.model.Command;
import io.shulie.agent.management.client.model.CommandAck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.*;

public class JdbcConfigPushCommandProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(JdbcPrecheckCommandProcessor.class.getName());

    public static void handlerConfigPushCommand(Command command, CommandCallback callback) {
        //先刷新数据源
        JdbcDataSourceFetcher.refreshDataSources();

        JdbcConfigPushCommand cmd = JSON.parseObject(command.getArgs(), JdbcConfigPushCommand.class);
        if (CollectionUtils.isEmpty(cmd.getData())) {
            CommandAck ack = new CommandAck();
            ack.setCommandId(command.getId());
            CommandExecuteResult result = new CommandExecuteResult();
            result.setSuccess(false);
            result.setErrorMsg("未拉取到数据源配置");
            ack.setResponse(JSON.toJSONString(result));
            callback.ack(ack);
            return;
        }

        Object[] compareResult = compareShadowDataSource(toShadowDatabaseConfig(cmd.getData()));
        Set<String> needCloseShadow = (Set<String>) compareResult[0];
        // 有需要关闭的影子数据源
        if (!needCloseShadow.isEmpty()) {
//            publishShadowDataSourceDisableEvent(needCloseShadow);
        }
        Set<ShadowDatabaseConfig> needAdd = (Set<ShadowDatabaseConfig>) compareResult[1];
        if (!needAdd.isEmpty()) {
            publishShadowDataSourceActiveEvent(needAdd);
        }

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

    /**
     * 返回新增的和需要陪关闭的影子数据源
     *
     * @param data
     * @return
     */
    private static Object[] compareShadowDataSource(List<ShadowDatabaseConfig> data) {
        // 需要被关闭的影子数据源
        Set<String> needCloseShadow = new HashSet<String>(JdbcDataSourceFetcher.getShadowKeys());
        // 新增的数据源
        Set<ShadowDatabaseConfig> needAdd = new HashSet<ShadowDatabaseConfig>();

        for (ShadowDatabaseConfig config : data) {
            String shadowKey = DbUrlUtils.getKey(config.getShadowUrl(), config.getShadowUsername());
            // 当前影子数据源存在
            if (needCloseShadow.remove(shadowKey)) {
                continue;
            }
            needAdd.add(config);
        }
        return new Object[]{needCloseShadow, needAdd};
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
            dataSourceMap.put(config ,dataSource);
        }
        LOGGER.info("[shadow-preparation] publish ShadowDataSourceActiveEvent, keys:{}", info);
        EventRouter.router().publish(new ShadowDataSourceActiveEvent(dataSourceMap));
    }

}
