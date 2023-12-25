package com.pamirs.attach.plugin.activemqv2;

import com.pamirs.attach.plugin.activemqv2.consumer.ActiveMQShadowConsumerExecute;
import com.pamirs.attach.plugin.activemqv2.producer.factory.ShadowProducerFactory;
import com.pamirs.attach.plugin.activemqv2.producer.proxy.SendProxy;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import io.shulie.instrument.module.isolation.IsolationManager;
import io.shulie.instrument.module.isolation.common.ResourceInit;
import io.shulie.instrument.module.isolation.enhance.EnhanceClass;
import io.shulie.instrument.module.isolation.proxy.ShadowMethodProxy;
import io.shulie.instrument.module.isolation.register.ShadowProxyConfig;
import io.shulie.instrument.module.isolation.resource.ShadowResourceProxyFactory;
import io.shulie.instrument.module.messaging.consumer.ConsumerManager;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowConsumerExecute;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerRegister;
import org.kohsuke.MetaInfServices;

/**
 * @author guann1n9
 * @date 2023/12/20 10:20 AM
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "activemqv2", version = "1.0.0", author = "xiaobin@shulie.io", description = "activemqv2消息中间件")
public class ActiveMQV2Plugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() throws Throwable {

        ShadowProxyConfig proxyConfig = new ShadowProxyConfig("activemqv2");
        //生产者代理
        proxyConfig
                .addEnhance(new EnhanceClass("org.apache.activemq.ActiveMQMessageProducer")
                        .setFactoryResourceInit(new ResourceInit<ShadowResourceProxyFactory>() {
                            @Override
                            public ShadowResourceProxyFactory init() {
                                return new ShadowProducerFactory();
                            }
                        })
                        .addEnhanceMethods(new ResourceInit<ShadowMethodProxy>() {
                            @Override
                            public ShadowMethodProxy init() {
                                return new SendProxy();
                            }
                        }, "send")
                        .addEnhanceMethods("getStats","getProducerStats","getDestination","checkClosed","getTransformer","onProducerAck","getStartTime","getMessageSequence","setMessageSequence"));

        IsolationManager.register(proxyConfig);


        ConsumerRegister consumerRegister = new ConsumerRegister("activemqv2").consumerExecute(new io.shulie.instrument.module.messaging.common.ResourceInit<ShadowConsumerExecute>() {
            @Override
            public ShadowConsumerExecute init() {
                return new ActiveMQShadowConsumerExecute();
            }
        });
        ConsumerManager.register(consumerRegister, "org.apache.activemq.ActiveMQMessageConsumer");
        return true;
    }



}
