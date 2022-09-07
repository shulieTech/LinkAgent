package com.pamirs.attach.plugin.shadow.preparation;

import com.alibaba.fastjson.JSON;
import com.pamirs.attach.plugin.shadow.preparation.constants.AgentType;
import com.pamirs.attach.plugin.shadow.preparation.entity.command.JdbcConfigPushCommand;
import com.pamirs.attach.plugin.shadow.preparation.entity.command.JdbcPrecheckCommand;
import com.pamirs.attach.plugin.shadow.preparation.entity.jdbc.DataSourceConfig;
import com.pamirs.attach.plugin.shadow.preparation.entity.jdbc.DataSourceEntity;
import com.pamirs.attach.plugin.shadow.preparation.jdbc.JdbcConfigPushCommandProcessor;
import com.pamirs.attach.plugin.shadow.preparation.jdbc.JdbcDataSourceFetcher;
import com.pamirs.attach.plugin.shadow.preparation.jdbc.JdbcPrecheckCommandProcessor;
import com.pamirs.pradar.AppNameUtils;
import com.pamirs.pradar.Pradar;
import com.shulie.instrument.simulator.agent.api.utils.HeartCommandUtils;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.executors.ExecutorServiceFactory;
import io.shulie.agent.management.client.AgentManagementClient;
import io.shulie.agent.management.client.constant.CommandType;
import io.shulie.agent.management.client.listener.CommandCallback;
import io.shulie.agent.management.client.listener.CommandListener;
import io.shulie.agent.management.client.listener.ConfigCallback;
import io.shulie.agent.management.client.listener.ConfigListener;
import io.shulie.agent.management.client.model.Command;
import io.shulie.agent.management.client.model.Config;
import io.shulie.agent.management.client.model.ConfigProperties;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "shadow-preparation", version = "1.0.0", author = "jiangjibo@shulie.io", description = "影子资源准备工作，包括创建，校验")
public class ShadowPreparationModule extends ModuleLifecycleAdapter implements ExtensionModule {

    private final static Logger LOGGER = LoggerFactory.getLogger(ShadowPreparationModule.class.getName());

    @Override
    public boolean onActive() throws Throwable {
//        registerAgentManagerListener();

        // 每隔5分钟刷新一次数据源
        ExecutorServiceFactory.getFactory().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                JdbcDataSourceFetcher.refreshDataSources();
                handlerConfigPushCommand();
            }
        }, 1, 2, TimeUnit.MINUTES);

        return true;
    }

    private void handlerPreCheckCommand() {
        JdbcPrecheckCommand command = new JdbcPrecheckCommand();
        DataSourceEntity bizDataSource = new DataSourceEntity();
        bizDataSource.setUrl("jdbc:mysql://192.168.1.46:3306/atester?useUnicode=true");
        bizDataSource.setUserName("admin");

        DataSourceEntity shadowDataSource = new DataSourceEntity();
        shadowDataSource.setUrl("jdbc:mysql://192.168.1.46:3306/pt_atester?useUnicode=true");
        shadowDataSource.setUserName("admin");
        shadowDataSource.setPassword("athene.admin");

        command.setShadowType(0);
        command.setBizDataSource(bizDataSource);
        command.setShadowDataSource(shadowDataSource);
        command.setTables(Arrays.asList("m_user", "user"));

        Command cmd = new Command();
        cmd.setId("11111");
        cmd.setArgs(JSON.toJSONString(command));
        JdbcPrecheckCommandProcessor.handlerPreCheckCommand(cmd, null);
    }

    private void handlerConfigPushCommand(){
        JdbcConfigPushCommand command = new JdbcConfigPushCommand();
        DataSourceConfig config = new DataSourceConfig();
        config.setUrl("jdbc:mysql://192.168.1.46:3306/atester?useUnicode=true");
        config.setUsername("admin");
        config.setShadowUrl("jdbc:mysql://192.168.1.46:3306/pt_atester_1?useUnicode=true");
        config.setShadowUsername("admin");
        config.setShadowPassword("athene.admin");
        config.setDsType(0);
        command.setData(Arrays.asList(config));

        Command cmd = new Command();
        cmd.setId("11111");
        cmd.setArgs(JSON.toJSONString(command));

        JdbcConfigPushCommandProcessor.handlerConfigPushCommand(cmd, null);
    }

    private void registerAgentManagerListener() {
        ConfigProperties properties = new ConfigProperties();
        properties.setAppName(AppNameUtils.appName());
        properties.setTenantCode(Pradar.getPradarTenantKey());
        properties.setUserId(Pradar.getPradarUserId());
        properties.setEnvCode(Pradar.getEnvCode());
        properties.setSpecification(AgentType.simulator_agent.getType());
        properties.setVersion(HeartCommandUtils.SIMULATOR_VERSION);
        properties.setAgentId(System.getProperty("simulator.agent.id"));
        AgentManagementClient client = new AgentManagementClient(null, properties);

        client.register(CommandType.DATABASE, new CommandListener() {
            @Override
            public void receive(Command command, CommandCallback commandCallback) {
                String id = command.getId();
                if ("precheck".equals(id)) {
                    JdbcPrecheckCommandProcessor.handlerPreCheckCommand(command, commandCallback);
                } else if ("config".equals(id)) {
                    JdbcConfigPushCommandProcessor.handlerConfigPushCommand(command, commandCallback);
                }
            }
        });

        client.register("", new ConfigListener() {
            @Override
            public void receive(Config config, ConfigCallback configCallback) {

            }
        });
    }

}
