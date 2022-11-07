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
package com.shulie.instrument.simulator.agent;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.shulie.instrument.simulator.agent.utils.ModuleUtils;

import static java.lang.String.format;

/**
 * Simulator Agent启动器
 * <ul>
 * <li>这个类的所有静态属性都必须和版本、环境无关</li>
 * <li>这个类删除、修改方法时必须考虑多版本情况下，兼容性问题!</li>
 * </ul>
 */
public class AgentLauncher {
    private final static BootLogger LOGGER = BootLogger.getLogger(AgentLauncher.class.getName());

    private static boolean isAppendedBootstropJars;
    // Simulator默认主目录
    private static final String DEFAULT_SIMULATOR_HOME
        = new File(AgentLauncher.class.getProtectionDomain().getCodeSource().getLocation().getFile())
        .getParent();

    private static final String SIMULATOR_USER_MODULE_PATH
        = DEFAULT_SIMULATOR_HOME
        + File.separator + "modules";

    private static final String SIMULATOR_CLASSLOADER_JAR_PATH = DEFAULT_SIMULATOR_HOME
        + File.separator + "biz-classloader-jars";

    // 启动模式: agent方式加载
    private static final String LAUNCH_MODE_AGENT = "agent";

    // 启动模式: attach方式加载
    private static final String LAUNCH_MODE_ATTACH = "attach";

    // 启动默认
    private static String LAUNCH_MODE;

    // agentmain上来的结果输出到文件${HOME}/.simulator.token
    private static final String RESULT_FILE_PATH = System.getProperties().getProperty("user.home")
        + File.separator + "%s" + File.separator + ".simulator.token";

    private static volatile SimulatorClassLoader simulatorClassLoader;

    private static final Pattern SIMULATOR_HOME_PATTERN = Pattern.compile("(?i)^[/\\\\]([a-z])[/\\\\]");

    private static final String CLASS_OF_CORE_CONFIGURE = "com.shulie.instrument.simulator.core.CoreConfigure";
    private static final String CLASS_OF_PROXY_CORE_SERVER
        = "com.shulie.instrument.simulator.core.server.ProxyCoreServer";

    // ----------------------------------------------- 以下代码用于配置解析 -----------------------------------------------

    private static final String EMPTY_STRING = "";

    private static final String KEY_SIMULATOR_HOME = "home";

    private static final String KEY_SERVER_IP = "server.ip";
    private static final String DEFAULT_IP = "0.0.0.0";

    private static final String KEY_SERVER_PORT = "server.port";
    private static final String DEFAULT_PORT = "0";

    private static final String KEY_LOG_PATH = "logPath";
    private static final String DEFAULT_LOG_PATH = EMPTY_STRING;

    private static final String KEY_LOG_LEVEL = "logLevel";
    private static final String DEFAULT_LOG_LEVEL = "info";

    private static final String KEY_ZK_SERVERS = "zkServers";
    private static final String DEFAULT_ZK_SERVERS = "localhost:2181";

    private static final String KEY_REGISTER_PATH = "registerPath";
    private static final String DEFAULT_REGISTER_PATH = "/config/log/pradar/client";

    private static final String KEY_ZK_CONNECTION_TIMEOUT = "zkConnectionTimeout";
    private static final String DEFAULT_ZK_CONNECTION_TIMEOUT = "30000";

    private static final String KEY_ZK_SESSION_TIMEOUT = "zkSessionTimeout";
    private static final String DEFAULT_ZK_SESSION_TIMEOUT = "60000";

    private static final String KEY_AGENT_VERSION = "agentVersion";
    private static final String DEFAULT_AGENT_VERSION = "1.0.0.1";

    private static final String KEY_TENANT_APP_KEY = "tenantAppKey";
    private static final String DEFAULT_TENANT_APP_KEY = "";

    private static final String KEY_USER_ID = "userId";
    private static final String DEFAULT_USER_ID = "";

    private static final String KEY_TRO_WEB_URL = "troWebUrl";
    private static final String DEFAULT_TRO_WEB_URL = "";

    private static final String KEY_ENV_CODE = "envCode";
    private static final String DEFAULT_ENV_CODE = "";

