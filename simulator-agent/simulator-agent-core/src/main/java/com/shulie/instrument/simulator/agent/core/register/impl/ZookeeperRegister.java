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
package com.shulie.instrument.simulator.agent.core.register.impl;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import com.shulie.instrument.simulator.agent.core.register.AgentStatus;
import com.shulie.instrument.simulator.agent.core.register.AgentStatusListener;
import com.shulie.instrument.simulator.agent.core.register.Register;
import com.shulie.instrument.simulator.agent.core.register.RegisterOptions;
import com.shulie.instrument.simulator.agent.core.util.AddressUtils;
import com.shulie.instrument.simulator.agent.core.util.ConfigUtils;
import com.shulie.instrument.simulator.agent.core.util.JvmArgsCheckUtils;
import com.shulie.instrument.simulator.agent.core.util.JvmArgsConstants;
import com.shulie.instrument.simulator.agent.core.util.PidUtils;
import com.shulie.instrument.simulator.agent.core.util.PropertyPlaceholderHelper;
import com.shulie.instrument.simulator.agent.core.zk.ZkClient;
import com.shulie.instrument.simulator.agent.core.zk.ZkHeartbeatNode;
import com.shulie.instrument.simulator.agent.core.zk.ZkNodeStat;
import com.shulie.instrument.simulator.agent.core.zk.impl.NetflixCuratorZkClientFactory;
import com.shulie.instrument.simulator.agent.core.zk.impl.ZkClientSpec;
import com.shulie.instrument.simulator.agent.spi.config.AgentConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.GetChildrenBuilder;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * zookeeper 注册器实现
 *
 * @author xiaobin.zfb
 * @since 2020/8/20 9:55 上午
 */
public class ZookeeperRegister implements Register {
    private final static Logger LOGGER = LoggerFactory.getLogger(ZookeeperRegister.class.getName());

