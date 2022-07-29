package io.shulie.instrument.module.messaging.consumer;

import com.alibaba.fastjson.JSON;
import com.pamirs.pradar.SyncObjectService;
import com.pamirs.pradar.bean.SyncObject;
import com.pamirs.pradar.bean.SyncObjectData;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import io.shulie.instrument.module.messaging.annoation.NotNull;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerRegister;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerModule;
import io.shulie.instrument.module.messaging.consumer.module.ShadowConsumer;
import io.shulie.instrument.module.messaging.exception.MessagingRuntimeException;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Licey
 * @date 2022/7/26
 */
public class ConsumerManager {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ConsumerManager.class);
    private static final Set<Field> notNullList = new HashSet<Field>();

    private final static List<ConsumerModule> registerList = new Vector<ConsumerModule>();

    private static ScheduledExecutorService taskService;
    private static final SyncObject EMPTY_SYNC_OBJECT = new SyncObject();
    private static final int initialDelay = 30;
    private static final int delay=120;

    static List<ShadowConsumer> runningShadowConsumer(){
        List<ShadowConsumer> list = new ArrayList<ShadowConsumer>();
        for (ConsumerModule consumerModule : registerList) {
            for (List<ShadowConsumer> shadowConsumers : consumerModule.getShadowConsumerMap().values()) {
                for (ShadowConsumer shadowConsumer : shadowConsumers) {
                    if (shadowConsumer.isStarted()) {
                        list.add(shadowConsumer);
                    }
                }
            }
        }
        return list;
    }

    public static void register(ConsumerRegister register, String... syncClassMethods) {
        checkRegister(register);

        ConsumerModule consumerModule = new ConsumerModule(register);
        if (syncClassMethods != null) {
            for (String syncClassMethod : syncClassMethods) {
                consumerModule.getSyncObjectMap().put(syncClassMethod, EMPTY_SYNC_OBJECT);
            }
        }
        registerList.add(consumerModule);
        runTask(consumerModule);

        startTask();
    }

    private static void checkRegister(ConsumerRegister register) {
        if (register == null) {
            throw new MessagingRuntimeException("register 不能为空");
        }
        if (notNullList.isEmpty()) {
            prepareField();
        }
        for (Field field : notNullList) {
            try {
                if (field.get(register) == null) {
                    throw new MessagingRuntimeException("ConsumerRegister 注册内容 " + field.getName() + " 不能为空");
                }
            } catch (IllegalAccessException e) {
                throw new MessagingRuntimeException("read field fail!", e);
            }
        }
    }

    private static synchronized void prepareField() {
        if (notNullList.isEmpty()) {
            for (Field field : ConsumerRegister.class.getDeclaredFields()) {
                if (field.getAnnotation(NotNull.class) != null) {
                    field.setAccessible(true);
                    notNullList.add(field);
                }
            }
        }
    }

    private static void doConsumerRegister(ConsumerModule consumerModule) {
        for (Map.Entry<String, SyncObject> entry : consumerModule.getSyncObjectMap().entrySet()) {
            for (SyncObjectData objectData : entry.getValue().getDatas()) {
                if (!consumerModule.getShadowConsumerMap().containsKey(objectData)) {
                    try {
                        List<ConsumerConfig> configList = consumerModule.getConsumerRegister().getConsumerExecute().prepareConfig(objectData);
                        if (configList != null && !configList.isEmpty()) {
                            logger.info("[messaging-common]success prepareConfig from: {}, key:{}", entry.getKey(), configList.stream().map(ConsumerConfig::keyOfConfig).collect(Collectors.joining(",")));
                            consumerModule.getShadowConsumerMap().put(objectData, configList.stream().map(ShadowConsumer::new).collect(Collectors.toList()));
                        }
                    } catch (Exception e) {
                        logger.error("prepare Config fail:{}", JSON.toJSONString(consumerModule.getConsumerRegister()), e);
                    }
                }
            }
        }
    }

    private static void runTask(){
        for (ConsumerModule consumerModule : registerList) {
            runTask(consumerModule);
        }
    }

    private static  void runTask(ConsumerModule consumerModule){
        try {
            synchronized (consumerModule) {
                refreshSyncObj(consumerModule);
                tryToStartConsumer(consumerModule);
            }
        } catch (Throwable e) {
            logger.warn("start task fail,will try next time: {}", JSON.toJSONString(consumerModule), e);
        }
    }

    private static void tryToStartConsumer(ConsumerModule consumerModule) {
        Set<String> mqWhiteList = GlobalConfig.getInstance().getMqWhiteList();
        for (Map.Entry<SyncObjectData, List<ShadowConsumer>> entry : consumerModule.getShadowConsumerMap().entrySet()) {
            for (ShadowConsumer shadowConsumer : entry.getValue()) {
                try {
                    if (!shadowConsumer.isStarted()) {
                        //todo@langyi 支持集群模式
                        String key = shadowConsumer.getConsumerConfig().keyOfConfig();
                        if (key == null) {
                            continue;
                        }
                        if (mqWhiteList.contains(key)) {
                            fetchShadowServer(consumerModule, shadowConsumer, key);
                            logger.info("[messaging-common]success fetch shadowServer with key:{}", key);
                            doStartShadowServer(shadowConsumer);
                            logger.info("[messaging-common]success start shadowServer with key:{}", key);
                        }else{
                            logger.info("[messaging-common] key {} is not allow to consumer message", key);
                        }
                    }
                } catch (Throwable e) {
                    logger.error("[messaging-common]try to start consumer server fail with key:{}", shadowConsumer.getConsumerConfig().keyOfConfig(), e);
                }

            }
        }
    }

    private static void fetchShadowServer(ConsumerModule consumerModule, ShadowConsumer shadowConsumer,String config) {
        try {
            //todo@langyi 传入影子消费者的配置
            ShadowServer shadowServer = consumerModule.getConsumerRegister().getConsumerExecute().fetchShadowServer(shadowConsumer.getConsumerConfig(), config);
            shadowConsumer.setShadowServer(shadowServer);
        } catch (Exception e) {
            logger.error("init shadow server fail:{}", JSON.toJSONString(shadowConsumer.getConsumerConfig()), e);
        }
    }

    private static void doStartShadowServer(ShadowConsumer shadowConsumer){
        try {
            if (shadowConsumer.getShadowServer() != null) {
                shadowConsumer.getShadowServer().start();
                shadowConsumer.setStarted(true);
            }else{
                shadowConsumer.setStarted(false);
            }
        } catch (Throwable e) {
            logger.error("start shadow consumer fail:{}", JSON.toJSON(shadowConsumer.getConsumerConfig()), e);
        }
    }

    private static void refreshSyncObj(ConsumerModule consumerModule) {
        boolean isRefreshed = false;
        for (Map.Entry<String, SyncObject> entry : consumerModule.getSyncObjectMap().entrySet()) {
            //当时获取不到的，重新再获取一次
            if (entry.getValue() == EMPTY_SYNC_OBJECT) {
                String key = entry.getKey();
                logger.info("[messaging-common]success fetch sync data from {}", key);
                entry.setValue(SyncObjectService.getSyncObject(key));
                isRefreshed = true;
            }
        }
        if (isRefreshed) {
            doConsumerRegister(consumerModule);
        }
    }

    private static void startTask(){
        if (taskService == null) {
            initTask();
        }
    }

    private synchronized static void initTask(){
        if (taskService == null) {
            taskService = Executors.newScheduledThreadPool(1);
            taskService.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    try {
                        runTask();
                    } catch (Throwable e) {
                        logger.error("run messaging task fail!", e);
                    }
                }
            }, initialDelay, delay, TimeUnit.SECONDS);
        }
    }

}
