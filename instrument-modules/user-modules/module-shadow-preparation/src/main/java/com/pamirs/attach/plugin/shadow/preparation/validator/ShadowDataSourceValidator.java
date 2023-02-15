package com.pamirs.attach.plugin.shadow.preparation.validator;

import com.alibaba.fastjson.JSON;
import com.pamirs.attach.plugin.shadow.preparation.ShadowPreparationModule;
import com.pamirs.attach.plugin.shadow.preparation.command.JdbcPreCheckCommand;
import com.pamirs.attach.plugin.shadow.preparation.command.processor.JdbcPreCheckCommandProcessor;
import com.pamirs.attach.plugin.shadow.preparation.jdbc.entity.ConfigPreCheckResult;
import com.pamirs.attach.plugin.shadow.preparation.jdbc.entity.DataSourceEntity;
import com.pamirs.pradar.AppNameUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.resource.SimulatorConfig;
import io.shulie.takin.sdk.kafka.HttpSender;
import io.shulie.takin.sdk.kafka.MessageSendCallBack;
import io.shulie.takin.sdk.kafka.MessageSendService;
import io.shulie.takin.sdk.pinpoint.impl.PinpointSendServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShadowDataSourceValidator {

    private final static Logger LOGGER = LoggerFactory.getLogger(ShadowPreparationModule.class);

    public static SimulatorConfig simulatorConfig;
    public static String scheduleInterval;

    private static MessageSendService service = new PinpointSendServiceFactory().getKafkaMessageInstance();

    public static void checkShadowDataSourceAvailable() {

        List<ConfigPreCheckResult> resultList = new ArrayList<>();
        Map<String, ShadowDatabaseConfig> configs = GlobalConfig.getInstance().getShadowDatasourceConfigs();

        for (Map.Entry<String, ShadowDatabaseConfig> entry : configs.entrySet()) {
            ShadowDatabaseConfig config = entry.getValue();

            JdbcPreCheckCommand command = new JdbcPreCheckCommand();
            command.setShadowType(config.getDsType());
            command.setTables(new ArrayList<>(config.getBusinessShadowTables().keySet()));

            DataSourceEntity bizDataSource = new DataSourceEntity();
            bizDataSource.setUrl(config.getUrl());
            bizDataSource.setUserName(config.getUsername());
            bizDataSource.setDriverClassName(config.getShadowDriverClassName());
            command.setBizDataSource(bizDataSource);

            DataSourceEntity shadowDataSource = new DataSourceEntity();
            shadowDataSource.setUrl(config.getShadowUrl());
            shadowDataSource.setUserName(config.getShadowUsername());
            shadowDataSource.setPassword(config.getShadowPassword());
            shadowDataSource.setDriverClassName(config.getShadowDriverClassName());
            command.setShadowDataSource(shadowDataSource);

            ConfigPreCheckResult result = new ConfigPreCheckResult();
            result.setConfigType("datasource");
            result.setConfigKey(entry.getKey());
            result.setAppName(AppNameUtils.appName());
            result.setTenantCode(Pradar.getProperty("tenant.app.key"));
            result.setEnvCode(Pradar.getEnvCode());
            result.setAgentId(simulatorConfig.getAgentId());
            result.setCheckTime(System.currentTimeMillis());
            result.setCheckInterval(scheduleInterval);
            try {
                JdbcPreCheckCommandProcessor.processPreCheckCommand(command, result);
                result.setHostIp(InetAddress.getLocalHost().getHostAddress());
            } catch (Throwable e) {
                result.setSuccess(false);
                result.setErrorMsg(e.getMessage());
            }
            resultList.add(result);
        }

        String basePath = "";

        service.send(basePath, new HashMap<>(), JSON.toJSONString(resultList),
                new MessageSendCallBack() {

                    @Override
                    public void success() {
                    }

                    @Override
                    public void fail(String errorMessage) {
                        LOGGER.error("[shadow-preparation] send shadow datasource config check result failed", basePath, errorMessage);
                    }
                }, new HttpSender() {
                    @Override
                    public void sendMessage() {

                    }
                });

    }

}
