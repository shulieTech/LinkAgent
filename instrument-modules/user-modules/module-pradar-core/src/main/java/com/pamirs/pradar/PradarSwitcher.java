/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.pradar;


import com.pamirs.pradar.event.Event;
import com.pamirs.pradar.event.PradarSwitchEvent;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * pradar全局开关
 */
public class PradarSwitcher {
    private final static Logger LOGGER = LoggerFactory.getLogger(PradarSwitcher.class);

    private static ConcurrentMap<String, Boolean> configSwitchers = new ConcurrentHashMap<String, Boolean>();

    private static CopyOnWriteArrayList<PradarSwitcherListener> listeners = new CopyOnWriteArrayList<PradarSwitcherListener>();

    /**
     * 压测环境还未准备好
     */
    public static final String PRADAR_CLUSTER_TEST_NOT_READY = "Pradar Cluster Tester is not ready now";

    /**
     * license无效
     */
    public static final String PRADAR_LICENSE_IS_VALID = "Pradar license is invalid";
    /**
     * pradar开关已经关闭
     */
    public static final String PRADAR_SWITCHER_OFF = "Pradar switcher is off";

    /**
     * 是否支持trace
     */
    private static boolean isTraceEnabled = getTraceEnabled();

    /**
     * 是否支持 monitor
     */
    private static boolean isMonitorEnabled = getMonitorEnabled();

    /**
     * RPC 日志开关，关闭之后不会记录 rpc 日志，但是会照生成 Pradar 上下文
     */
    static private boolean rpcStatus = getRpcStatus();

    /**
     * monitor 日志开关，关闭之后不会记录 monitor 日志,但是 monitor 日志还是会获取
     */
    static private boolean monitorStatus = getMonitorStatus();

    /**
     * 数据透传开关，关闭之后不会透传任何数据
     */
    static private boolean userDataEnabled = getUserDataEnabled();

    /**
     * 是否支持压测
     */
    private static boolean isClusterTestEnabled = getPressEnabled();

    /**
     * kafka是否支持消息header
     */
    private static boolean isKafkaMessageHeadersEnabled = getKafkaMessageHeadersEnabled();
    /**
     * rabbitmq是否使用routingKey
     */
    private static boolean isRabbitmqRoutingkeyEnabled = getRabbitmqRoutingkeyEnabled();

    /**
     * 压测开关 -- 控制台
     */
    private static AtomicBoolean clusterTestSwitch = new AtomicBoolean(true);

    /**
     * 静默开关 -- 控制台
     */
    private static AtomicBoolean silentSwitch = new AtomicBoolean(false);


    /**
     * 白名单开关 -- 控制台
     */
    private static AtomicBoolean whiteListSwitchOn = new AtomicBoolean(true);

    /**
     * 实时同步配置开关
     */
    private static AtomicBoolean configSyncSwitchOn = new AtomicBoolean(true);

    /**
     * {@link PradarLogDaemon} 是否启动
     */
    private static boolean isPradarLogDaemonEnabled = getPradarLogDaemonEnabled();

    /**
     * -pradar.localip.use = false 返回是否使用本地IP的配置参数,默认false
     */
    static public final boolean USE_LOCAL_IP = Boolean.valueOf(getUseLocalIp());

    /**
     * 采样率是否使用zk配置
     */
    static private boolean samplingZkConfig = false;

    /**
     * 是否有效期内
     */
    private static volatile boolean isValid = true;

    /**
     * 压测环境是否已经准备好
     */
    private static volatile boolean isClusterTestReady = false;

    /**
     * 错误编码
     */
    private static String errorCode;

    /**
     * 错误信息
     */
    private static String errorMsg;

    /**
     * 当前线程http可以放过的前缀
     */
    public static ThreadLocal<String> httpPassPrefix = new ThreadLocal<String>();

    static private final String getUseLocalIp() {
        String useLocalIp = getSystemProperty("pradar.localip.use", "false");
        return useLocalIp;
    }

