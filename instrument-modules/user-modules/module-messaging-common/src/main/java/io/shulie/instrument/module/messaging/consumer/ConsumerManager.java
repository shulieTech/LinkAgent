package io.shulie.instrument.module.messaging.consumer;

import com.pamirs.pradar.*;
import com.pamirs.pradar.bean.SyncObject;
import com.pamirs.pradar.bean.SyncObjectData;
import com.pamirs.pradar.gson.GsonFactory;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.SilenceSwitchOnEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.guard.SimulatorGuard;
import io.shulie.instrument.module.isolation.IsolationManager;
import io.shulie.instrument.module.isolation.enhance.EnhanceClass;
import io.shulie.instrument.module.isolation.register.ShadowProxyConfig;
import io.shulie.instrument.module.messaging.annoation.NotNull;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowConsumerExecute;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;
import io.shulie.instrument.module.messaging.consumer.isolation.ConsumerIsolationCache;
import io.shulie.instrument.module.messaging.consumer.isolation.ConsumerIsolationProxyFactory;
import io.shulie.instrument.module.messaging.consumer.module.*;
import io.shulie.instrument.module.messaging.consumer.module.isolation.ConsumerClass;
import io.shulie.instrument.module.messaging.exception.MessagingRuntimeException;
import io.shulie.instrument.module.messaging.handler.ConsumerRouteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author Licey
 * @date 2022/7/26
 */
