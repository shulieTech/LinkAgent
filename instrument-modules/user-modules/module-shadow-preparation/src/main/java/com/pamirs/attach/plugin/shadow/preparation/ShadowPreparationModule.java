package com.pamirs.attach.plugin.shadow.preparation;

import com.alibaba.fastjson.JSON;
import com.pamirs.attach.plugin.shadow.preparation.command.EsConfigPushCommand;
import com.pamirs.attach.plugin.shadow.preparation.command.EsPreCheckCommand;
import com.pamirs.attach.plugin.shadow.preparation.command.JdbcConfigPushCommand;
import com.pamirs.attach.plugin.shadow.preparation.command.JdbcPreCheckCommand;
import com.pamirs.attach.plugin.shadow.preparation.command.processor.*;
import com.pamirs.attach.plugin.shadow.preparation.es.EsConfigEntity;
import com.pamirs.attach.plugin.shadow.preparation.jdbc.entity.DataSourceConfig;
import com.pamirs.attach.plugin.shadow.preparation.jdbc.entity.DataSourceEntity;
import com.pamirs.pradar.AppNameUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.pressurement.base.util.PropertyUtil;
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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "shadow-preparation", version = "1.0.0", author = "jiangjibo@shulie.io", description = "影子资源准备工作，包括创建，校验，生效")
public class ShadowPreparationModule extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() throws Throwable {
        if (!PropertyUtil.isShadowPreparationEnabled()) {
            return true;
        }
       /*ExecutorServiceFactory.getFactory().schedule(new Runnable() {
            @Override
            public void run() {
                registerAgentManagerListener();
            }
        }, 1, TimeUnit.MINUTES);*/


        ExecutorServiceFactory.getFactory().schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    handlerPreCheckCommand();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, 30, TimeUnit.SECONDS);

        return true;
    }

    private void handlerPreCheckCommand() throws InterruptedException {

        EsConfigEntity entity = new EsConfigEntity();
        entity.setBusinessNodes("192.168.1.210:9200,192.168.1.193:9200,192.168.1.194:9200");
        entity.setPerformanceTestNodes("192.168.1.210:9200,192.168.1.193:9200,192.168.1.194:9200");
        entity.setShadowType(1);
        entity.setIndices(Arrays.asList("es7-test-index"));

        EsConfigPushCommand pushCommand = new EsConfigPushCommand();
        pushCommand.setData(Arrays.asList(entity));

        Config config = new Config();
        config.setParam(JSON.toJSONString(pushCommand));
        config.setVersion("1");

        EsConfigPushCommandProcessor.processConfigPushCommand(config, new Consumer<ConfigAck>() {
            @Override
            public void accept(ConfigAck configAck) {
                System.out.println("success");
            }
        });

        Thread.sleep(1 * 1000);

        EsConfigEntity et = new EsConfigEntity();
        et.setBusinessNodes("192.168.1.210:9200,192.168.1.193:9200,192.168.1.194:9200");
        et.setPerformanceTestNodes("192.168.1.210:9200,192.168.1.193:9200,192.168.1.194:9200");
        et.setShadowType(3);
        pushCommand.setData(Arrays.asList(et));
        config.setParam(JSON.toJSONString(pushCommand));
        EsConfigPushCommandProcessor.processConfigPushCommand(config, new Consumer<ConfigAck>() {
            @Override
            public void accept(ConfigAck configAck) {
                System.out.println("success");
            }
        });

    }

    private void registerAgentManagerListener() {
        ConfigProperties properties = new ConfigProperties();
        properties.setAppName(AppNameUtils.appName());
        properties.setTenantCode(PropertyUtil.getAgentManagerTenantCode());
        properties.setUserId(Pradar.getPradarUserId());
        properties.setEnvCode(Pradar.getEnvCode());
        properties.setAgentSpecification(AgentSpecification.SIMULATOR_AGENT);
        properties.setVersion(simulatorConfig.getAgentVersion());
        properties.setAgentId(simulatorConfig.getAgentId());
        AgentManagementClient client = new AgentManagementClient(PropertyUtil.getAgentManagerUrl(), properties);

        // 数据源
        client.register("pressure_database", new ConfigListener() {
            @Override
            public void receive(Config config, Consumer<ConfigAck> consumer) {
                JdbcConfigPushCommandProcessor.processConfigPushCommand(config, consumer);
            }
        });

        client.register("pressure_database", new CommandListener() {
            @Override
            public void receive(Command command, Consumer<CommandAck> consumer) {
                JdbcPreCheckCommandProcessor.processPreCheckCommand(command, consumer);
            }
        });

        // 白名单
        client.register("pressure_whitelist", new ConfigListener() {
            @Override
            public void receive(Config config, Consumer<ConfigAck> consumer) {
                WhiteListPushCommandProcessor.processConfigPushCommand(config, consumer);
            }
        });

        // mq
        client.register("pressure_mq", new CommandListener() {
            @Override
            public void receive(Command command, Consumer<CommandAck> consumer) {
                MqPreCheckCommandProcessor.processPreCheckCommand(command, consumer);
            }
        });

        client.register("pressure_mq", new ConfigListener() {
            @Override
            public void receive(Config config, Consumer<ConfigAck> consumer) {
                MqConfigPushCommandProcessor.processConfigPushCommand(config, consumer);
            }
        });

        //es
        client.register("pressure_es", new CommandListener() {
            @Override
            public void receive(Command command, Consumer<CommandAck> consumer) {
                EsPreCheckCommandProcessor.processPreCheckCommand(command, consumer);
            }
        });
        client.register("pressure_es", new ConfigListener() {
            @Override
            public void receive(Config config, Consumer<ConfigAck> consumer) {
                EsConfigPushCommandProcessor.processConfigPushCommand(config, consumer);
            }
        });
    }

}
