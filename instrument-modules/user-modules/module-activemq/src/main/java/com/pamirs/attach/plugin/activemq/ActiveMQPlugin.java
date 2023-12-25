package com.pamirs.attach.plugin.activemq;

import com.pamirs.attach.plugin.activemq.interceptor.ConsumerOnMessageInterceptor;
import com.pamirs.attach.plugin.activemq.interceptor.ProducerSendInterceptor;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author guann1n9
 * @date 2023/12/20 10:20 AM
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = ActiveMQConstants.MODULE_NAME, version = "1.0.0", author = "xiaobin@shulie.io", description = "activemq消息中间件")
public class ActiveMQPlugin extends ModuleLifecycleAdapter implements ExtensionModule {
    private static final Logger logger = LoggerFactory.getLogger(ActiveMQPlugin.class);


    @Override
    public boolean onActive() throws Throwable {

        //消息发送
        this.enhanceTemplate.enhance(this, "org.apache.activemq.ActiveMQSession", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod sendMethod = target.getDeclaredMethods("send");
                sendMethod.addInterceptor(Listeners.of(ProducerSendInterceptor.class));
            }
        });


        //消费者
        this.enhanceTemplate.enhance(this, "org.apache.activemq.ActiveMQMessageConsumer", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod sendMethod = target.getDeclaredMethods("dispatch");
                sendMethod.addInterceptor(Listeners.of(ConsumerOnMessageInterceptor.class));
            }
        });

        return true;

    }



}