    private static final String KEY_TENANT_CODE = "tenantCode";
    private static final String DEFAULT_TENANT_CODE = "";


    private static final String KEY_AGENT_MANAGER_URL = "agentManagerUrl";
    private static final String DEFAULT_AGENT_MANAGER_URL = "";

    private static final String KEY_SHADOW_PREPARATION_ENABLED = "shadowPreparationEnabled";
    private static final String DEFAULT_SHADOW_PREPARATION_ENABLED = "false";

    private static final String KEY_NACOS_TIMEOUT = "nacosTimeout";
    private static final String DEFAULT_NACOS_TIMEOUT = "";

    private static final String KEY_NACOS_SERVER_DDR = "nacosServerAddr";
    private static final String DEFAULT_NACOS_SERVER_DDR = "";

    private static final String KEY_NACOS_APP_DATA_ID = "nacosAppDataId";
    private static final String DEFAULT_NACOS_APP_DATA_ID = "";

    private static final String KEY_NACOS_APP_GROUP = "nacosAppGroup";
    private static final String DEFAULT_NACOS_APP_GROUP = "";

    private static final String KEY_NACOS_GLOBAL_DATA_ID = "nacosGlobalDataId";
    private static final String DEFAULT_NACOS_GLOBAL_DATA_ID = "";

    private static final String KEY_NACOS_GLOBAL_GROUP = "nacosGlobalGroup";
    private static final String DEFAULT_NACOS_GLOBAL_GROUP = "";

    private static final String KEY_NACOS_MANAGEMENT_DATA_ID = "nacosManagementDataId";
    private static final String DEFAULT_NACOS_MANAGEMENT_DATA_ID = "";

    private static final String KEY_NACOS_MANAGEMENT_GROUP = "nacosManagementGroup";
    private static final String DEFAULT_NACOS_MANAGEMENT_GROUP = "";

    private static final String KEY_NACOS_SWITCH_DATA_ID = "nacosSwitchDataId";
    private static final String DEFAULT_NACOS_SWITCH_DATA_ID = "";

    private static final String KEY_NACOS_SWITCH_GROUP = "nacosSwitchGroup";
    private static final String DEFAULT_NACOS_SWITCH_GROUP = "";

    private static final String KEY_APP_NAME = "app.name";
    private static final String DEFAULT_APP_NAME = "";

    private static final String KEY_AGENT_ID = "agentId";
    private static final String DEFAULT_AGENT_ID = "";

    private static final String KEY_MODULE_REPOSITORY_MODE = "module.repository.mode";
    private static final String DEFAULT_MODULE_REPOSITORY_MODE = "local";

    private static final String KEY_MODULE_REMOTE_REPOSITORY_ADDR = "module.remote.repository.addr";
    private static final String DEFAULT_MODULE_REPOSITORY_ADDR = "http://127.0.0.1:9888";

    private static final String KEY_PROPERTIES_FILE_PATH = "prop";
    private static final String KEY_AGENT_CONFIG_FILE_PATH = "agentConfigPath";

    private static String getSimulatorConfigPath(String simulatorHome) {
        return simulatorHome + File.separatorChar + "config";
    }

    private static String getSimulatorModulePath(String simulatorHome) {
        return simulatorHome + File.separatorChar + "system";
    }

    private static String getSimulatorCoreJarPath(String simulatorHome) {
        return simulatorHome + File.separatorChar + "lib" + File.separator + "instrument-simulator-core.jar";
    }

