package io.shulie.instrument.module.messaging.consumer;

import com.alibaba.fastjson.JSON;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.SyncObjectService;
import com.pamirs.pradar.bean.SyncObject;
import com.pamirs.pradar.bean.SyncObjectData;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import io.shulie.instrument.module.messaging.annoation.NotNull;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowConsumerExecute;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;
import io.shulie.instrument.module.messaging.consumer.module.*;
import io.shulie.instrument.module.messaging.exception.MessagingRuntimeException;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    //所有注册上来的消费者拉起信息
    private final static List<ConsumerRegisterModule> registerList = new Vector<ConsumerRegisterModule>();

    //按照业务server分组的影子消费者信息，
    private final static Map<String, ShadowConsumer> shadowConsumerMap = new ConcurrentHashMap<>();

    private static ScheduledExecutorService taskService;
    private static final SyncObject EMPTY_SYNC_OBJECT = new SyncObject();
    private static final int initialDelay = 30;
    private static final int delay = 120;

    static List<ShadowConsumer> runningShadowConsumer() {
        List<ShadowConsumer> list = new ArrayList<ShadowConsumer>();
        for (ShadowConsumer shadowConsumer : shadowConsumerMap.values()) {
            if (shadowConsumer.isStarted()) {
                list.add(shadowConsumer);
            }
        }
        return list;
    }

    public static void register(ConsumerRegister register, String... syncClassMethods) {
        checkRegister(register);

        ConsumerRegisterModule consumerRegisterModule = new ConsumerRegisterModule(register);
        if (syncClassMethods != null) {
            for (String syncClassMethod : syncClassMethods) {
                consumerRegisterModule.getSyncObjectMap().put(syncClassMethod, EMPTY_SYNC_OBJECT);
            }
        }
        registerList.add(consumerRegisterModule);

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

    private static void doConsumerRegister(ConsumerRegisterModule consumerRegisterModule) {
        for (Map.Entry<String, SyncObject> entry : consumerRegisterModule.getSyncObjectMap().entrySet()) {
            for (SyncObjectData objectData : entry.getValue().getDatas()) {
                if (!consumerRegisterModule.getSyncObjectDataMap().containsKey(objectData)) {
                    try {
//                        BizClassLoaderService.setBizClassLoader(objectData.getTarget().getClass().getClassLoader());
                        ShadowConsumerExecute shadowConsumerExecute = prepareShadowConsumerExecute(consumerRegisterModule, objectData);
                        List<ConsumerConfig> configList = shadowConsumerExecute.prepareConfig(objectData);
                        if (configList != null && !configList.isEmpty()) {
                            logger.info("[messaging-common]success prepareConfig from: {}, key:{}", entry.getKey(), configList.stream().map(ConsumerConfig::keyOfConfig).collect(Collectors.joining(",")));
                            for (ConsumerConfig consumerConfig : configList) {
                                if (!isValidConfig(consumerConfig)) {
                                    continue;
                                }
//                                String key = consumerConfig.keyOfServer();
//                                if (StringUtils.isEmpty(key)) {
//                                    logger.error("[messaging-common] {} consumerConfig keyOfServer is null:{}", consumerConfig, JSON.toJSONString(consumerConfig));
//                                    continue;
//                                }
                                //把target也作为key， 防止 ShadowConsumerExecute new 出来的对象有问题
                                String keyOfObj = objectData.getTarget().getClass() + "#" + Objects.hashCode(objectData.getTarget());
                                addShadowServerModule(keyOfObj, consumerConfig, shadowConsumerExecute, objectData.getTarget());
                            }
                        }
                    } catch (Throwable e) {
                        logger.error("prepare Config fail:{}", JSON.toJSONString(consumerRegisterModule.getConsumerRegister()), e);
                    } finally {
//                        BizClassLoaderService.clearBizClassLoader();
                    }
                }
            }
        }
    }

    private static boolean isValidConfig(ConsumerConfig consumerConfig) {
        String config = consumerConfig.keyOfConfig();
        if (config != null) {
            config = config.trim();
            if (config.startsWith("#")) {
                config = config.substring(1);
            }
            return !Pradar.isClusterTestPrefix(config);
        }
        return false;
    }

    private static ShadowConsumerExecute prepareShadowConsumerExecute(ConsumerRegisterModule consumerRegisterModule, SyncObjectData objectData) {
        ShadowConsumerExecute shadowConsumerExecute = consumerRegisterModule.getSyncObjectDataMap().get(objectData);
        if (shadowConsumerExecute == null) {
            try {
                shadowConsumerExecute = consumerRegisterModule.getConsumerRegister().getConsumerExecuteResourceInit().init();
            } catch (Throwable e) {
                throw new MessagingRuntimeException("can not init shadowConsumerExecute:" + JSON.toJSONString(consumerRegisterModule.getConsumerRegister()), e);
            }
            consumerRegisterModule.getSyncObjectDataMap().put(objectData, shadowConsumerExecute);
        }
        return shadowConsumerExecute;
    }

    private static void addShadowServerModule(String key, ConsumerConfig consumerConfig, ShadowConsumerExecute shadowConsumerExecute, Object bizTarget) {
        ShadowConsumer shadowConsumer = shadowConsumerMap.get(key);
        if (shadowConsumer == null) {
            synchronized (shadowConsumerMap) {
                shadowConsumer = shadowConsumerMap.get(key);
                if (shadowConsumer == null) {
                    shadowConsumer = new ShadowConsumer(shadowConsumerExecute, bizTarget);
                    shadowConsumerMap.put(key, shadowConsumer);
                }
            }
        }
        shadowConsumer.getConfigSet().add(consumerConfig);
    }

    static void runTask() {
        for (ConsumerRegisterModule consumerRegisterModule : registerList) {
            refreshSyncObj(consumerRegisterModule);
        }
        tryToStartConsumer();
    }

    private static void refreshSyncObj(ConsumerRegisterModule consumerRegisterModule) {
        try {
            synchronized (consumerRegisterModule) {
                boolean isRefreshed = false;
                for (Map.Entry<String, SyncObject> entry : consumerRegisterModule.getSyncObjectMap().entrySet()) {
                    //当时获取不到的，重新再获取一次
                    if (entry.getValue() == EMPTY_SYNC_OBJECT) {
                        String key = entry.getKey();
                        SyncObject newData = SyncObjectService.getSyncObject(key);
                        if (newData != null) {
                            logger.info("[messaging-common]success fetch sync data from {}", key);
                            entry.setValue(newData);
                            isRefreshed = true;
                        }
                    }
                }
                if (isRefreshed) {
                    //有拿到新注册上来的信息， 重新做一次注册
                    doConsumerRegister(consumerRegisterModule);
                }
            }
        } catch (Throwable e) {
            logger.warn("start task fail,will try next time: {}", JSON.toJSONString(consumerRegisterModule), e);
        }
    }

    private synchronized static void tryToStartConsumer() {
        Set<String> mqWhiteList = GlobalConfig.getInstance().getMqWhiteList();
        for (Map.Entry<String, ShadowConsumer> entry : shadowConsumerMap.entrySet()) {
            ShadowConsumer shadowConsumer = entry.getValue();
            String configs = null;
            try {
                configs = shadowConsumer.getConfigSet().stream().map(ConsumerConfig::keyOfConfig).collect(Collectors.joining(","));
                //todo@langyi 支持集群模式
                Set<ConsumerConfig> enableConfigSet = new HashSet<>();
                if (shadowConsumer.getConfigSet().isEmpty()) {
                    continue;
                }
                for (ConsumerConfig consumerConfig : shadowConsumer.getConfigSet()) {
                    String key = consumerConfig.keyOfConfig();
                    if (key == null) {
                        continue;
                    }
                    if (mqWhiteList.contains(key)) {
                        enableConfigSet.add(consumerConfig);
                    }
                }
                if (isConfigDiff(enableConfigSet, shadowConsumer.getEnableConfigSet())) {
                    stopAndClearShadowServer(shadowConsumer);
                    fetchShadowServer(shadowConsumer, configs);
                    logger.info("[messaging-common]success fetch shadowServer with config:{}", configs);
                    doStartShadowServer(shadowConsumer);
                    logger.info("[messaging-common]success start shadowServer with config:{}", configs);
                    shadowConsumer.setEnableConfigSet(enableConfigSet);
                }
            } catch (Throwable e) {
                logger.error("[messaging-common]try to start consumer server fail with key:{}", configs, e);
            } finally {

            }
        }
    }

    private static void stopAndClearShadowServer(ShadowConsumer shadowConsumer) {
        ShadowServer shadowServer = shadowConsumer.getShadowServer();
        if (shadowServer != null && shadowServer.isRunning()) {
            shadowServer.stop();
        }
        //todo 暂时先直接移除,可能会有没彻底关闭的情况， 后续需要增加这快的监控
        shadowConsumer.setShadowServer(null);
    }

    private static boolean isConfigDiff(Set<ConsumerConfig> enableConfig, Set<ConsumerConfig> currentConfig) {
        if (currentConfig.size() != enableConfig.size()) {
            return true;
        }
        for (ConsumerConfig consumerConfig : enableConfig) {
            if (!currentConfig.contains(consumerConfig)) {
                return true;
            }
        }
        return false;
    }

    private static void fetchShadowServer(ShadowConsumer shadowConsumer, String config) {
        try {
            ShadowServer shadowServer = shadowConsumer.getConsumerExecute().fetchShadowServer(shadowConsumer.getConfigSet().stream().map(ConsumerConfigWithData::new).collect(Collectors.toList()));
            shadowConsumer.setShadowServer(shadowServer);
        } catch (Exception e) {
            logger.error("init shadow server fail:{}", JSON.toJSONString(shadowConsumer.getConfigSet()), e);
        }
    }

    private static void doStartShadowServer(ShadowConsumer shadowConsumer) {
        try {
            if (!shadowConsumer.isStarted()) {
                shadowConsumer.getShadowServer().start();
            } else {
                logger.info("shadowConsumer {} is started, will not try to start!", JSON.toJSONString(shadowConsumer));
            }
        } catch (Throwable e) {
            logger.error("start shadow consumer fail:{}", JSON.toJSON(shadowConsumer.getConfigSet()), e);
        }
    }

    private static void startTask() {
        if (taskService == null) {
            initTask();
        }
    }

    private synchronized static void initTask() {
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