public class ConsumerManager {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerManager.class);
    private static final Set<Field> notNullList = new HashSet<Field>();

    //所有注册上来的消费者拉起信息
    private final static List<ConsumerRegisterModule> registerList = new Vector<ConsumerRegisterModule>();

    //按照业务server分组的影子消费者信息，
    private final static Map<String, ShadowConsumer> shadowConsumerMap = new ConcurrentHashMap<>();

    private static ScheduledExecutorService taskService;
    private static final SyncObject EMPTY_SYNC_OBJECT = new SyncObject();
    private static final int initialDelay = 30;
    private static final int delay = 60;

    static {
        PradarEventListener listener = new PradarEventListener() {
            @Override
            public EventResult onEvent(IEvent event) {
                // 静默时关闭所有影子消费者
                if (event instanceof SilenceSwitchOnEvent) {
                    releaseAll();
                }
                return EventResult.IGNORE;
            }

            @Override
            public int order() {
                return 34;
            }
        };
        EventRouter.router().addListener(listener);
    }

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
        register(register, null, syncClassMethods);
    }

    public static void register(ConsumerRegister register, ConsumerIsolationRegister isolationRegister, String... syncClassMethods) {
        checkRegister(register);

        ConsumerRegisterModule consumerRegisterModule = new ConsumerRegisterModule(register, isolationRegister);
        if (syncClassMethods != null) {
            for (String syncClassMethod : syncClassMethods) {
                consumerRegisterModule.getSyncObjectMap().put(syncClassMethod, EMPTY_SYNC_OBJECT);
            }
        }
        registerList.add(consumerRegisterModule);

        startTask();
    }

    /**
     * 释放所有的影子消费者
     */
    public static void releaseAll() {
        for (Map.Entry<String, ShadowConsumer> entry : shadowConsumerMap.entrySet()) {
            releaseShadowConsumer(entry.getValue());
        }
        shadowConsumerMap.clear();
    }

    /**
     * 释放影子消费者资源
     *
     * @param shadowConsumer 影子消费者对象
     */
    private static void releaseShadowConsumer(ShadowConsumer shadowConsumer) {
        try {
            BizClassLoaderService.setBizClassLoader(shadowConsumer.getBizTarget().getClass().getClassLoader());
            if (shadowConsumer.getShadowServer() != null) {
                shadowConsumer.getShadowServer().stop();
            }
        } catch (Throwable t) {
            logger.error("[messaging-common] release shadow consumer error, obj:{}", GsonFactory.getGson().toJson(shadowConsumer), t);
        } finally {
            BizClassLoaderService.clearBizClassLoader();
        }
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
                        BizClassLoaderService.setBizClassLoader(objectData.getTarget().getClass().getClassLoader());
                        ShadowConsumerExecute shadowConsumerExecute = prepareShadowConsumerExecute(consumerRegisterModule, objectData);
                        List<ConsumerConfig> configList = shadowConsumerExecute.prepareConfig(objectData);
                        if (configList != null && !configList.isEmpty()) {
                            logger.info("[messaging-common]success prepareConfig from: {}, key:{}", entry.getKey(), configList.stream().map(ConsumerConfig::keyOfConfig).collect(Collectors.joining(",")));
                            for (ConsumerConfig consumerConfig : configList) {
                                if (!isValidConfig(consumerConfig)) {
                                    continue;
                                }
                                Object bizTarget = objectData.getTarget();
                                String keyOfObj = bizTarget.getClass() + "#" + Objects.hashCode(bizTarget);
                                // 如果一个消费者只能订阅一个topic
                                if (!consumerConfig.canSubscribeMultiTopics()) {
                                    keyOfObj = keyOfObj + "#" + consumerConfig.keyOfConfig();
                                }
                                enhanceIsolationBytecode(consumerRegisterModule, bizTarget);
                                addShadowServerModule(keyOfObj, consumerConfig, shadowConsumerExecute, bizTarget);
                            }
                        }
                    } catch (Throwable e) {
                        logger.error("[messaging-common]prepare Config fail:" + GsonFactory.getGson().toJson(consumerRegisterModule.getConsumerRegister()), e);
                    } finally {
                        BizClassLoaderService.clearBizClassLoader();
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
                throw new MessagingRuntimeException("can not init shadowConsumerExecute:" + GsonFactory.getGson().toJson(consumerRegisterModule.getConsumerRegister()), e);
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
        if (!PradarSwitcher.isClusterTestReady() || PradarService.isSilence()) {
            return;
        }
        // 避免出现 ConcurrentModificationException
        for (ConsumerRegisterModule consumerRegisterModule : new Vector<>(registerList)) {
            Thread.currentThread().setContextClassLoader(consumerRegisterModule.getConsumerRegister().getClass().getClassLoader());
            refreshSyncObj(consumerRegisterModule);
        }

        tryToStartConsumer();
    }

    private static void enhanceIsolationBytecode(ConsumerRegisterModule consumerRegisterModule, Object bizTarget) {
        ConsumerIsolationCache.put(bizTarget, null);

        if (consumerRegisterModule.isEnhanced() || consumerRegisterModule.getIsolationRegister() == null) {
            return;
        }
        List<ConsumerClass> consumerClassList = consumerRegisterModule.getIsolationRegister().getConsumerClassList();
        ShadowProxyConfig shadowProxyConfig = new ShadowProxyConfig("messaging-common_" + consumerRegisterModule.getName());
        //初始化好套壳的 lifecycle, 后续初始化好 shadowTarget 的时候进行替换.

        for (ConsumerClass consumerClass : consumerClassList) {
            shadowProxyConfig.addEnhance(toEnhanceClass(consumerClass, new ConsumerIsolationProxyFactory()));
        }
        IsolationManager.register(shadowProxyConfig);
        consumerRegisterModule.setEnhanced(true);
    }

    private static EnhanceClass toEnhanceClass(ConsumerClass consumerClass, ConsumerIsolationProxyFactory proxyFactory) {
        EnhanceClass enhanceClass = new EnhanceClass(consumerClass.getClassName(), consumerClass.getMethodList());
        enhanceClass.setConvertImpl(consumerClass.isConvertImpl());
        enhanceClass.setFactoryResourceInit(() -> proxyFactory);
        return enhanceClass;
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
            logger.warn("start task fail,will try next time: {}", GsonFactory.getGson().toJson(consumerRegisterModule), e);
        }
    }

    private synchronized static void tryToStartConsumer() {
        Set<String> mqWhiteList = GlobalConfig.getInstance().getMqWhiteList();
        for (Map.Entry<String, ShadowConsumer> entry : shadowConsumerMap.entrySet()) {
            ShadowConsumer shadowConsumer = entry.getValue();
            Thread.currentThread().setContextClassLoader(shadowConsumer.getConsumerExecute().getClass().getClassLoader());
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
                    try {
                        BizClassLoaderService.setBizClassLoader(shadowConsumer.getBizTarget().getClass().getClassLoader());
                        logger.info("[messaging-common]threadName: {}, bizClassLoad: {}", Thread.currentThread().getName(), shadowConsumer.getBizTarget().getClass().getClassLoader().toString());
                        stopAndClearShadowServer(shadowConsumer);
                        fetchShadowServer(shadowConsumer, configs, enableConfigSet);
                        logger.info("[messaging-common]success fetch shadowServer with config:{}", configs);
                        doStartShadowServer(shadowConsumer);
                        logger.info("[messaging-common]success start shadowServer with config:{}", configs);
                        shadowConsumer.setEnableConfigSet(enableConfigSet);
                    } finally {
                        BizClassLoaderService.clearBizClassLoader();
                    }

                }
            } catch (Throwable e) {
                logger.error("[messaging-common]try to start consumer server fail with key:" + configs, e);
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

    private static void fetchShadowServer(ShadowConsumer shadowConsumer, String config, Set<ConsumerConfig> enableConfigSet) {
        ShadowServer shadowServer = shadowConsumer.getConsumerExecute().fetchShadowServer(enableConfigSet.stream().map(ConsumerConfigWithData::new).collect(Collectors.toList()));
        if (shadowServer != null) {
            shadowConsumer.setShadowServer(shadowServer);
            ConsumerRouteHandler.addNotRouteObj(shadowServer.getShadowTarget());
            ConsumerIsolationCache.put(shadowConsumer.getBizTarget(), shadowServer);
        }
    }

    private static void doStartShadowServer(ShadowConsumer shadowConsumer) {
        if (!shadowConsumer.isStarted()) {
            shadowConsumer.getShadowServer().start();
        } else {
            logger.info("shadowConsumer {} is started, will not try to start!", GsonFactory.getGson().toJson(shadowConsumer));
        }
    }

    private static void startTask() {
        if (taskService == null) {
            initTask();
        }
    }

    private synchronized static void initTask() {
        if (taskService == null) {
            taskService = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "messaging-common-thread");
                    t.setDaemon(true);
                    t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                        @Override
                        public void uncaughtException(Thread t, Throwable e) {
                            logger.error("Thread {} caught a Unknown exception with UncaughtExceptionHandler", t.getName(), e);
                        }
                    });
                    return t;
                }
            });
            taskService.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    try {
                        SimulatorGuard.getInstance().doGuard(new Runnable() {
                            @Override
                            public void run() {
                                runTask();
                            }
                        });
                    } catch (Throwable e) {
                        logger.error("run messaging task fail!", e);
                    }
                }
            }, initialDelay, delay, TimeUnit.SECONDS);
        }
    }

}
