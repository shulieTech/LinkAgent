package io.shulie.instrument.module.spring.kafka;

import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.listener.Listeners;
import io.shulie.instrument.module.isolation.IsolationManager;
import io.shulie.instrument.module.isolation.enhance.EnhanceClass;
import io.shulie.instrument.module.isolation.register.ShadowProxyConfig;
import io.shulie.instrument.module.messaging.consumer.ConsumerManager;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowConsumerExecute;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerRegister;
import io.shulie.instrument.module.spring.kafka.consumer.SpringKafkaConsumerExecute;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Licey
 * @date 2022/7/28
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = SpringKafkaPlugin.moduleName, version = "1.0.0", author = "likan@shulie.io", description = "spring-kafka 插件")
public class SpringKafkaPlugin extends ModuleLifecycleAdapter implements ExtensionModule {
    private static final Logger logger = LoggerFactory.getLogger(SpringKafkaPlugin.class);
    public static final String moduleName = "spring-kafka";

    @Override
    public boolean onActive() throws Throwable {
        //spring-kafka 的发送使用原生kafka 代码
        ConsumerRegister consumerRegister = new ConsumerRegister(moduleName).consumerExecute(SpringKafkaConsumerExecute::new);
        ConsumerManager.register(consumerRegister, "org.springframework.kafka.listener.KafkaMessageListenerContainer#doStart");
        return true;
    }
}
