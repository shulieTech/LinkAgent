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
package com.shulie.instrument.simulator.agent.core.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSONObject;

import com.shulie.instrument.simulator.agent.core.util.AddressUtils;
import com.shulie.instrument.simulator.agent.core.util.ConfigUtils;
import com.shulie.instrument.simulator.agent.core.util.PidUtils;
import com.shulie.instrument.simulator.agent.core.util.PropertyPlaceholderHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * agent 配置
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/17 8:09 下午
 */
public class CoreConfig {
    private final Logger logger = LoggerFactory.getLogger(CoreConfig.class);

    private static final RuntimeMXBean RUNTIME_MBEAN = ManagementFactory.getRuntimeMXBean();
    private static final Random RANDOM = new Random();
    private static int PID = 0;
    private static String PID_NAME = "";

    private final static String JPS_COMMAND = "jps";

    private final static String CONFIG_PATH_NAME = "config";
    private final static String AGENT_PATH_NAME = "agent";
    private final static String PROVIDER_PATH_NAME = "provider";
    private final static String LOG_PATH_NAME = "simulator.log.path";
    private final static String LOG_LEVEL_NAME = "simulator.log.level";
    private final static String MULTI_APP_SWITCH = "simulator.multiapp.switch.on";
    private final static String DEFAULT_LOG_LEVEL = "info";
    private final static String SIMULATOR_KEY_LITE = "simulator.lite";

    private static final String RESULT_FILE_PATH = System.getProperties().getProperty("user.home")
        + File.separator + "%s" + File.separator + ".simulator.token";
    /**
     * 存放所有的 agent 配置
     */
    private final Map<String, String> configs = new HashMap<String, String>();

    /**
     * agent 配置文件读取的配置
     */
    private final Map<String, String> agentFileConfigs = new HashMap<String, String>();

    /**
     * agent home 路径
     */
    private final String agentHome;

    /**
     * config 文件路径
     */
    private final String configFilePath;

    /**
     * spi 目录路径
     */
    private final String providerFilePath;

    /**
     * simulator 目录路径
     */
    private final String simulatorHome;

    /**
     * simulator 启动jar 路径
     */
    private final String simulatorJarPath;

    /**
     * log 配置文件路径
     */
    private final String logConfigFilePath;

    /**
     * attach的进程 id
     */
    private long attachId = -1L;

    /**
     * attach 的进程名称
     */
    private String attachName;

    private ScheduledExecutorService service;