    static public void registerListener(PradarSwitcherListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void turnConfigSwitcherOn(String configName) {
        configSwitchers.get(configName);
        Boolean oldValue = configSwitchers.put(configName, Boolean.TRUE);
        if (oldValue != null && !oldValue) {
            for (PradarSwitcherListener listener : listeners) {
                listener.onListen(new PradarSwitchEvent(isClusterTestEnabled(), getClusterTestUnableReason()));
            }
        }

    }

    public static void turnConfigSwitcherOff(String configName) {
        Boolean oldValue = configSwitchers.get(configName);
        configSwitchers.put(configName, Boolean.FALSE);
        if (oldValue == null || oldValue) {
            for (PradarSwitcherListener listener : listeners) {
                listener.onListen(new PradarSwitchEvent(isClusterTestEnabled(), getClusterTestUnableReason()));
            }
        }
    }

    private static boolean isAllConfigValid() {
        for (Map.Entry<String, Boolean> entry : configSwitchers.entrySet()) {
            if (!entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 打开压测开关 -- 控制台
     */
    static public boolean turnClusterTestSwitchOn() {
        boolean before = isClusterTestEnabled();
        try {
            boolean compareAndSet = clusterTestSwitch.compareAndSet(false, true);
            if (compareAndSet) {
                LOGGER.info("turnClusterTestSwitchOn");
            }
            return compareAndSet;
        } finally {
            boolean after = isClusterTestEnabled();
            if (before != after) {
                for (PradarSwitcherListener listener : listeners) {
                    listener.onListen(new PradarSwitchEvent(after, getClusterTestUnableReason()));
                }
            }
        }
    }

    /**
     * 关闭压测开关 -- 控制台
     */
    static public boolean turnClusterTestSwitchOff() {
        boolean before = isClusterTestEnabled();
        try {
            boolean compareAndSet = clusterTestSwitch.compareAndSet(true, false);
            if (compareAndSet) {
                LOGGER.info("turnClusterTestSwitchOff");
            }
            return compareAndSet;
        } finally {
            boolean after = isClusterTestEnabled();
            if (before != after) {
                for (PradarSwitcherListener listener : listeners) {
                    listener.onListen(new PradarSwitchEvent(after, getClusterTestUnableReason()));
                }
            }
        }
    }

    /**
     * 返回压测开关是否打开
     *
     * @return
     */
    static public boolean clusterTestSwitchOn() {
        return clusterTestSwitch.get();
    }


    /**
     * 返回静默模式是否打开
     *
     * @return
     */
    static public boolean silenceSwitchOn() {
        return silentSwitch.get();
    }

    /***
     * 打开静默模式
     */
    static public void turnSilenceSwitchOn() {
        silentSwitch.set(true);
    }

    /**
     * 关闭静默模式
     */
    static public void turnSilenceSwitchOff() {
        silentSwitch.set(false);
    }

    /**
     * 关闭白名单开关 -- 控制台
     */
    static public boolean turnWhiteListSwitchOn() {
        boolean before = isClusterTestEnabled();
        try {
            boolean compareAndSet = whiteListSwitchOn.compareAndSet(false, true);
            if (compareAndSet) {
                LOGGER.info("turnWhiteListSwitchOn");
            }
            return compareAndSet;
        } finally {
            boolean after = isClusterTestEnabled();
            if (before != after) {
                for (PradarSwitcherListener listener : listeners) {
                    listener.onListen(new PradarSwitchEvent(after, getClusterTestUnableReason()));
                }
            }
        }
    }

    /**
     * 打开白名单开关 -- 控制台
     */
    static public boolean turnWhiteListSwitchOff() {
        boolean before = isClusterTestEnabled();
        try {
            boolean compareAndSet = whiteListSwitchOn.compareAndSet(true, false);
            if (compareAndSet) {
                LOGGER.info("turnWhiteListSwitchOff");
            }
            return compareAndSet;
        } finally {
            boolean after = isClusterTestEnabled();
            if (before != after) {
                for (PradarSwitcherListener listener : listeners) {
                    listener.onListen(new PradarSwitchEvent(after, getClusterTestUnableReason()));
                }
            }
        }
    }

    /**
     * 返回白名单开关是否打开
     */
    static public boolean whiteListSwitchOn() {
        return whiteListSwitchOn.get();
    }

    /**
     * 关闭配置实时同步开关 -- 应用内异常
     */
    static public boolean turnConfigSyncSwitchOn() {
        return configSyncSwitchOn.compareAndSet(false, true);
    }

    /**
     * 打开配置实时同步开关 -- 应用内异常
     */
    static public boolean turnConfigSyncSwitchOff() {
        return configSyncSwitchOn.compareAndSet(true, false);
    }

    /**
     * 返回配置实时同步开关是否打开
     */
    static public boolean configSyncSwitchOn() {
        return configSyncSwitchOn.get();
    }


    /**
     * 设置压测环境已经准备完成
     */
    static public void clusterTestReady() {
        boolean before = isClusterTestEnabled();
        isClusterTestReady = true;
        configSwitchers.clear();
        boolean after = isClusterTestEnabled();
        if (before != after) {
            for (PradarSwitcherListener listener : listeners) {
                listener.onListen(new PradarSwitchEvent(after, getClusterTestUnableReason()));
            }
        }
    }

    /**
     * 设置压测环境没有准备完成
     */
    static public void clusterTestPrepare() {
        /**
         * 只要有一次 ready 则不会再 prepare
         */
        if (isClusterTestReady) {
            return;
        }
        boolean before = isClusterTestEnabled();
        isClusterTestReady = false;
        boolean after = isClusterTestEnabled();
        if (before != after) {
            for (PradarSwitcherListener listener : listeners) {
                listener.onListen(new PradarSwitchEvent(after, getClusterTestUnableReason()));
            }
        }
    }

    /**
     * 压测环境是否已经加载完成准备就绪
     *
     * @return
     */
    static public boolean isClusterTestReady() {
        return isClusterTestReady;
    }

    /**
     * @return 采样频率 1/x
     */
    static public int getSamplingInterval() {
        return GlobalConfig.getInstance().getSimulatorDynamicConfig().getTraceSamplingInterval();
    }

    /**
     * @return 压测采样频率 1/x
     */
    static public int getClusterTestSamplingInterval() {
        return GlobalConfig.getInstance().getSimulatorDynamicConfig().getClusterTestTraceSamplingInterval();
    }

    static public boolean isSwitchSaveBusinessTrace() {
        return GlobalConfig.getInstance().getSimulatorDynamicConfig().isSwitchSaveBusinessTrace();
    }

    /**
     * 检查是否透传
     */
    static public final boolean isUserDataEnabled() {
        return userDataEnabled;
    }

    static public final boolean getRpcStatus() {
        Boolean flag = Boolean.valueOf(getSystemProperty("pradar.rpc.switch", "true"));
        LOGGER.info("pradar.rpc.switch={}", flag);
        return flag;
    }

    static public final boolean getMonitorStatus() {
        Boolean flag = Boolean.valueOf(getSystemProperty("pradar.monitor.switch", "true"));
        LOGGER.info("pradar.monitor.switch={}", flag);
        return flag;
    }

    static public final boolean getUserDataEnabled() {
        Boolean flag = Boolean.valueOf(getSystemProperty("pradar.user.data.switch", "true"));
        LOGGER.info("pradar.user.data.switch={}", flag);
        return flag;
    }

    static public final boolean isRpcOff() {
        return !rpcStatus;
    }

    static public final boolean isMonitorOff() {
        return !monitorStatus;
    }

    private static Boolean getPressEnabled() {
        Boolean flag = Boolean.valueOf(getSystemProperty("pradar.switcher.press", "true"));
        LOGGER.info("pradar.switcher.press={}", flag);
        return flag;
    }

    private static boolean getKafkaMessageHeadersEnabled() {
        Boolean flag = Boolean.valueOf(getSystemProperty("is.kafka.message.headers", "false"));
        LOGGER.info("is.kafka.shadow.message={}", flag);
        return flag;
    }

    private static boolean getRabbitmqRoutingkeyEnabled() {
        Boolean flag = Boolean.valueOf(getSystemProperty("is.rabbitmq.routingkey", "true"));
        LOGGER.info("is.rabbitmq.routingkey.enable={}", flag);
        return flag;
    }

    private static Boolean getPradarLogDaemonEnabled() {
        Boolean flag = Boolean.valueOf(getSystemProperty("pradar.switcher.log.daemon", "true"));
        LOGGER.info("pradar.switcher.log.daemon={}", flag);
        return flag;
    }

    private static Boolean getTraceEnabled() {
        Boolean flag = Boolean.valueOf(getSystemProperty("pradar.switcher.trace", "true"));
        LOGGER.info("pradar.switcher.trace={}", flag);
        return flag;
    }

    private static Boolean getMonitorEnabled() {
        Boolean flag = Boolean.valueOf(getSystemProperty("pradar.switcher.monitor", "true"));
        LOGGER.info("pradar.switcher.monitor={}", flag);
        return flag;
    }

    static private final String getSystemProperty(String key, String defau) {
        try {
            return System.getProperty(key, defau);
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * 每一个switcher都需要判断是否isValid
     *
     * @return
     */
    public static boolean isTraceEnabled() {
        return isTraceEnabled && isValid;
    }

    /**
     * 判断 monitor 是否可用
     *
     * @return
     */
    public static boolean isMonitorEnabled() {
        return isMonitorEnabled && isValid;
    }

    /**
     * 压测是否可用
     * 前提条件为压测环境已经准备好,并且压测开关是打开的
     * 并且压测开关是打开的并且在有效期内或者体验模式开启
     *
     * @return
     */
    public static boolean isClusterTestEnabled() {
        return isClusterTestReady && clusterTestSwitch.get() && (isClusterTestEnabled && isValid) && isAllConfigValid();
    }

    public static String getErrorCode() {
        return errorCode;
    }

    public static void setErrorCode(String code) {
        errorCode = code;
    }

    public static void setErrorMsg(String msg) {
        errorMsg = msg;
    }

    public static String getErrorMsg() {
        if (errorMsg != null) {
            return errorMsg;
        }
        return getClusterTestUnableReason();
    }

    /**
     * 获取压测不可用的原因
     *
     * @return
     */
    public static String getClusterTestUnableReason() {
        if (!isClusterTestReady) {
            return PRADAR_CLUSTER_TEST_NOT_READY + ":" + AppNameUtils.appName();
        }
        if (!(isClusterTestEnabled && isValid)) {
            if (!isValid) {
                return PRADAR_LICENSE_IS_VALID + ":" + AppNameUtils.appName();
            }
        }
        return PRADAR_SWITCHER_OFF + ":" + AppNameUtils.appName();
    }

    public static boolean isKafkaMessageHeadersEnabled() {
        return GlobalConfig.getInstance().getSimulatorDynamicConfig().getIsKafkaMessageHeaders(isKafkaMessageHeadersEnabled);
    }

    public static boolean isRabbitmqRoutingkeyEnabled() {
        return isRabbitmqRoutingkeyEnabled;
    }

    /**
     * pradar log daemon是否可用
     *
     * @return
     */
    public static boolean isPradarLogDaemonEnabled() {
        return isPradarLogDaemonEnabled && isValid;
    }


    public synchronized static void invalid() {
        boolean before = isClusterTestEnabled();
        LOGGER.info("Cluster Tester is force closed....");
        isValid = false;
        boolean after = isClusterTestEnabled();
        if (before != after) {
            for (PradarSwitcherListener listener : listeners) {
                listener.onListen(new PradarSwitchEvent(after, "Cluster Tester is force closed...."));
            }
        }
    }

    public static void destroy() {
        configSwitchers.clear();
        listeners.clear();
    }

    static public boolean getSamplingZkConfig() {
        return samplingZkConfig;
    }

    public static void setSamplingZkConfig(boolean b) {
        samplingZkConfig = b;
    }

    /**
     * pradar压测开关监听器
     */
    public interface PradarSwitcherListener {
        /**
         * 监听pradar开关的状态
         *
         * @param event
         */
        void onListen(Event event);
    }
}
