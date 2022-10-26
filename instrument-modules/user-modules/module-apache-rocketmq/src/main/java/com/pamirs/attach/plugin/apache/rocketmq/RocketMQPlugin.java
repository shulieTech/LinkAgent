/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.apache.rocketmq;

import com.pamirs.attach.plugin.apache.rocketmq.common.ConsumerRegistry;
import com.pamirs.attach.plugin.apache.rocketmq.interceptor.*;
import com.pamirs.attach.plugin.apache.rocketmq.listener.RocketMQShadowPreCheckEventListener;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.SyncObjectService;
import com.pamirs.pradar.bean.SyncObject;
import com.pamirs.pradar.bean.SyncObjectData;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author vincent
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = RocketmqConstants.MODULE_NAME, version = "1.0.0", author = "xiaobin@shulie.io")
public class RocketMQPlugin extends ModuleLifecycleAdapter implements ExtensionModule {
    private static final Logger logger = LoggerFactory.getLogger(RocketMQPlugin.class);

    @Override
    public boolean onActive() throws Throwable {
        addListener();
        return addHookRegisterInterceptor();
    }

    private boolean addHookRegisterInterceptor() {

        this.enhanceTemplate.enhance(this,
                "org.apache.rocketmq.client.consumer.DefaultMQPushConsumer", new EnhanceCallback() {
                    @Override
                    public void doEnhance(InstrumentClass target) {
                        final InstrumentMethod createTopicMethod = target.getDeclaredMethods("createTopic");
                        createTopicMethod.addInterceptor(Listeners.of(DefaultPushConsumerCreateTopicInterceptor.class));

                        final InstrumentMethod earliestMsgStoreTimeMethod = target.getDeclaredMethods("earliestMsgStore");
                        earliestMsgStoreTimeMethod.addInterceptor(Listeners.of(DefaultPushConsumerEarliestMsgStoreTimeInterceptor.class));

                        final InstrumentMethod searchOffsetMethod = target.getDeclaredMethods("searchOffset");
                        searchOffsetMethod.addInterceptor(Listeners.of(DefaultPushConsumerSearchOffsetInterceptor.class));

                        final InstrumentMethod maxOffsetMethod = target.getDeclaredMethods("maxOffset");
                        maxOffsetMethod.addInterceptor(Listeners.of(DefaultPushConsumerMaxOffsetInterceptor.class));

                        final InstrumentMethod minOffsetMethod = target.getDeclaredMethods("minOffset");
                        minOffsetMethod.addInterceptor(Listeners.of(DefaultPushConsumerMinOffsetInterceptor.class));

                        final InstrumentMethod viewMessageMethod = target.getDeclaredMethods("viewMessage");
                        viewMessageMethod.addInterceptor(Listeners.of(DefaultPushConsumerViewMessageInterceptor.class));

                        final InstrumentMethod queryMessageMethod = target.getDeclaredMethods("queryMessage");
                        queryMessageMethod.addInterceptor(Listeners.of(DefaultPushConsumerQueryMessageInterceptor.class));

                        final InstrumentMethod fetchSubscribeMessageQueuesMethod = target.getDeclaredMethods("fetchSubscribeMessageQueues");
                        fetchSubscribeMessageQueuesMethod.addInterceptor(Listeners.of(DefaultPushConsumerFetchSubscribeMessageQueuesInterceptor.class));

                        final InstrumentMethod shutdownMethod = target.getDeclaredMethod("shutdown");
                        shutdownMethod.addInterceptor(Listeners.of(DefaultPushConsumerShutdownInterceptor.class));
                    }
                });
//
//        this.enhanceTemplate.enhance(this, "org.apache.rocketmq.client.impl.consumer.DefaultMQPushConsumerImpl", new EnhanceCallback() {
//            @Override
//            public void doEnhance(InstrumentClass target) {
//                final InstrumentMethod hasHookMethod = target.getDeclaredMethod("hasHook");
//                hasHookMethod.addInterceptor(Listeners.of(DefaultMQPushConsumerImplHasHookListener.class));
//            }
//        });

        this.enhanceTemplate.enhance(this,
                "org.apache.rocketmq.client.impl.consumer.DefaultMQPullConsumerImpl", new EnhanceCallback() {
                    @Override
                    public void doEnhance(InstrumentClass target) {

                        final InstrumentMethod pullSyncImplMethod = target.getDeclaredMethod("pullSyncImpl", "org.apache.rocketmq.common.message.MessageQueue", "java.lang.String", "long", "int", "boolean", "long");
                        pullSyncImplMethod
                                .addInterceptor(Listeners.of(PullConsumerInterceptor.class));
                    }
                });

//        this.enhanceTemplate.enhance(this, "org.apache.rocketmq.client.producer.DefaultMQProducer", new EnhanceCallback() {
//            @Override
//            public void doEnhance(InstrumentClass target) {
//
//                //压测代码
//                InstrumentMethod sendMethod = target.getDeclaredMethods("send*");
//                sendMethod.addInterceptor(Listeners.of(MQProducerSendInterceptor.class));
//
//            }
//        });

        this.enhanceTemplate.enhance(this, "org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                //压测代码
                InstrumentMethod method = target.getDeclaredMethods("sendKernelImpl");
                method.addInterceptor(Listeners.of(MQProducerSendInterceptor.class));

                InstrumentMethod sendMessageInTransactionMethod = target.getDeclaredMethods("sendMessageInTransaction");
                sendMessageInTransactionMethod.addInterceptor(Listeners.of(MQProducerSendInterceptor.class));

                //压测代码 end
            }
        });


        this.enhanceTemplate.enhance(this, "org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod enhanceMethod = target.getDeclaredMethods("checkTransactionState*");
                enhanceMethod.addInterceptor(Listeners.of(TransactionCheckInterceptor.class));
            }
        });

        this.enhanceTemplate.enhance(this,
                "org.apache.rocketmq.client.impl.consumer.ConsumeMessageConcurrentlyService$ConsumeRequest",
                new EnhanceCallback() {
                    @Override
                    public void doEnhance(InstrumentClass target) {
                        InstrumentMethod enhanceMethod = target.getDeclaredMethods("run");
                        enhanceMethod.addInterceptor(Listeners.of(ConcurrentlyTraceInterceptor.class));
                    }
                });

        //--for orderly
        this.enhanceTemplate.enhance(this,
                "org.apache.rocketmq.client.impl.consumer.ConsumeMessageOrderlyService$ConsumeRequest", new EnhanceCallback() {
                    @Override
                    public void doEnhance(InstrumentClass target) {
                        InstrumentMethod enhanceMethod = target.getDeclaredMethods("run");
                        enhanceMethod.addInterceptor(Listeners.of(OrderlyTraceContextInterceptor.class));
                    }
                });


        //--for orderly
        this.enhanceTemplate.enhanceWithInterface(this,
                "org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly", new EnhanceCallback() {
                    @Override
                    public void doEnhance(InstrumentClass target) {
                        InstrumentMethod enhanceMethod = target.getDeclaredMethods("consumeMessage");
                        enhanceMethod.addInterceptor(Listeners.of(OrderlyTraceBeforeInterceptor.class));
                    }
                });

        this.enhanceTemplate.enhance(this,
                "org.apache.rocketmq.client.impl.consumer.ProcessQueue", new EnhanceCallback() {
                    @Override
                    public void doEnhance(InstrumentClass target) {
                        InstrumentMethod enhanceMethod = target.getDeclaredMethods("takeMessags");
                        enhanceMethod.addInterceptor(Listeners.of(OrderlyTraceBeforeInterceptor.class));
                    }
                });

        this.enhanceTemplate.enhance(this,
                "org.apache.rocketmq.client.impl.consumer.ConsumeMessageOrderlyService", new EnhanceCallback() {
                    @Override
                    public void doEnhance(InstrumentClass target) {
                        InstrumentMethod enhanceMethod = target.getDeclaredMethods("processConsumeResult");
                        enhanceMethod.addInterceptor(Listeners.of(OrderlyTraceAfterInterceptor.class));
                    }
                });

        //--for orderly
        delayToInitShadowConsumer();
        return true;
    }

    private void delayToInitShadowConsumer() {
        final SyncObject syncObject = SyncObjectService.getSyncObject("org.apache.rocketmq.client.consumer.DefaultMQPushConsumer#start");
        if (syncObject == null) {
            return;
        }
        Thread thread = new Thread(new Runnable() {
            boolean isInit;
            int initTimes;

            @Override
            public void run() {
                while (true) {
                    try {
                        if (isInit) {
                            Thread.sleep(300000);
                        } else {
                            Thread.sleep(5000);
                        }
                    } catch (InterruptedException e) {
                        logger.warn("delayToInitShadowConsumer tnterrupted", e);
                    }
                    if (!PradarSwitcher.isClusterTestReady() || GlobalConfig.getInstance().getMqWhiteList() == null || GlobalConfig.getInstance().getMqWhiteList().isEmpty()) {
                        if (!isInit && initTimes++ > 20) {
                            isInit = true;
                        }
                        continue;
                    }
                    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                    try {
                        logger.info("delayToInitShadowConsumer start ############");
                        if (syncObject == null) {
                            return;
                        }
                        Thread.currentThread().setContextClassLoader(syncObject.getClass().getClassLoader());
                        for (SyncObjectData data : syncObject.getDatas()) {
                            Object target = data.getTarget();
                            if (target instanceof DefaultMQPushConsumer) {
                                logger.info("delayToInitShadowConsumer to init ######### {} ", target);
                                ConsumerRegistry.registerConsumer((DefaultMQPushConsumer) target);
                            }
                        }
                        logger.info("delayToInitShadowConsumer end ############");
                    } catch (Throwable e) {
                        logger.error("delayToInitShadowConsumer init fail!", e);
                    } finally {
                        Thread.currentThread().setContextClassLoader(classLoader);
                        isInit = true;
                    }
                }
            }
        });
        thread.setName("apache-rocketmq-init-shadownConsumer-thread");
        thread.start();
    }

    private void addListener() {
        EventRouter.router()
                .addListener(new RocketMQShadowPreCheckEventListener());
    }

}