    public static void main(String[] args) throws Exception {
        CuratorFramework client = CuratorFrameworkFactory.builder()
            .connectString("127.0.01:2181")
            .retryPolicy(new ExponentialBackoffRetry(1000, 3))
            .connectionTimeoutMs(60000)
            .sessionTimeoutMs(30000)
            .threadFactory(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "2222");
                }
            })
            .build();
        client.start();
        GetChildrenBuilder children = client.getChildren();
        for (String s : children.forPath("/config")) {
            System.out.println(s);
        }
    }

    /**
     * 基础路径
     */
    private String basePath;
    /**
     * app 名称
     */
    private String appName;
    /**
     * 心跳节点路径
     */
    private String heartbeatPath;
    /**
     * zk 客户端
     */
    private ZkClient zkClient;
    /**
     * 心跳节点
     */
    private ZkHeartbeatNode heartbeatNode;
    /**
     * 定时服务，定时上报
     */
    private ScheduledExecutorService executorService;
    private AtomicBoolean isStarted = new AtomicBoolean(false);

    private AgentConfig agentConfig;

    public ZookeeperRegister(AgentConfig agentConfig) {
        this.agentConfig = agentConfig;
    }

    private final String pid = String.valueOf(PidUtils.getPid());
    private final String name = PidUtils.getName();
    private String agentId = null;
    private static final String inputArgs = JSON.toJSONString(ManagementFactory.getRuntimeMXBean().getInputArguments());

    private String jvmArgsCheck = null;

    private byte[] getHeartbeatDatas() {
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
        StringBuilder errorMsg = new StringBuilder(AgentStatus.getErrorMessage());
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
        }
        //校验日志目录是否存在并且有权限
        String checkSimulatorLogPathResult = checkSimulatorLogPath(agentConfig.getLogPath());
        if (checkSimulatorLogPathResult != null) {
            agentStatus = AgentStatus.INSTALL_FAILED;
            errorMsg.append("启动参数日志目录校验异常：").append(checkSimulatorLogPathResult);
        }
        map.put("agentStatus", agentStatus);
        map.put("errorMsg", errorMsg.toString());

        String str = JSON.toJSONString(map);

        try {
            return str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return str.getBytes();
        }
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
     * 校验agent配置文件参数
     *
     * @return
     */
    private Map<String, String> checkConfigs() {
        Map<String, String> result = new HashMap<String, String>(32, 1);
        //当前agent使用配置文件的配置
        Map<String, String> agentFileConfigs = agentConfig.getAgentFileConfigs();
        Map<String, Object> agentConfigFromUrl =
            ConfigUtils.getFixedAgentConfigFromUrl(agentConfig.getTroWebUrl(), agentConfig.getAppName()
                , agentConfig.getAgentVersion(), agentConfig.getHttpMustHeaders());
        if (agentConfigFromUrl == null
            || agentConfigFromUrl.get("success") == null
            || !Boolean.parseBoolean(agentConfigFromUrl.get("success").toString())) {
            result.put("status", "false");
            result.put("errorMsg", "获取控制台配置信息失败,检查接口服务是否正常");
            return result;
        }
        boolean status = true;
        StringBuilder unEqualConfigs = new StringBuilder();

        JSONObject jsonObject = (JSONObject)agentConfigFromUrl.get("data");
        for (Map.Entry<String, String> entry : agentFileConfigs.entrySet()) {
            String value = (String)jsonObject.get(entry.getKey());
            if (entry.getValue().equals(value)) {
                result.put(entry.getKey(), "true");
            } else {
                status = false;
                result.put(entry.getKey(), "false");
                unEqualConfigs.append("参数key:").append(entry.getKey()).append(",").append("本地参数值:").append(
                        entry.getValue())
                    .append(",").append("远程参数值:").append(value).append(",");
            }
        }
        result.put("status", String.valueOf(result));
        if (!status) {
            result.put("errorMsg", unEqualConfigs.toString());
        }
        return result;

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
        return "zookeeper";
    }

    @Override
    public void init(RegisterOptions registerOptions) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("prepare to init zookeeper register.");
        }

        if (registerOptions == null) {
            throw new NullPointerException("RegisterOptions is null");
        }
        this.basePath = registerOptions.getRegisterBasePath();
        this.appName = registerOptions.getAppName();
        String registerBasePath = null;
        if (StringUtils.endsWith(basePath, "/")) {
            registerBasePath = this.basePath + appName;
        } else {
            registerBasePath = this.basePath + '/' + appName;
        }
        try {
            ZkClientSpec zkClientSpec = new ZkClientSpec();
            zkClientSpec.setZkServers(registerOptions.getZkServers());
            zkClientSpec.setConnectionTimeoutMillis(registerOptions.getConnectionTimeoutMillis());
            zkClientSpec.setSessionTimeoutMillis(registerOptions.getSessionTimeoutMillis());
            zkClientSpec.setThreadName("heartbeat");
            this.zkClient = NetflixCuratorZkClientFactory.getInstance().create(zkClientSpec);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        String client = getAgentId(true);
        try {
            this.zkClient.ensureDirectoryExists(registerBasePath);
        } catch (Exception e) {
            LOGGER.error("ensureDirectoryExists err:{}", registerBasePath, e);
        }
        this.heartbeatPath = registerBasePath + '/' + client;
        cleanExpiredNodes(registerBasePath);
        this.heartbeatNode = this.zkClient.createHeartbeatNode(this.heartbeatPath);
        AgentStatus.registerListener(new AgentStatusListener() {
            @Override
            public void onListen() {
                refresh();
            }
        });
        this.executorService = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "Scan-App-Jar-Thread");
                t.setDaemon(true);
                return t;
            }
        });
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("init zookeeper register successful.");
        }
    }

    /**
     * 清除过期的节点,防止 zookeeper 低版本时有版本不致的 bug 导致过期的心跳节点删除不掉的问题
     *
     * @param path
     */
    private void cleanExpiredNodes(String path) {
        try {
            List<String> children = this.zkClient.listChildren(path);
            if (children != null) {
                for (String node : children) {
                    ZkNodeStat stat = this.zkClient.getStat(path + '/' + node);
                    if (stat == null) {
                        continue;
                    }
                    if (stat.getEphemeralOwner() == 0) {
                        zkClient.deleteQuietly(path + '/' + node);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("clean expired register node error.", e);
        }
    }

    @Override
    public String getPath() {
        return heartbeatPath;
    }

    @Override
    public void start() {
        if (isStarted.get()) {
            return;
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("prepare to start zookeeper register.");
        }
        try {
            this.heartbeatNode.start();
            this.heartbeatNode.setData(getHeartbeatDatas());
            LOGGER.info("start zookeeper register successful.");
        } catch (Throwable e) {
            LOGGER.error("register node to zk for heartbeat node err: {}!", heartbeatPath, e);
        }
        isStarted.compareAndSet(false, true);
    }

    @Override
    public void stop() {
        if (!isStarted.compareAndSet(true, false)) {
            return;
        }
        if (this.heartbeatNode != null) {
            try {
                this.heartbeatNode.stop();
            } catch (Throwable e) {
                LOGGER.error("unregister node to zk for heartbeat node err: {}!", heartbeatPath, e);
            }
        }
        this.zkClient.deleteQuietly(this.heartbeatPath);
        try {
            this.zkClient.stop();
        } catch (Exception e) {
            LOGGER.error("stop zkClient failed!", e);
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Override
    public void refresh() {
        if (isStarted.get()) {
            try {
                heartbeatNode.setData(getHeartbeatDatas());
            } catch (Exception e) {
                LOGGER.error("[register] refresh node data to zk for heartbeat node err: {}!", heartbeatPath, e);
            }
        } else {
            /**
             * 使用定时线程去更新数据，防止连接 zk 时出现问题数据没有被更新上去
             */
            executorService.schedule(new Runnable() {
                @Override
                public void run() {
                    try {
                        heartbeatNode.setData(getHeartbeatDatas());
                    } catch (Exception e) {
                        LOGGER.error("[register] refresh node data to zk for heartbeat node err: {}!", heartbeatPath,
                            e);
                        executorService.schedule(this, 5, TimeUnit.SECONDS);
                    }
                }
            }, 0, TimeUnit.SECONDS);
        }

    }
}