    private static List<File> getSimulatorBootstrapJars(String simulatorHome) {
        File file = new File(simulatorHome, "bootstrap");
        return Arrays.asList(file.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        }));
    }

    private static String getSimulatorPropertiesPath(String simulatorHome) {
        return getSimulatorConfigPath(simulatorHome) + File.separator + "simulator.properties";
    }

    private static String getSimulatorProviderPath(String simulatorHome) {
        return simulatorHome + File.separatorChar + "provider";
    }

    private static void addBootResource(ClassLoader classLoader) {
        if (ModuleUtils.isModuleSupported()) {
            return;
        }
        try {
            Class moduleBootLoaderOfClass = classLoader.loadClass(
                "com.shulie.instrument.simulator.agent.utils.BootResourceLoader");
            Method loadModuleSupportOfMethod = moduleBootLoaderOfClass.getDeclaredMethod("addResource",
                java.util.List.class);
            loadModuleSupportOfMethod.invoke(null, Collections.emptyList());
        } catch (Throwable e) {
            LOGGER.error("SIMULATOR: add resource to boot class path err...", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * start with start opts
     *
     * @param featureString start params
     *                      [prop]
     * @param inst          inst
     */
    public static void premain(String featureString, Instrumentation inst) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                "SIMULATOR: agent starting with agent mode. args=" + (featureString == null ? "" : featureString));
        }
        LAUNCH_MODE = LAUNCH_MODE_AGENT;
        long startTime = System.nanoTime();
        try {
            final Map<String, String> featureMap = toFeatureMap(featureString);
            String appName = featureMap.get(KEY_APP_NAME);
            writeAttachResult(
                    appName,
                    install(featureMap, inst, false)
            );
        } catch (Throwable e) {
            System.out.println("========" + e.getMessage());
            e.printStackTrace();
            LOGGER.error("SIMULATOR: premain execute error!", e);
        } finally {
            LOGGER.info(
                "simulator server start successful. cost:" + ((System.nanoTime() - startTime) / 1000000000) + "s");
        }
    }

    public static void syncModulePremain(String featureString, Instrumentation inst) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                "SIMULATOR-sync: agent starting with premain mode. args=" + (featureString == null ? "" : featureString));
        }
        LAUNCH_MODE = LAUNCH_MODE_AGENT;
        long startTime = System.nanoTime();
        try {
            final Map<String, String> featureMap = toFeatureMap(featureString);
            install(featureMap, inst, true);
        } catch (Throwable e) {
            System.out.println("========" + e.getMessage());
            e.printStackTrace();
            LOGGER.error("SIMULATOR-sync: premain execute error!", e);
        } finally {
            LOGGER.info(
                "simulator-sync start successful. cost:" + ((System.nanoTime() - startTime) / 1000000000) + "s");
        }
    }

    /**
     * 动态加载 启动入口
     * attach agent
     *
     * @param featureString start params
     *                      [ip,port,prop]
     * @param inst          inst
     */
    public static void agentmain(String featureString, Instrumentation inst) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                "SIMULATOR: agent starting with attach mode. args=" + (featureString == null ? "" : featureString));
        }
        try {
            LAUNCH_MODE = LAUNCH_MODE_ATTACH;
            final Map<String, String> featureMap = toFeatureMap(featureString);
            String appName = featureMap.get(KEY_APP_NAME);
            writeAttachResult(
                    appName,
                    install(featureMap, inst, false)
            );
        } catch (Throwable e) {
            e.printStackTrace();
            LOGGER.error("SIMULATOR: agentmain execute error!", e);
        }
    }

    /**
     * write start result file
     * <p>
     * IP;PORT
     * </p>
     *
     * @param installInfo listen server[IP:PORT]
     */
    private static synchronized void writeAttachResult(String appName, final InstallInfo installInfo) {
        String path = String.format(RESULT_FILE_PATH, appName);
        final File file = new File(path);
        if (file.exists()
            && (!file.isFile()
            || !file.canWrite())) {

            throw new RuntimeException("write to result file : " + file + " failed.");
        } else {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            FileWriter fw = null;
            try {
                fw = new FileWriter(file, false);
                fw.append(
                    format("%s;%s;%s\n",
                        installInfo.inetSocketAddress.getHostName(),
                        installInfo.inetSocketAddress.getPort(),
                        installInfo.installVersion
                    )
                );
                fw.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (null != fw) {
                    try {
                        fw.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }
        file.deleteOnExit();
    }

    private static synchronized ClassLoader defineClassLoader(final String coreJar) throws Throwable {
        if (simulatorClassLoader == null) {
            simulatorClassLoader = new SimulatorClassLoader(coreJar);
        }
        return simulatorClassLoader;
    }

    /**
     * uninstall simulator
     *
     * @throws Throwable throws Throwable when uninstall failed.
     */
    @SuppressWarnings("unused")
    public static synchronized void uninstall() throws Throwable {
        if (null == simulatorClassLoader) {
            return;
        }

        // 关闭服务器
        final Class<?> classOfProxyServer = simulatorClassLoader.loadClass(CLASS_OF_PROXY_CORE_SERVER);
        classOfProxyServer.getMethod("destroy")
            .invoke(classOfProxyServer.getMethod("getInstance").invoke(null));

        // 关闭SimulatorClassLoader
        simulatorClassLoader.closeIfPossible();
        /**
         * 删除结果文件
         */
        String path = RESULT_FILE_PATH;
        final File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * install simulator with current VM
     *
     * @param featureMap start params
     * @param inst       inst
     * @return serverIP :PORT
     */
    private static synchronized InstallInfo install(final Map<String, String> featureMap,
        final Instrumentation inst,boolean isOnlySyncModule) {

        final String propertiesFilePath = getPropertiesFilePath(featureMap);
        String config = System.getProperty("config");
        final String coreFeatureString = toFeatureString(featureMap);
        final String agentConfigFilePath = getAgentConfigFilePath(featureMap);

        try {
            final String home = getSimulatorHome(featureMap);

            if (!isAppendedBootstropJars) {
                // 将bootstrap下所有的jar注入到BootstrapClassLoader
                List<File> bootstrapFiles = getSimulatorBootstrapJars(home);
                for (File file : bootstrapFiles) {
                    if (file.isHidden()) {
                        LOGGER.warn(
                                "prepare to append bootstrap file " + file.getAbsolutePath()
                                        + " but found a hidden file. skip it.");
                        continue;
                    }
                    if (!file.isFile()) {
                        LOGGER.warn("prepare to append bootstrap file " + file.getAbsolutePath()
                                + " but found a directory file. skip it.");
                        continue;
                    }
                    LOGGER.info("append bootstrap file=" + file.getAbsolutePath());
                    inst.appendToBootstrapClassLoaderSearch(new JarFile(file));
                }
                isAppendedBootstropJars = true;
            }

            // 构造自定义的类加载器，尽量减少Simulator对现有工程的侵蚀
            final ClassLoader simulatorClassLoader = defineClassLoader(
                getSimulatorCoreJarPath(home)
                // SIMULATOR_CORE_JAR_PATH
            );

            /**
             * 如果jdk9以下版本
             */
            if (!ModuleUtils.isModuleSupported()) {
                /**
                 * 将 bootstrap 资源添加到 bootstrap classpath 下面
                 * 因为有一些实现加载 bootstrap 下的类时使用 getResource 方式加载
                 * appendToBootstrapClassLoaderSearch方法将不能使用 getResource 能搜索到
                 * 资源，所以需要再处理一下
                 */
                addBootResource(AgentLauncher.class.getClassLoader());
            }

            ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(simulatorClassLoader);
                // CoreConfigure类定义
                final Class<?> classOfConfigure = simulatorClassLoader.loadClass(CLASS_OF_CORE_CONFIGURE);

                // 反序列化成CoreConfigure类实例
                final Object objectOfCoreConfigure = classOfConfigure.getMethod("toConfigure", Class.class,
                                String.class,
                                String.class, String.class, Instrumentation.class)
                        .invoke(null, AgentLauncher.class, coreFeatureString, propertiesFilePath, agentConfigFilePath,
                                inst);
                // CoreServer类定义
                final Class<?> classOfProxyServer = simulatorClassLoader.loadClass(CLASS_OF_PROXY_CORE_SERVER);

                // 获取CoreServer单例
                final Object objectOfProxyServer = classOfProxyServer
                        .getMethod("getInstance")
                        .invoke(null);

                if (isOnlySyncModule) {
                    LOGGER.info("to start syncModule ###########################################");
                    classOfProxyServer
                            .getMethod("prepareSyncModule", classOfConfigure, Instrumentation.class)
                            .invoke(objectOfProxyServer, objectOfCoreConfigure, inst);
                    LOGGER.info("syncModule end ###########################################");
                    return null;
                }else{
                    // CoreServer.isBind()
                    final boolean isBind = (Boolean)classOfProxyServer.getMethod("isBind").invoke(objectOfProxyServer);

                    // 如果未绑定,则需要绑定一个地址
                    if (!isBind) {
                        try {
                            classOfProxyServer
                                    .getMethod("bind", classOfConfigure, Instrumentation.class)
                                    .invoke(objectOfProxyServer, objectOfCoreConfigure, inst);
                        } catch (Throwable t) {
                            LOGGER.error("AGENT: agent bind error {}", t);
                            classOfProxyServer.getMethod("destroy").invoke(objectOfProxyServer);
                            throw t;
                        }

                    } else {
                        LOGGER.warn("AGENT: agent start already. skip it. ");
                    }

                    // 返回服务器绑定的地址
                    InetSocketAddress inetSocketAddress = (InetSocketAddress)classOfProxyServer
                            .getMethod("getLocal")
                            .invoke(objectOfProxyServer);
                    String version = classOfConfigure.getMethod("getSimulatorVersion").invoke(objectOfCoreConfigure)
                            .toString();
                    return new InstallInfo(inetSocketAddress, version);

                }
            } finally {
                if(config != null){
                    System.setProperty("config",config);
                }else{
                    System.clearProperty("config");
                }
                Thread.currentThread().setContextClassLoader(currentClassLoader);
            }

        } catch (Throwable cause) {
            //如果是agent模式则不阻塞应用正常启动,但是会将错误打印出来,
            //如果是attach模式则直接将异常抛至上层调用方
            try {
                uninstall();
            } catch (Throwable ignore) {
            }
            throw new RuntimeException("simulator attach failed.", cause);
        }

    }

    private static boolean isNotBlank(final String string) {
        return null != string
            && string.length() > 0
            && !string.matches("^\\s*$");
    }

    private static boolean isBlank(final String string) {
        return !isNotBlank(string);
    }

    private static String getDefault(final String string, final String defaultString) {
        return isNotBlank(string)
            ? string
            : defaultString;
    }

    private static Map<String, String> toFeatureMap(final String featureString) {
        final Map<String, String> featureMap = new LinkedHashMap<String, String>();

        // 不对空字符串进行解析
        if (isBlank(featureString)) {
            return featureMap;
        }

        // KV对片段数组
        final String[] kvPairSegmentArray = featureString.split(";");
        if (kvPairSegmentArray.length <= 0) {
            return featureMap;
        }

        for (String kvPairSegmentString : kvPairSegmentArray) {
            if (isBlank(kvPairSegmentString)) {
                continue;
            }
            final String[] kvSegmentArray = kvPairSegmentString.split("=");
            if (kvSegmentArray.length != 2
                || isBlank(kvSegmentArray[0])
                || isBlank(kvSegmentArray[1])) {
                continue;
            }
            featureMap.put(decode(kvSegmentArray[0]), decode(kvSegmentArray[1]));
        }

        return featureMap;
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    private static String getDefault(final Map<String, String> map, final String key, final String defaultValue) {
        return null != map
            && !map.isEmpty()
            ? getDefault(map.get(key), defaultValue)
            : defaultValue;
    }

    private static String OS = System.getProperty("os.name").toLowerCase();

    private static boolean isWindows() {
        return OS.contains("win");
    }

    // 获取主目录
    private static String getSimulatorHome(final Map<String, String> featureMap) {
        String home = getDefault(featureMap, KEY_SIMULATOR_HOME, DEFAULT_SIMULATOR_HOME);
        if (isWindows()) {
            Matcher m = SIMULATOR_HOME_PATTERN.matcher(home);
            if (m.find()) {
                home = m.replaceFirst("$1:/");
            }
        }
        return home;
    }

    //获取 log path
    private static String getLogPath(final Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_LOG_PATH, DEFAULT_LOG_PATH);
    }

    //获取 log level
    private static String getLogLevel(final Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_LOG_LEVEL, DEFAULT_LOG_LEVEL);
    }

    private static String getZkServers(final Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_ZK_SERVERS, DEFAULT_ZK_SERVERS);
    }

    private static String getRegisterPath(final Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_REGISTER_PATH, DEFAULT_REGISTER_PATH);
    }

    private static String getZkConnectionTimeout(final Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_ZK_CONNECTION_TIMEOUT, DEFAULT_ZK_CONNECTION_TIMEOUT);
    }

    private static String getZkSessionTimeout(final Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_ZK_SESSION_TIMEOUT, DEFAULT_ZK_SESSION_TIMEOUT);
    }

    private static String getAgentVersion(final Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_AGENT_VERSION, DEFAULT_AGENT_VERSION);
    }

    private static String getTenantAppKey(Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_TENANT_APP_KEY, DEFAULT_TENANT_APP_KEY);
    }

    private static String getUserId(Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_USER_ID, DEFAULT_USER_ID);
    }

    private static String getTroWebUrl(Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_TRO_WEB_URL, DEFAULT_TRO_WEB_URL);
    }

    private static String getEnvCode(Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_ENV_CODE, DEFAULT_ENV_CODE);
    }

    private static String getTenantCode(Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_TENANT_CODE, DEFAULT_TENANT_CODE);
    }

    private static String getAgentManagerUrl(Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_AGENT_MANAGER_URL, DEFAULT_AGENT_MANAGER_URL);
    }

    private static String getNacosTimeout(Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_NACOS_TIMEOUT, DEFAULT_NACOS_TIMEOUT);
    }

    private static String getNacosServerAddr(Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_NACOS_SERVER_DDR, DEFAULT_NACOS_SERVER_DDR);
    }

    private static String getNacosAppDataId(Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_NACOS_APP_DATA_ID, DEFAULT_NACOS_APP_DATA_ID);
    }

    private static String getNacosAppGroup(Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_NACOS_APP_GROUP, DEFAULT_NACOS_APP_GROUP);
    }

    private static String getNacosGlobalDataId(Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_NACOS_GLOBAL_DATA_ID, DEFAULT_NACOS_GLOBAL_DATA_ID);
    }

    private static String getNacosGlobalGroup(Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_NACOS_GLOBAL_GROUP, DEFAULT_NACOS_GLOBAL_GROUP);
    }

    private static String getNacosManagementDataId(Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_NACOS_MANAGEMENT_DATA_ID, DEFAULT_NACOS_MANAGEMENT_DATA_ID);
    }

    private static String getNacosManagementGroup(Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_NACOS_MANAGEMENT_GROUP, DEFAULT_NACOS_MANAGEMENT_GROUP);
    }

    private static String getNacosSwitchDataId(Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_NACOS_SWITCH_DATA_ID, DEFAULT_NACOS_SWITCH_DATA_ID);
    }

    private static String getNacosSwitchGroup(Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_NACOS_SWITCH_GROUP, DEFAULT_NACOS_SWITCH_GROUP);
    }

    private static String getShadowPreparationEnabled(Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_SHADOW_PREPARATION_ENABLED, DEFAULT_SHADOW_PREPARATION_ENABLED);
    }

    private static String getAppName(final Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_APP_NAME, DEFAULT_APP_NAME);
    }

    private static String getAgentId(final Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_AGENT_ID, DEFAULT_AGENT_ID);
    }

    private static String getRepositoryMode(final Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_MODULE_REPOSITORY_MODE, DEFAULT_MODULE_REPOSITORY_MODE);
    }

    private static String getRemoteRepositoryAddr(final Map<String, String> featureMap) {
        return getDefault(featureMap, KEY_MODULE_REMOTE_REPOSITORY_ADDR, DEFAULT_MODULE_REPOSITORY_ADDR);
    }

    // 获取容器配置文件路径
    private static String getPropertiesFilePath(final Map<String, String> featureMap) {
        return getDefault(
            featureMap,
            KEY_PROPERTIES_FILE_PATH,
            getSimulatorPropertiesPath(getSimulatorHome(featureMap))
            // SIMULATOR_PROPERTIES_PATH
        );
    }

    // 获取agent配置文件路径
    private static String getAgentConfigFilePath(final Map<String, String> featureMap) {
        return featureMap.get(KEY_AGENT_CONFIG_FILE_PATH);
    }

    // 如果featureMap中有对应的key值，则将featureMap中的[K,V]对合并到builder中
    private static void appendFromFeatureMap(final StringBuilder builder,
        final Map<String, String> featureMap,
        final String key,
        final String defaultValue) {
        if (featureMap.containsKey(key)) {
            builder.append(format("%s=%s;", key, getDefault(featureMap, key, defaultValue)));
        }
    }

    // 将featureMap中的[K,V]对转换为featureString
    private static String toFeatureString(final Map<String, String> featureMap) {
        final String simulatorHome = getSimulatorHome(featureMap);
        final StringBuilder builder = new StringBuilder(
            format(
                ";app_name=%s;agentId=%s;config=%s;system_module=%s;mode=%s;simulator_home=%s;user_module=%s;"
                    + "classloader_jars=%s;provider=%s;module_repository_mode=%s;"
                    + "module_repository_addr=%s;log_path=%s;log_level=%s;zk_servers=%s;register_path=%s;"
                    + "zk_connection_timeout=%s;zk_session_timeout=%s;agent_version=%s;tenant.app.key=%s;pradar.user"
                    + ".id=%s;tro.web.url=%s;pradar.env.code=%s;shulie.agent.tenant.code=%s;shulie.agent.manager.url=%s;"
                    + "shadow.preparation.enabled=%s;nacos.timeout=%s;nacos.serverAddr=%s;nacos.app.dataId=%s;nacos.app.group=%s;"
                    + "nacos.global.dataId=%s;nacos.global.group=%s;nacos.management.dataId=%s;nacos.management.group=%s;"
                    + "nacos.switch.dataId=%s;nacos.switch.group=%s",
                getAppName(featureMap),
                getAgentId(featureMap),
                getSimulatorConfigPath(simulatorHome),
                // SIMULATOR_CONFIG_PATH,
                getSimulatorModulePath(simulatorHome),
                // SIMULATOR_MODULE_PATH,
                LAUNCH_MODE,
                simulatorHome,
                // SIMULATOR_HOME,
                SIMULATOR_USER_MODULE_PATH,
                SIMULATOR_CLASSLOADER_JAR_PATH,
                getSimulatorProviderPath(simulatorHome),
                // REPOSITORY MODE (local/remote)
                getRepositoryMode(featureMap),
                // REPOSITORY REMOTE ADDR
                getRemoteRepositoryAddr(featureMap),
                // LOG PATH
                getLogPath(featureMap),
                //LOG LEVEL
                getLogLevel(featureMap),
                // ZK SERVERS
                getZkServers(featureMap),
                // REGISTER PATH
                getRegisterPath(featureMap),
                // ZK CONNECTION TIMEOUT
                getZkConnectionTimeout(featureMap),
                // ZK SESSION TIMEOUT
                getZkSessionTimeout(featureMap),
                // AGENT VERSION
                getAgentVersion(featureMap),
                //Tenant APP KEY
                getTenantAppKey(featureMap),
                //USE ID
                getUserId(featureMap),
                //TRO WEB
                getTroWebUrl(featureMap),
                // CURRENT ENV
                getEnvCode(featureMap),
                // TENANT CODE
                getTenantCode(featureMap),
                // agent manager url
                getAgentManagerUrl(featureMap),
                // shadow.preparation.enabled
                getShadowPreparationEnabled(featureMap),
                // nacos.timeout
                getNacosTimeout(featureMap),
                // nacos.serverAddr
                getNacosServerAddr(featureMap),
                // nacos.app.dataId
                getNacosAppDataId(featureMap),
                // nacos.app.group
                getNacosAppGroup(featureMap),
                // nacos.global.dataId
                getNacosGlobalDataId(featureMap),
                // nacos.global.group
                getNacosGlobalGroup(featureMap),
                // nacos.management.dataId
                getNacosManagementDataId(featureMap),
                // nacos.management.group
                getNacosManagementGroup(featureMap),
                // nacos.switch.dataId
                getNacosSwitchDataId(featureMap),
                // nacos.switch.group
                getNacosSwitchGroup(featureMap)
            )
        );

        // 合并IP(如有)
        appendFromFeatureMap(builder, featureMap, KEY_SERVER_IP, DEFAULT_IP);

        // 合并PORT(如有)
        appendFromFeatureMap(builder, featureMap, KEY_SERVER_PORT, DEFAULT_PORT);

        return builder.toString();
    }

    private static class InstallInfo {

        private final InetSocketAddress inetSocketAddress;

        private final String installVersion;

        private InstallInfo(InetSocketAddress inetSocketAddress, String installVersion) {
            this.inetSocketAddress = inetSocketAddress;
            this.installVersion = installVersion;
        }
    }

}
