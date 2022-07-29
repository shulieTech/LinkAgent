package io.shulie.instrument.module.spring.kafka;

import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.listener.Listeners;
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
@ModuleInfo(id = "spring-kafka", version = "1.0.0", author = "likan@shulie.io", description = "spring-kafka 插件")
public class SpringKafkaPlugin extends ModuleLifecycleAdapter implements ExtensionModule {
    private static final Logger logger = LoggerFactory.getLogger(SpringKafkaPlugin.class);

    @Override
    public boolean onActive() throws Throwable {
        ConsumerRegister consumerRegister = new ConsumerRegister().consumerExecute(new SpringKafkaConsumerExecute());
        ConsumerManager.register(consumerRegister, "org.springframework.kafka.listener.KafkaMessageListenerContainer#doStart");

//        enhanceTemplate.enhance(this, "org.springframework.kafka.listener.KafkaMessageListenerContainer", new EnhanceCallback() {
//            @Override
//            public void doEnhance(InstrumentClass target) {
//                target.getDeclaredMethods("doStart").addInterceptor(Listeners.of());
//                target.getDeclaredMethods("doStart").addInterceptor(Listeners.of());
//            }
//        });
        return true;
    }
}
