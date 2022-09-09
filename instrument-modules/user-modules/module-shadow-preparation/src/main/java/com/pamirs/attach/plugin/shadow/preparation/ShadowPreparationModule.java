package com.pamirs.attach.plugin.shadow.preparation;

import com.alibaba.fastjson.JSON;
import com.pamirs.attach.plugin.shadow.preparation.command.JdbcConfigPushCommand;
import com.pamirs.attach.plugin.shadow.preparation.command.JdbcPrecheckCommand;
import com.pamirs.attach.plugin.shadow.preparation.command.processor.JdbcConfigPushCommandProcessor;
import com.pamirs.attach.plugin.shadow.preparation.command.processor.JdbcPrecheckCommandProcessor;
import com.pamirs.attach.plugin.shadow.preparation.command.processor.WhiteListPushCommandProcessor;
import com.pamirs.attach.plugin.shadow.preparation.constants.ShadowPreparationConstants;
import com.pamirs.attach.plugin.shadow.preparation.entity.jdbc.DataSourceConfig;
import com.pamirs.attach.plugin.shadow.preparation.entity.jdbc.DataSourceEntity;
import com.pamirs.attach.plugin.shadow.preparation.jdbc.JdbcDataSourceFetcher;
import com.pamirs.pradar.AppNameUtils;
import com.pamirs.pradar.Pradar;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.executors.ExecutorServiceFactory;
import io.shulie.agent.management.client.AgentManagementClient;
import io.shulie.agent.management.client.constant.AgentSpecification;
import io.shulie.agent.management.client.constant.CommandType;
import io.shulie.agent.management.client.listener.CommandListener;
import io.shulie.agent.management.client.listener.ConfigListener;
import io.shulie.agent.management.client.model.*;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "shadow-preparation", version = "1.0.0", author = "jiangjibo@shulie.io", description = "影子资源准备工作，包括创建，校验")
public class ShadowPreparationModule extends ModuleLifecycleAdapter implements ExtensionModule {

    private final static Logger LOGGER = LoggerFactory.getLogger(ShadowPreparationModule.class.getName());

    @Override
    public boolean onActive() throws Throwable {
        registerAgentManagerListener();
        // 每隔5分钟刷新一次数据源
        ExecutorServiceFactory.getFactory().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                JdbcDataSourceFetcher.refreshDataSources();
            }
        }, 1, 3, TimeUnit.MINUTES);

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
        JdbcPrecheckCommandProcessor.processPreCheckCommand(cmd, null);
    }

    private void handlerConfigPushCommand() {
        JdbcConfigPushCommand command = new JdbcConfigPushCommand();
        DataSourceConfig config = new DataSourceConfig();
        config.setUrl("jdbc:mysql://192.168.1.46:3306/atester?useUnicode=true");
        config.setUsername("admin");
        config.setShadowUrl("jdbc:mysql://192.168.1.46:3306/pt_atester_1?useUnicode=true");
        config.setShadowUsername("admin");
        config.setShadowPassword("athene.admin");
        config.setDsType(0);
        command.setData(Arrays.asList(config));

        Config cmd = new Config();
        cmd.setType("database");
        cmd.setVersion("1");
        cmd.setParam(JSON.toJSONString(command));
        JdbcConfigPushCommandProcessor.processConfigPushCommand(cmd, null);
    }

    private void handlerWhiteListPushCommand(){
        String json = "[\n" +
                "    {\n" +
                "        \"INTERFACE_NAME\":\"/demo/remote/getUser\",\n" +
                "        \"appNames\":null,\n" +
                "        \"isGlobal\":null,\n" +
                "        \"TYPE\":\"http\",\n" +
                "        \"checkType\":\"mock\",\n" +
                "        \"content\":\"import com.pamirs.agent.httpclient4.demo.model.User;\\n User user = new User();\\n        user.setId(\\\"23\\\");\\n        user.setName(\\\"OK00\\\");\\n        return user;\",\n" +
                "        \"forwardUrl\":\"import com.pamirs.agent.httpclient4.demo.model.User;\\n User user = new User();\\n        user.setId(\\\"23\\\");\\n        user.setName(\\\"OK00\\\");\\n        return user;\"\n" +
                "    },\n" +
                "    {\n" +
                "        \"INTERFACE_NAME\":\"/demo/httpclient/post\",\n" +
                "        \"appNames\":null,\n" +
                "        \"isGlobal\":null,\n" +
                "        \"TYPE\":\"http\",\n" +
                "        \"checkType\":\"white\",\n" +
                "        \"content\":null,\n" +
                "        \"forwardUrl\":null\n" +
                "    }\n" +
                "]";
        Config cmd = new Config();
        cmd.setType("whitelist");
        cmd.setVersion("1");
        cmd.setParam(json);
        WhiteListPushCommandProcessor.handlerConfigPushCommand(cmd, null);
    }

    private void registerAgentManagerListener() {
        ConfigProperties properties = new ConfigProperties();
        properties.setAppName(AppNameUtils.appName());
        properties.setTenantCode(simulatorConfig.getProperty(ShadowPreparationConstants.AGENT_MANAGER_TENANT_CODE_KEY));
//        properties.setUserId(Pradar.getPradarUserId());
        properties.setUserId("164");
        properties.setEnvCode(Pradar.getEnvCode());
        properties.setAgentSpecification(AgentSpecification.SIMULATOR_AGENT);
        properties.setVersion(simulatorConfig.getAgentVersion());
        properties.setAgentId(simulatorConfig.getAgentId());
        AgentManagementClient client = new AgentManagementClient(simulatorConfig.getProperty(ShadowPreparationConstants.AGENT_MANAGER_CLIENT_URL_KEY), properties);

        client.register("pressure_database", new ConfigListener() {
            @Override
            public void receive(Config config, Consumer<ConfigAck> consumer) {
                JdbcConfigPushCommandProcessor.processConfigPushCommand(config, consumer);
            }
        });

        client.register("pressure_whitelist", new ConfigListener() {
            @Override
            public void receive(Config config, Consumer<ConfigAck> consumer) {
                WhiteListPushCommandProcessor.handlerConfigPushCommand(config, consumer);
            }
        });

        client.register(CommandType.DATABASE, new CommandListener() {
            @Override
            public void receive(Command command, Consumer<CommandAck> consumer) {
                JdbcPrecheckCommandProcessor.processPreCheckCommand(command, consumer);
            }
        });
    }

}
