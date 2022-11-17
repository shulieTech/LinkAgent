package com.shulie.instrument.simulator.agent.core.register.impl;

import com.alibaba.fastjson.JSON;
import com.shulie.instrument.simulator.agent.core.register.AgentStatus;
import com.shulie.instrument.simulator.agent.core.register.AgentStatusListener;
import com.shulie.instrument.simulator.agent.core.register.Register;
import com.shulie.instrument.simulator.agent.core.register.RegisterOptions;
import com.shulie.instrument.simulator.agent.core.util.*;
import com.shulie.instrument.simulator.agent.spi.config.AgentConfig;
import io.shulie.takin.sdk.kafka.HttpSender;
import io.shulie.takin.sdk.kafka.MessageSendCallBack;
import io.shulie.takin.sdk.kafka.MessageSendService;
import io.shulie.takin.sdk.pinpoint.impl.PinpointSendServiceFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class KafkaRegister implements Register {
    private final static Logger LOGGER = LoggerFactory.getLogger(KafkaRegister.class.getName());

    private MessageSendService messageSendService;

    /**
     * 基础路径
     */
    private String basePath;
    /**
     * app 名称
     */
    private String appName;
    /**
     * 定时服务，定时上报
     */
    private ScheduledExecutorService executorService;

    private AgentConfig agentConfig;

    public KafkaRegister(AgentConfig agentConfig) {
        this.agentConfig = agentConfig;
    }

    private final String pid = String.valueOf(PidUtils.getPid());
    private final String name = PidUtils.getName();
    private String agentId = null;
    private static final String inputArgs = JSON.toJSONString(ManagementFactory.getRuntimeMXBean().getInputArguments());

    private String jvmArgsCheck = null;

    private Map<String, String> getHeartbeatDatas() {
        jvmArgsCheck = jvmArgsCheck == null ? JSON.toJSONString(
                JvmArgsCheckUtils.checkJvmArgs(System.getProperty("java.version"), inputArgs, agentConfig)) : jvmArgsCheck;

        Map<String, String> map = new HashMap<String, String>();
        map.put("address", AddressUtils.getLocalAddress());
        map.put("host", AddressUtils.getHostName());
        map.put("name", name);
        map.put("pid", pid);
        if (agentId == null) {
            agentId = getAgentId(false);
        }
        map.put("agentId", agentId);
        String agentStatus = AgentStatus.getAgentStatus();
        StringBuilder errorMsg = new StringBuilder(AgentStatus.getErrorMessage() == null ? "" : AgentStatus.getErrorMessage());

        map.put("errorCode", AgentStatus.getErrorCode());
        map.put("agentLanguage", "JAVA");
        map.put("agentVersion", agentConfig.getAgentVersion());
        map.put("simulatorVersion", AgentStatus.getSimulatorVersion());
        //应用启动jvm参数
        map.put("jvmArgs", inputArgs);
        //设置jdk版本
        String java_version = System.getProperty("java.version");
        map.put("jdk", java_version == null ? "" : java_version);

        // 放入当前环境及用户信息
        map.put("tenantAppKey", agentConfig.getTenantAppKey());
        map.put("envCode", agentConfig.getEnvCode());
        map.put("userId", agentConfig.getUserId());

        //设置agent配置文件参数
        //        map.put("agentFileConfigs", JSON.toJSONString(agentConfig.getAgentFileConfigs()));
        //参数比较
        //        map.put("agentFileConfigsCheck", JSON.toJSONString(checkConfigs()));
        map.put("jvmArgsCheck", jvmArgsCheck);
        if (!JvmArgsCheckUtils.getCheckJvmArgsStatus()) {
            agentStatus = AgentStatus.INSTALL_FAILED;
            errorMsg.append("启动参数校验失败：").append(jvmArgsCheck);
            AgentStatus.checkError("启动参数校验失败：" + jvmArgsCheck);
        }
        //校验日志目录是否存在并且有权限
        String checkSimulatorLogPathResult = checkSimulatorLogPath(agentConfig.getLogPath());
        if (checkSimulatorLogPathResult != null) {
            agentStatus = AgentStatus.INSTALL_FAILED;
            errorMsg.append("启动参数日志目录校验异常：").append(checkSimulatorLogPathResult);
            AgentStatus.checkError("启动参数日志目录校验异常：" + checkSimulatorLogPathResult);

        }
        map.put("agentStatus", agentStatus);
        map.put("errorMsg", errorMsg.toString());

        return map;
    }

    private String checkSimulatorLogPath(String simulatorLogPath) {
        if (StringUtils.isBlank(simulatorLogPath)) {
            return String.format(JvmArgsConstants.simulatorLogPathCodeErrorMsg_4, simulatorLogPath);
        }
        File file = new File(simulatorLogPath);
        if (!file.exists()) {
            return String.format(JvmArgsConstants.simulatorLogPathCodeErrorMsg_1, simulatorLogPath);
        }
        if (!file.canWrite()) {
            return String.format(JvmArgsConstants.simulatorLogPathCodeErrorMsg_3, simulatorLogPath);
        }
        if (!file.canRead()) {
            return String.format(JvmArgsConstants.simulatorLogPathCodeErrorMsg_2, simulatorLogPath);
        }
        return null;
    }

    /**
     * 获取agentId
     *
     * @param needUserInfo 是否需要用户信息
     * @return String字符串
     */
    public String getAgentId(boolean needUserInfo) {
        String agentId = agentConfig.getAgentId();
        if (StringUtils.isBlank(agentId)) {
            agentId = AddressUtils.getLocalAddress() + '-' + PidUtils.getPid();
        } else {
            Properties properties = new Properties();
            properties.setProperty("pid", String.valueOf(PidUtils.getPid()));
            properties.setProperty("hostname", AddressUtils.getHostName());
            properties.setProperty("ip", AddressUtils.getLocalAddress());
            PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}");
            agentId = propertyPlaceholderHelper.replacePlaceholders(agentId, properties);
        }

        // 新版探针兼容老版本的控制台，当envCode没有时不需要再agentId后加上租户信息
        if (needUserInfo && !StringUtils.isBlank(agentConfig.getEnvCode())) {
            // agentId中加上tenantAppKey、userId和currentEnv
            agentId += "&" + agentConfig.getEnvCode()
                    + ":" + (StringUtils.isBlank(agentConfig.getUserId()) ? "" : agentConfig.getUserId())
                    + ":" + (StringUtils.isBlank(agentConfig.getTenantAppKey()) ? "" : agentConfig.getTenantAppKey());
        }
        return agentId;
    }

    @Override
    public String getName() {
        return "kafka";
    }

    @Override
    public void init(RegisterOptions registerOptions) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("prepare to init kafka register.");
        }

        if (registerOptions == null) {
            throw new NullPointerException("RegisterOptions is null");
        }
        this.basePath = registerOptions.getRegisterBasePath();
        this.appName = registerOptions.getAppName();
        messageSendService = new PinpointSendServiceFactory().getKafkaMessageInstance();

        this.executorService = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "kafka-register-push-status-Thread");
                t.setDaemon(true);
                return t;
            }
        });
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("init kafka register successful.");
        }
    }

    @Override
    public String getPath() {
        return null;
    }

    @Override
    public void start() {
        executorService.schedule(new Runnable() {
            @Override
            public void run() {
                Map<String, String> heartbeatDatas = getHeartbeatDatas();
                heartbeatDatas.put("appName", appName);
                messageSendService.send(basePath, new HashMap<String, String>(), JSON.toJSONString(heartbeatDatas), new MessageSendCallBack() {
                    @Override
                    public void success() {
                    }

                    @Override
                    public void fail(String errorMessage) {
                        LOGGER.error("心跳信息发送失败，节点路径为:{},errorMessage为:{}", basePath, errorMessage);
                    }
                }, new HttpSender() {
                    @Override
                    public void sendMessage() {
                    }
                });
            }
        }, 60, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {

    }

    @Override
    public void refresh() {

    }
}
