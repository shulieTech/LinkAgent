package io.shulie.instrument.module.messaging;

import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;
import org.kohsuke.MetaInfServices;

/**
 * @author Licey
 * @date 2022/7/28
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "messaging-common", version = "1.0.0", author = "likan@shulie.io", description = "MQ 基础插件")
public class MessagingCommonPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() throws Throwable {
        ConsumerConfig consumerConfig = new ConsumerConfig(){
            @Override
            public String keyOfConfig() {
                return null;
            }

            @Override
            public String keyOfServer() {
                return null;
            }
        };
        consumerConfig.key();
        return true;
    }
}
