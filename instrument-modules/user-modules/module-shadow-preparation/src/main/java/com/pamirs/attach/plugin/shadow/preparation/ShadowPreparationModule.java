package com.pamirs.attach.plugin.shadow.preparation;

import com.pamirs.attach.plugin.shadow.preparation.constants.AgentType;
import com.pamirs.pradar.AppNameUtils;
import com.pamirs.pradar.Pradar;
import com.shulie.instrument.simulator.agent.api.utils.HeartCommandUtils;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import io.shulie.agent.management.client.AgentManagementClient;
import io.shulie.agent.management.client.listener.EventListener;
import io.shulie.agent.management.client.model.ConfigProperties;
import io.shulie.agent.management.client.model.Event;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "shadow-preparation", version = "1.0.0", author = "jiangjibo@shulie.io", description = "影子资源准备工作，包括创建，校验")
public class ShadowPreparationModule extends ModuleLifecycleAdapter implements ExtensionModule {

    private final static Logger LOGGER = LoggerFactory.getLogger(ShadowPreparationModule.class.getName());

    @Override
    public boolean onActive() throws Throwable {
        ConfigProperties properties = new ConfigProperties();
        properties.setAppName(AppNameUtils.appName());
        properties.setTenantCode(Pradar.getPradarTenantKey());
        properties.setUserId(Pradar.getPradarUserId());
        properties.setEnvCode(Pradar.getEnvCode());
        properties.setSpecification(AgentType.simulator_agent.getType());
        properties.setVersion(HeartCommandUtils.SIMULATOR_VERSION);
        properties.setAgentId(System.getProperty("simulator.agent.id"));
        AgentManagementClient client = new AgentManagementClient(null, properties);

        client.register(properties, new EventListener() {

            @Override
            public void onEvent(Event event) {
                Integer eventType = event.getEventType();
                switch (eventType) {
                    case 1:

                        break;
                    case 2:

                        break;
                    default:
                        LOGGER.error("[shadow-preparation] unknown event type {}", eventType);
                }
            }

            @Override
            public void onException(Exception e) {

            }
        });
        AgentManagerExecutor.setAgentManagementClient(client);
        return true;
    }
}
