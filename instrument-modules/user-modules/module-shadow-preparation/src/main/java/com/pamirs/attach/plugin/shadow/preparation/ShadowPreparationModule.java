package com.pamirs.attach.plugin.shadow.preparation;

import com.pamirs.attach.plugin.shadow.preparation.command.processor.JdbcConfigPushCommandProcessor;
import com.pamirs.attach.plugin.shadow.preparation.command.processor.JdbcPreCheckCommandProcessor;
import com.pamirs.attach.plugin.shadow.preparation.command.processor.WhiteListPushCommandProcessor;
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
        ExecutorServiceFactory.getFactory().schedule(new Runnable() {
            @Override
            public void run() {
                registerAgentManagerListener();
            }
        }, 1, TimeUnit.MINUTES);
        return true;
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

        client.register("pressure_database", new ConfigListener() {
            @Override
            public void receive(Config config, Consumer<ConfigAck> consumer) {
                JdbcConfigPushCommandProcessor.processConfigPushCommand(config, consumer);
            }
        });

        client.register("pressure_whitelist", new ConfigListener() {
            @Override
            public void receive(Config config, Consumer<ConfigAck> consumer) {
                WhiteListPushCommandProcessor.processConfigPushCommand(config, consumer);
            }
        });

        client.register(CommandType.DATABASE, new CommandListener() {
            @Override
            public void receive(Command command, Consumer<CommandAck> consumer) {
                JdbcPreCheckCommandProcessor.processPreCheckCommand(command, consumer);
            }
        });
    }

}
