package com.pamirs.attach.plugin.jms;

import com.pamirs.attach.plugin.jms.interceptor.*;
import com.pamirs.pradar.interceptor.Interceptors;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import com.shulie.instrument.simulator.api.scope.ExecutionPolicy;
import org.kohsuke.MetaInfServices;

@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = JmsConstants.PLUGIN_NAME, version = "1.0.0", author = "liqiyu@shulie.io", description = "jms")
public class JmsPlugin extends ModuleLifecycleAdapter implements ExtensionModule {


    @Override
    public boolean onActive() throws Throwable {
        // 收集到context
        enhanceTemplate.enhanceWithInterface(this, "javax.naming.Context", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod instrumentMethod = target.getDeclaredMethod("lookup", "java.lang.String");
                instrumentMethod.addInterceptor(
                    Listeners.of(LookupInterceptor.class, "javax.naming.Context.lookup", ExecutionPolicy.BOUNDARY,
                        Interceptors.SCOPE_CALLBACK));
            }
        });

        //// 收集压测的connect
        //enhanceTemplate.enhanceWithInterface(this, "javax.jms.QueueConnectionFactory", new EnhanceCallback() {
        //    @Override
        //    public void doEnhance(InstrumentClass target) {
        //        final InstrumentMethod instrumentMethod = target.getDeclaredMethods("createQueueConnection");
        //        instrumentMethod.addInterceptor(Listeners.of(QueueConnectionFactoryInterceptor.class,
        //            "javax.jms.QueueConnectionFactory.createQueueConnection", ExecutionPolicy.BOUNDARY,
        //            Interceptors.SCOPE_CALLBACK));
        //
        //    }
        //});
        //enhanceTemplate.enhanceWithInterface(this, "javax.jms.ConnectionFactory", new EnhanceCallback() {
        //    @Override
        //    public void doEnhance(InstrumentClass target) {
        //        final InstrumentMethod instrumentMethod = target.getDeclaredMethods("createConnection");
        //        instrumentMethod.addInterceptor(Listeners.of(QueueConnectionFactoryInterceptor.class,
        //            "javax.jms.ConnectionFactory.createConnection", ExecutionPolicy.BOUNDARY,
        //            Interceptors.SCOPE_CALLBACK));
        //
        //    }
        //});

        // 如果没有接收到业务消息则接收压测消息
        enhanceTemplate.enhance(this, "weblogic.jms.client.JMSConsumer", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                final InstrumentMethod instrumentMethod = target.getDeclaredMethod("receive");
                instrumentMethod.addInterceptor(
                    Listeners.of(JmsReceiveInterceptor.class, "javax.jms.QueueReceiver.receive",
                        ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
                //instrumentMethod.addInterceptor(
                //    Listeners.of(QueueReceiverTraceInterceptor.class, "javax.jms.QueueReceiver.receive_trace",
                //        ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
            }
        });

        // 发送trace
        enhanceTemplate.enhanceWithInterface(this, "javax.jms.QueueSender", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                final InstrumentMethod instrumentMethod = target.getDeclaredMethod("send", "javax.jms.Message");
                instrumentMethod.addInterceptor(
                    Listeners.of(SendTraceInterceptor.class, "javax.jms.QueueSender.send", ExecutionPolicy.BOUNDARY,
                        Interceptors.SCOPE_CALLBACK));
            }
        });

        enhanceTemplate.enhance(this, "weblogic.jms.client.WLSessionImpl", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                final InstrumentMethod createProducerInstrumentMethod = target.getDeclaredMethod("createProducer",
                    "javax.jms.Destination");
                createProducerInstrumentMethod.addInterceptor(
                    Listeners.of(QueueSessionInterceptor.class, "javax.jms.Session.createSender",
                        ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
                final InstrumentMethod createReceiverInstrumentMethod = target.getDeclaredMethod("createSender",
                    "javax.jms.Queue");
                createReceiverInstrumentMethod.addInterceptor(
                    Listeners.of(QueueSessionInterceptor.class, "javax.jms.Session.createSender",
                        ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
            }
        });

        enhanceTemplate.enhance(this, "com.chinalife.esp.scheduler.worker.TaskWorker", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                final InstrumentMethod constructor = target.getDeclaredMethod("run");
                constructor.addInterceptor(
                    Listeners.of(TaskWorkerRunInterceptor.class, "com.chinalife.esp.scheduler.worker.TaskWorker.run",
                        ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
            }
        });

        return true;
    }
}
