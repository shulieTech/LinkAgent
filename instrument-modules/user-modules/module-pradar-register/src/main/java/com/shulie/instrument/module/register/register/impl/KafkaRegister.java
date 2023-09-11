package com.shulie.instrument.module.register.register.impl;

import com.pamirs.pradar.*;
import com.pamirs.pradar.common.HttpUtils;
import com.pamirs.pradar.common.IOUtils;
import com.pamirs.pradar.common.RuntimeUtils;
import com.pamirs.pradar.gson.GsonFactory;
import com.pamirs.pradar.pressurement.base.util.PropertyUtil;
import com.shulie.instrument.module.register.NodeRegisterModule;
import com.shulie.instrument.module.register.register.Register;
import com.shulie.instrument.module.register.register.RegisterOptions;
import com.shulie.instrument.module.register.utils.SimulatorStatus;
import com.shulie.instrument.simulator.api.obj.ModuleLoadInfo;
import com.shulie.instrument.simulator.api.obj.ModuleLoadStatusEnum;
import com.shulie.instrument.simulator.api.resource.SimulatorConfig;
import io.shulie.takin.pinpoint.thrift.dto.TStressTestAgentHeartbeatData;
import io.shulie.takin.sdk.kafka.HttpSender;
import io.shulie.takin.sdk.kafka.MessageSendCallBack;
import io.shulie.takin.sdk.kafka.MessageSendService;
import io.shulie.takin.sdk.pinpoint.impl.PinpointSendServiceFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class KafkaRegister implements Register {

    private final static Logger LOGGER = LoggerFactory.getLogger(KafkaRegister.class.getName());
    private String basePath;
    private String appName;
    private String md5;
    private SimulatorConfig simulatorConfig;
    private Set<String> jars;
    private final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    private static final String inputArgs = GsonFactory.getGson().toJson(ManagementFactory.getRuntimeMXBean().getInputArguments());
    private final AtomicInteger sendCount = new AtomicInteger(0);
    private MessageSendService messageSendService;
    /**
     * 定时服务，定时上报
     */
    private ScheduledExecutorService executorService;

    private TStressTestAgentHeartbeatData getHeartbeatData() {
        TStressTestAgentHeartbeatData agentHeartbeatData = new TStressTestAgentHeartbeatData();
        agentHeartbeatData.setAgentId(Pradar.AGENT_ID_NOT_CONTAIN_USER_INFO);
        // 放入当前环境及用户信息
        agentHeartbeatData.setTenantAppKey(Pradar.PRADAR_TENANT_KEY);
        agentHeartbeatData.setEnvCode(Pradar.PRADAR_ENV_CODE);
        agentHeartbeatData.setAppName(appName);
        try {
            agentHeartbeatData.setUserId(Long.parseLong(Pradar.PRADAR_USER_ID));
        } catch (Exception e) {
            LOGGER.error("没有获取到正确的userId", e);
        }

        if (sendCount.get() <= 5){
            sendCount.incrementAndGet();
            agentHeartbeatData.setSimulatorVersion(simulatorConfig.getSimulatorVersion());
            agentHeartbeatData.setAddress(PradarCoreUtils.getLocalAddress());
            agentHeartbeatData.setPid(RuntimeUtils.getPid());
            agentHeartbeatData.setAgentLanguage("JAVA");
            agentHeartbeatData.setJvmArgsCheck("");
            agentHeartbeatData.setJvmArgs(inputArgs);
            agentHeartbeatData.setHost(PradarCoreUtils.getHostName());
            agentHeartbeatData.setName(RuntimeUtils.getName());
            agentHeartbeatData.setAgentVersion(simulatorConfig.getAgentVersion());
            agentHeartbeatData.setMd5(md5);

            //设置jdk版本
            String java_version = System.getProperty("java.version");
            agentHeartbeatData.setJdk(java_version == null ? "" : java_version);

            //服务的 url
            String serviceUrl = "http://" + simulatorConfig.getServerAddress().getAddress().getHostAddress() + ":"
                    + simulatorConfig.getServerAddress().getPort()
                    + "/simulator";
            agentHeartbeatData.setServiceName(serviceUrl);
            agentHeartbeatData.setPort(simulatorConfig.getServerAddress().getPort());
        }

        agentHeartbeatData.setStatus(PradarSwitcher.isClusterTestEnabled());

        if (!PradarSwitcher.isClusterTestEnabled()) {
            agentHeartbeatData.setErrorCode(StringUtils.defaultIfBlank(PradarSwitcher.getErrorCode(), ""));
            agentHeartbeatData.setErrorMsg(StringUtils.defaultIfBlank(PradarSwitcher.getErrorMsg(), ""));
        }

        if (!SimulatorStatus.statusCalculated()) {
            boolean moduleLoadResult = getModuleLoadResult();
            if (!moduleLoadResult) {
                SimulatorStatus.installFailed(GsonFactory.getGson().toJson(NodeRegisterModule.moduleLoadInfoManager.getModuleLoadInfos().values()));
            } else {
                SimulatorStatus.installed();
            }
            agentHeartbeatData.setModuleLoadResult(moduleLoadResult);
        }
        agentHeartbeatData.setAgentStatus(SimulatorStatus.getStatus());
        if (SimulatorStatus.isInstallFailed()) {
            agentHeartbeatData.setErrorMsg(agentHeartbeatData.getErrorMsg() + ";模块加载异常");
        }

        return agentHeartbeatData;
    }

    /**
     * 动态参数不需要校验参数是否生效，
     */
    private static final List<String> excludeCheckConfig = new ArrayList<String>(6);

    static {
        excludeCheckConfig.add("pradar.trace.log.version");
        excludeCheckConfig.add("pradar.monitor.log.version");
        excludeCheckConfig.add("pradar.error.log.version");
        excludeCheckConfig.add("is.kafka.message.headers");
        excludeCheckConfig.add("trace.samplingInterval");
        excludeCheckConfig.add("trace.mustsamplingInterval");
        excludeCheckConfig.add("pradar.sampling.interval");
    }

    private boolean getModuleLoadResult() {
        for (Map.Entry<String, ModuleLoadInfo> entry : NodeRegisterModule.moduleLoadInfoManager.getModuleLoadInfos()
                .entrySet()) {
            if (entry.getValue().getStatus() == ModuleLoadStatusEnum.LOAD_FAILED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getName() {
        return "kafka";
    }

    private String toJarFileString(Set<String> jars) {
        if (jars == null || jars.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String jar : jars) {
            builder.append(jar).append(';');
        }
        return builder.toString();
    }


    @Override
    public void init(RegisterOptions registerOptions) {
        if (registerOptions == null) {
            throw new NullPointerException("RegisterOptions is null");
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[pradar-register] prepare to init kafkaRegister . {}", Pradar.AGENT_ID_CONTAIN_USER_INFO);
        }
        this.basePath = registerOptions.getRegisterBasePath();
        this.appName = registerOptions.getAppName();
        this.md5 = registerOptions.getMd5();
        this.simulatorConfig = registerOptions.getSimulatorConfig();
        this.messageSendService = new PinpointSendServiceFactory().getKafkaMessageInstance();
        this.executorService = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "kafka-register-push-client-Thread");
                t.setDaemon(true);
                return t;
            }
        });
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[pradar-register] init kafkaRegister successful. {}", Pradar.AGENT_ID_CONTAIN_USER_INFO);
        }

    }

    @Override
    public String getPath() {
        return null;
    }

    @Override
    public void start() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[pradar-register] prepare to start kafkaRegister. {}", Pradar.AGENT_ID_CONTAIN_USER_INFO);
        }

        try {
            this.jars = loadAllJars();
            getHeartbeatData();
            this.pushMiddlewareJarInfo();
            executorService.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    messageSendService.send(getHeartbeatData(), new MessageSendCallBack() {
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
            }, 60, 60, TimeUnit.SECONDS);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("[pradar-register] start kafkaRegister successful. {}", Pradar.AGENT_ID_CONTAIN_USER_INFO);
            }
        } catch (Throwable e) {
            LOGGER.error("[pradar-register] start kafkaRegister for heartbeat node err: {}!", basePath, e);
        }

    }

    @Override
    public void stop() {

    }

    @Override
    public void refresh() {

    }

    private Set<String> loadAllJars() {

        String classPath = runtimeMXBean.getClassPath();
        String[] files = StringUtils.split(classPath, File.pathSeparator);
        if (files == null || files.length == 0) {
            return Collections.EMPTY_SET;
        }
        Set<String> list = new HashSet<String>();
        String javaHome = System.getProperty("java.home");
        String simulatorHome = simulatorConfig.getSimulatorHome();
        String tmpDir = System.getProperty("java.io.tmpdir");
        for (String file : files) {
            /**
             * 如果是 jdk 的 jar 包，过滤掉
             */
            if (StringUtils.isNotBlank(javaHome) && StringUtils.startsWith(file, javaHome)) {
                continue;
            }
            /**
             * 如果是仿真器的 jar 包，过滤掉
             */
            if (StringUtils.startsWith(file, simulatorHome)) {
                continue;
            }
            /**
             * 如果是监时目录加载的 jar 包，则过滤掉, simulator 所有的扩展 jar 包
             * 都会从临时目录加载
             */
            if (StringUtils.isNotBlank(tmpDir) && StringUtils.startsWith(file, tmpDir)) {
                continue;
            }

            /**
             * 如果 jar包是这一些打头的，也过滤掉
             */
            if (StringUtils.startsWith(file, "pradar-")
                    || StringUtils.startsWith(file, "simulator-")
                    || StringUtils.startsWith(file, "module-")) {
                continue;
            }

            /**
             * 如果有依赖包则不添加自身作为依赖。
             */
            if (processSpringBootProject(list, file)) {
                continue;
            }

            list.add(file);
        }
        return list;
    }

    private boolean processSpringBootProject(Set<String> list, String file) {
        boolean hasDependencies = false;
        if (file.endsWith(".jar") || file.endsWith(".war")) {
            JarFile jarFile = null;
            try {
                jarFile = new JarFile(file);
                final Enumeration<JarEntry> entries = jarFile.entries();
                final String tempPath = System.getProperty("java.io.tmpdir");
                final String randomPath = UUID.randomUUID().toString().replaceAll("-", "");
                final String filePath = (tempPath.endsWith(File.separator) ? tempPath : tempPath + File.separator)
                        + randomPath;
                final File fileDir = new File(filePath);
                if (!fileDir.exists()) {
                    final boolean mkdirs = fileDir.mkdirs();
                    if (!mkdirs) {
                        LOGGER.error("中间件信息上报：创建临时目录失败。");
                        return false;
                    }
                    for (File listFile : fileDir.listFiles()) {
                        listFile.deleteOnExit();
                    }
                    fileDir.deleteOnExit();
                }
                while (entries.hasMoreElements()) {
                    final JarEntry jarEntry = entries.nextElement();
                    String jarPath = "";
                    if (jarEntry.getName().endsWith(".jar")) {
                        InputStream inputStream = null;
                        FileOutputStream fileOutputStream = null;
                        try {
                            inputStream = jarFile.getInputStream(jarEntry);
                            final String[] split = jarEntry.getName().split(File.separator);
                            jarPath = (filePath.endsWith(File.separator) ? filePath : filePath + File.separator)
                                    + split[
                                    split.length - 1];
                            fileOutputStream = new FileOutputStream(jarPath);
                            IOUtils.copy(inputStream, fileOutputStream);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (inputStream != null) {
                                try {
                                    inputStream.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            if (fileOutputStream != null) {
                                try {
                                    fileOutputStream.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        list.add(jarPath);
                        hasDependencies = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                if (jarFile != null) {
                    try {
                        jarFile.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return hasDependencies;
    }

    private static final String PUSH_MIDDLEWARE_URL = "/agent/push/application/middleware";

    private void pushMiddlewareJarInfo() {
        MessageSendService messageSendService = new PinpointSendServiceFactory().getKafkaMessageInstance();
        String body = "";
        try {
            final String troControlWebUrl = PropertyUtil.getTroControlWebUrl();
            LOGGER.info(String.format("中间件管理：jars：%s", jars));
            final Set<String> jarInfoSet = ScanJarPomUtils.scanByJarPaths(jars);
            final ArrayList<MiddlewareRequest> middlewareList = new ArrayList<MiddlewareRequest>();
            for (String jarInfoStr : jarInfoSet) {
                final String[] split = jarInfoStr.split(":");
                middlewareList.add(new MiddlewareRequest(split[1], split[0], split[2]));
            }
            final PushMiddlewareVO pushMiddlewareVO = new PushMiddlewareVO(AppNameUtils.appName(), middlewareList);
            body = GsonFactory.getGson().toJson(pushMiddlewareVO);
            final String finalBody = body;
            messageSendService.send(PUSH_MIDDLEWARE_URL, HttpUtils.getHttpMustHeaders(), body, new MessageSendCallBack() {
                @Override
                public void success() {
                    LOGGER.info(String.format("中间件信息上报成功,body:%s", finalBody));
                }

                @Override
                public void fail(String errorMessage) {
                    LOGGER.info(String.format("中间件信息上报失败,body:%s,失败信息：%s", finalBody, errorMessage));
                }
            }, new HttpSender() {
                @Override
                public void sendMessage() {
                    final HttpUtils.HttpResult httpResult = HttpUtils.doPost(troControlWebUrl + PUSH_MIDDLEWARE_URL,
                            finalBody);
                    if (httpResult.isSuccess()) {
                        LOGGER.info(String.format("中间件信息上报成功,body:%s,返回结果：%s", finalBody, httpResult.getResult()));
                    } else {
                        LOGGER.info(String.format("中间件信息上报失败,body:%s,失败信息：%s", finalBody, httpResult.getResult()));
                    }
                }
            });
        } catch (Exception e) {
            LOGGER.error(String.format("中间件信息上报异常。body:%s", body), e);
        }
    }

    private class MiddlewareRequest {
        private final String artifactId;
        private final String groupId;
        private final String version;

        public MiddlewareRequest(String artifactId, String groupId, String version) {
            this.artifactId = artifactId;
            this.groupId = groupId;
            this.version = version;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getVersion() {
            return version;
        }
    }

    private class PushMiddlewareVO {
        private final String applicationName;
        private final List<MiddlewareRequest> middlewareList;

        public PushMiddlewareVO(String applicationName,
                                List<MiddlewareRequest> middlewareList) {
            this.applicationName = applicationName;
            this.middlewareList = middlewareList;
        }

        public String getApplicationName() {
            return applicationName;
        }

        public List<MiddlewareRequest> getMiddlewareList() {
            return middlewareList;
        }
    }
}