    public CoreConfig(String agentHome) {
        //暂时无动态参数，不开启
        //        initFetchConfigTask();
        this.agentHome = agentHome;
        this.configFilePath = agentHome + File.separator + CONFIG_PATH_NAME;
        this.providerFilePath = agentHome + File.separator + PROVIDER_PATH_NAME;
        this.simulatorHome = agentHome + File.separator + AGENT_PATH_NAME;
        this.simulatorJarPath = this.simulatorHome + File.separator + "simulator" + File.separator
            + "instrument-simulator-agent.jar";
        this.logConfigFilePath = this.configFilePath + File.separator + "simulator-agent-logback.xml";
        File configFile = new File(configFilePath, "agent.properties");
        Properties properties = new Properties();
        properties.putAll(System.getProperties());
        InputStream configIn = null;
        try {
            if (!configFile.exists() || !configFile.canRead()) {
                configIn = CoreConfig.class.getClassLoader().getResourceAsStream("agent.properties");
            } else {
                configIn = new FileInputStream(configFile);
            }

            Enumeration enumeration = properties.propertyNames();
            while (enumeration.hasMoreElements()) {
                String name = (String)enumeration.nextElement();
                configs.put(name, properties.getProperty(name));
            }
            properties.clear();
            properties.load(configIn);
            enumeration = properties.propertyNames();
            while (enumeration.hasMoreElements()) {
                String name = (String)enumeration.nextElement();
                agentFileConfigs.put(name, properties.getProperty(name));
            }
            configs.putAll(agentFileConfigs);
        } catch (Throwable e) {
            throw new RuntimeException("Agent: read agent.properties file err:" + configFile.getAbsolutePath(), e);
        } finally {
            if (configIn != null) {
                try {
                    configIn.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }

    private void initFetchConfigTask() {
        this.service = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "Pradar-agent-Fetch-Config-Service");
                t.setDaemon(true);
                t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        logger.error("Thread {} caught a Unknown exception with UncaughtExceptionHandler", t.getName(),
                            e);
                    }
                });
                return t;
            }
        });

        service.scheduleAtFixedRate(getRunnableTask(), 60 * 3, 60 * 3, TimeUnit.SECONDS);
    }

    private Runnable getRunnableTask() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, Object> dynamicConfigs = ConfigUtils.getDynamicAgentConfigFromUrl(getTroWebUrl(),
                        getAppName(), "", getHttpMustHeaders());
                    if (dynamicConfigs == null
                        || dynamicConfigs.get("success") == null
                        || !Boolean.parseBoolean(dynamicConfigs.get("success").toString())) {
                        logger.error("getDynamicAgentConfigFromUrl failed");
                        return;
                    }
                    JSONObject jsonObject = (JSONObject)dynamicConfigs.get("data");
                    //                    for (Map.Entry<String, String> entry : jsonObject){
                    //                        if (!agentFileConfigs.containsKey(entry.getKey())){
                    //                            configs.put(entry.getKey(), entry.getValue());
                    //                        }
                    //                    }
                } catch (Throwable e) {
                    logger.error("CoreConfig getRunnableTask error ", e);
                }
            }
        };
    }

    public boolean isMultiAppSwitch() {
        String value = configs.get(MULTI_APP_SWITCH);
        return Boolean.parseBoolean(value);
    }

    /**
     * 获取日志级别
     *
     * @return 日志级别
     */
    public String getLogLevel() {
        String level = configs.get(LOG_LEVEL_NAME);
        if (StringUtils.isBlank(level)) {
            return DEFAULT_LOG_LEVEL;
        }
        return StringUtils.trim(level);
    }

    /**
     * 获取日志路径,日志路径如果不包含应用名称，则自动加上应用名称
     *
     * @return 日志路径
     */
    public String getLogPath() {
        String path = configs.get(LOG_PATH_NAME);
        if (StringUtils.isNotBlank(path)) {
            String cpath = path;
            if (!StringUtils.endsWith(cpath, "/")) {
                cpath += "/";
            }
            String appName = getAppName();
            /**
             * 这样判断是防止有路径包含了应用名称的字母但是不是应用名为目录
             */
            if (StringUtils.isNotBlank(appName) && StringUtils.indexOf(cpath, "/" + appName + "/") == -1) {
                cpath += appName;
                return isMultiAppSwitch() ? cpath + '/' + AddressUtils.getLocalAddress() + '/' + PidUtils.getPid()
                    : cpath;
            }
            return isMultiAppSwitch() ? path + '/' + AddressUtils.getLocalAddress() + '/' + PidUtils.getPid() : path;
        }
        String value = System.getProperty("user.home") + File.separator + "pradarlogs" + File.separator + getAppName();
        if (isMultiAppSwitch()) {
            value += '/' + PidUtils.getPid();
        }
        return value;
    }

    /**
     * 获取 boolean类型的属性
     *
     * @param propertyName 属性名称
     * @param defaultValue 默认值
     * @return property value
     */
    public boolean getBooleanProperty(String propertyName, boolean defaultValue) {
        if (!configs.containsKey(propertyName)) {
            return defaultValue;
        }
        return Boolean.parseBoolean(configs.get(propertyName));
    }

    /**
     * 获取 int 类型的属性
     *
     * @param propertyName 属性名称
     * @param defaultValue 默认值
     * @return property value
     */
    public int getIntProperty(String propertyName, int defaultValue) {
        if (!configs.containsKey(propertyName)) {
            return defaultValue;
        }
        String value = StringUtils.trim(configs.get(propertyName));
        if (NumberUtils.isDigits(value)) {
            return Integer.parseInt(value);
        }
        return defaultValue;
    }

    /**
     * 获取 long 类型的属性
     *
     * @param propertyName 属性名称
     * @param defaultValue 默认值
     * @return property value
     */
    public long getLongProperty(String propertyName, long defaultValue) {
        if (!configs.containsKey(propertyName)) {
            return defaultValue;
        }
        String value = StringUtils.trim(configs.get(propertyName));
        if (NumberUtils.isDigits(value)) {
            return Integer.parseInt(value);
        }
        return defaultValue;
    }

    /**
     * 获取属性配置值
     *
     * @param propertyName 属性名称
     * @param defaultValue 默认值
     * @return property value
     */
    public String getProperty(String propertyName, String defaultValue) {
        if (!configs.containsKey(propertyName)) {
            return defaultValue;
        }
        return StringUtils.trim(configs.get(propertyName));
    }

    /**
     * 获取 agent home 目录地址
     *
     * @return agent home
     */
    public String getAgentHome() {
        return agentHome;
    }

    /**
     * 获取配置文件路径
     *
     * @return config file path
     */
    public String getConfigFilePath() {
        return configFilePath;
    }

    /**
     * 获取 spi 目录路径
     *
     * @return spi file path
     */
    public String getProviderFilePath() {
        return providerFilePath;
    }

    /**
     * 获取 agent 目录路径
     *
     * @return agent file path
     */
    public String getSimulatorHome() {
        return simulatorHome;
    }

    /**
     * 获取 agent jar 路径
     *
     * @return agent jar path
     */
    public String getSimulatorJarPath() {
        return simulatorJarPath;
    }

    /**
     * 获取 zk 地址
     *
     * @return
     */
    public String getZkServers() {
        return getProperty("simulator.zk.servers", "localhost:2181");
    }

    /**
     * 获取zk 注册路径
     *
     * @return
     */
    public String getRegisterPath() {
        return getProperty("simulator.client.zk.path", "/config/log/pradar/client");
    }

    /**
     * 获取 zk 连接超时时间
     *
     * @return
     */
    public int getZkConnectionTimeout() {
        String connectionTimeout = getProperty("simulator.zk.connection.timeout.ms", "30000");
        if (NumberUtils.isDigits(connectionTimeout)) {
            return Integer.parseInt(connectionTimeout);
        }
        return 60000;
    }

    /**
     * 获取 zk session 超时时间
     *
     * @return
     */
    public int getZkSessionTimeout() {
        String sessionTimeout = getProperty("simulator.zk.session.timeout.ms", "60000");
        if (NumberUtils.isDigits(sessionTimeout)) {
            return Integer.parseInt(sessionTimeout);
        }
        return 60000;
    }

    /**
     * 获取应用名称
     *
     * @return 应用名称
     */
    public String getAppName() {
        //String value = getPropertyInAll("simulator.app.name");
        //if (StringUtils.isBlank(value)) {
        //    value = getPropertyInAll("pradar.project.name");
        //}
        //if (StringUtils.isBlank(value)) {
        //    value = getPropertyInAll("app.name");
        //}

        // takin lite项目应用名改成：进程号-进程名
        int pid = getPid();
        return pid + "-" + pidName();
    }

    /**
     * 获取pid集合
     *
     * @throws IOException io异常
     */
    public String pidName() {
        if (StringUtils.isNotBlank(PID_NAME)) {
            return PID_NAME;
        }
        PID_NAME = "default";
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(JPS_COMMAND);
        } catch (IOException e) {
            //ignore
        }

        if (process == null) {
            return PID_NAME;
        }

        try (InputStream inputStream = process.getInputStream();
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
             BufferedReader reader = new BufferedReader(inputStreamReader)) {
            process.waitFor(3, TimeUnit.SECONDS);
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().matches("^[0-9]*$")) {
                    String[] pidItem = line.split(" ");
                    if (String.valueOf(getPid()).equals(pidItem[0])) {
                        PID_NAME = pidItem[1];
                        return pidItem[1];
                    }
                }
            }
            return PID_NAME;
        } catch (Exception e) {
            return PID_NAME;
        }
    }

    public static int getPid() {
        if (PID == 0) {
            PID = getPid0();
        }
        return PID;
    }

    private static int getPid0() {
        final String name = RUNTIME_MBEAN.getName();
        final int pidIndex = name.indexOf('@');
        if (pidIndex == -1) {
            return getNegativeRandomValue();
        }
        String strPid = name.substring(0, pidIndex);
        try {
            return Integer.parseInt(strPid);
        } catch (NumberFormatException e) {
            return getNegativeRandomValue();
        }
    }

    private static int getNegativeRandomValue() {
        final int abs = Math.abs(RANDOM.nextInt());
        if (abs == Integer.MIN_VALUE) {
            return -1;
        }
        return abs;
    }

    private String getPropertyInAll(String key) {
        String value = System.getProperty(key);
        if (StringUtils.isBlank(value)) {
            value = getProperty(key, null);
        }
        if (StringUtils.isBlank(value)) {
            value = System.getenv(key);
        }
        return value;
    }

    /**
     * 获取 agentId
     *
     * @return 获取 agentId
     */
    public String getAgentId() {
        String agentId = internalGetAgentId();
        if (StringUtils.isBlank(agentId)) {
            return AddressUtils.getLocalAddress() + "-" + PidUtils.getPid();
        } else {
            Properties properties = new Properties();
            properties.setProperty("pid", String.valueOf(PidUtils.getPid()));
            properties.setProperty("hostname", AddressUtils.getHostName());
            properties.setProperty("ip", AddressUtils.getLocalAddress());
            PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}");
            return propertyPlaceholderHelper.replacePlaceholders(agentId, properties);
        }
    }

    private String internalGetAgentId() {
        String value = System.getProperty("simulator.agentId");
        if (StringUtils.isBlank(value)) {
            value = System.getProperty("pradar.agentId");
        }
        if (StringUtils.isBlank(value)) {
            value = getProperty("simulator.agentId", null);
        }
        if (StringUtils.isBlank(value)) {
            value = System.getenv("simulator.agentId");
        }
        return value;
    }

    public String getTenantAppKey() {
        // 兼容老版本，如果有user.app.key，则优先使用user.app.key
        String value = getProperty("user.app.key");
        if (StringUtils.isBlank(value)) {
            value = getProperty("tenant.app.key");
        }
        return value;
    }

    /**
     * 获取配置信息 优先级：启动参数 > 配置文件 > 环境变量
     *
     * @param key 配置key
     * @return value
     */
    private String getProperty(String key) {
        String value = System.getProperty(key, null);
        if (StringUtils.isBlank(value)) {
            value = getProperty(key, null);
        }
        if (StringUtils.isBlank(value)) {
            value = System.getenv(key);
        }
        return value;
    }

    public String getTroWebUrl() {
        return getProperty("tro.web.url");
    }

    public String getUserId() {
        return getProperty("pradar.user.id");
    }

    /**
     * 获取当前环境
     *
     * @return 当前环境
     */
    public String getEnvCode() {
        return getProperty("pradar.env.code");
    }

    /**
     * 获取发起http请求中必须包含的head
     *
     * @return map集合
     */
    public Map<String, String> getHttpMustHeaders() {
        Map<String, String> headerMap = new HashMap<String, String>();
        // 新探针兼容老版本的控制台，所以userAppKey和tenantAppKey都传
        headerMap.put("userAppKey", getTenantAppKey());
        headerMap.put("tenantAppKey", getTenantAppKey());
        headerMap.put("userId", getUserId());
        headerMap.put("envCode", getEnvCode());

        return headerMap;
    }

    /**
     * 获取 agent结果文件路径
     *
     * @return
     */
    public String getAgentResultFilePath() {
        return String.format(RESULT_FILE_PATH, getAppName());
    }

    /**
     * 获取 log 配置文件路径
     *
     * @return
     */
    public String getLogConfigFilePath() {
        return logConfigFilePath;
    }

    public InputStream getLogConfigFile() {
        File file = new File(getLogConfigFilePath());
        if (!file.exists()) {
            return CoreConfig.class.getClassLoader().getResourceAsStream("simulator-agent-logback.xml");
        } else {
            try {
                return new FileInputStream(getLogConfigFilePath());
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void setAttachId(long attachId) {
        this.attachId = attachId;
    }

    public void setAttachName(String attachName) {
        this.attachName = attachName;
    }

    public long getAttachId() {
        return this.attachId;
    }

    public String getAttachName() {
        return this.attachName;
    }

    public Map<String, String> getAgentFileConfigs() {
        return agentFileConfigs;
    }

    /**
     * 标记是否是lite版本探针
     *
     * @return true/false
     */
    public boolean isLite() {
        String isLite = System.getProperty("simulator.lite");
        if (StringUtils.isNotBlank(isLite)) {
            try {
                return Boolean.parseBoolean(isLite);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
}
