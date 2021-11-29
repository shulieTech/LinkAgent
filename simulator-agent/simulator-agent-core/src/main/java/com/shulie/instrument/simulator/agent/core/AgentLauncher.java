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
package com.shulie.instrument.simulator.agent.core;

import com.alibaba.fastjson.JSON;
import com.shulie.instrument.simulator.agent.api.model.CommandPacket;
import com.shulie.instrument.simulator.agent.api.utils.HeartCommandConstants;
import com.shulie.instrument.simulator.agent.core.classloader.FrameworkClassLoader;
import com.shulie.instrument.simulator.agent.core.register.AgentStatus;
import com.shulie.instrument.simulator.agent.core.response.Response;
import com.shulie.instrument.simulator.agent.core.util.HttpUtils;
import com.shulie.instrument.simulator.agent.core.util.PidUtils;
import com.shulie.instrument.simulator.agent.core.util.ThrowableUtils;
import com.shulie.instrument.simulator.agent.spi.command.impl.*;
import com.shulie.instrument.simulator.agent.spi.config.AgentConfig;
import com.shulie.instrument.simulator.agent.spi.model.CommandExecuteResponse;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Agent启动器
 *
 * @author xiaobin@shulie.io
 * @since 1.0.0
 */
public class AgentLauncher {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final static int START_MODE_ATTACH = 1;
    private final static int START_MODE_PREMAIN = 2;
    private final static int MAX_INSTALL_WAIT_TIME = 1000 * 60 * 5;

    /**
     * 目标应用进程的描述,可以是 pid 也可以是进程名称的模糊匹配
     */
    private String descriptor;

    /**
     * agent 的 baseUrl
     */
    private String baseUrl;

    /**
     * agent 的 home 目录
     */
    private final AgentConfig agentConfig;

    /**
     * 是否是运行中状态
     */
    private AtomicBoolean isRunning = new AtomicBoolean(false);

    private Instrumentation instrumentation;
    private ClassLoader parent;
    private boolean usePremain;

    private int startMode = START_MODE_ATTACH;

    private FrameworkClassLoader frameworkClassLoader;

    public AgentLauncher(final AgentConfig agentConfig, final Instrumentation instrumentation,
                         final ClassLoader parent) {
        this.agentConfig = agentConfig;
        this.instrumentation = instrumentation;
        this.parent = parent;
        if (this.agentConfig.getAttachId() != -1) {
            this.descriptor = String.valueOf(this.agentConfig.getAttachId());
        } else if (this.agentConfig.getAttachName() != null) {
            this.descriptor = this.agentConfig.getAttachName();
        } else {
            this.descriptor = System.getProperty("attach.pid");
            if (StringUtils.isBlank(descriptor)) {
                this.descriptor = System.getProperty("attach.name");
            }
        }
        this.usePremain = agentConfig.getBooleanProperty("simulator.use.premain", false);
    }

    /**
     * 判断是否是数字
     *
     * @param str
     * @return
     */
    private static boolean isDigits(final String str) {
        if (StringUtils.isEmpty(str)) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 内部启动 agent
     *
     * @param descriptor   agent 描述,可以是 attach 的目标 pid，也可以是目标的进程名称
     * @param agentJarPath agent 所在的 路径
     * @param config
     */
    private void start0(final String descriptor,
                        final String agentJarPath,
                        final String config) throws Throwable {
        if (logger.isDebugEnabled()) {
            logger.debug("AGENT: prepare to attach agent: descriptor={}, agentJarPath={}, config={}", descriptor,
                    agentJarPath, config);
        }
        //针对docker pid小于等于5的使用premain方式
        //如果是 main 方法执行， 强制使用 premain 模式
        if (usePremain || PidUtils.getPid() <= 5 || "main".equals(Thread.currentThread().getName())) {
            startWithPremain(agentJarPath, config);
            startMode = START_MODE_PREMAIN;
            logger.info("AGENT: simulator with premain mode start successful.");
            return;
        }

        try {
            VirtualMachineDescriptor virtualMachineDescriptor = null;
            String targetJvmPid = descriptor;

            if (isDigits(descriptor)) {
                for (VirtualMachineDescriptor vmDescriptor : VirtualMachine.list()) {
                    if (vmDescriptor.id().equals(descriptor)) {
                        virtualMachineDescriptor = vmDescriptor;
                        break;
                    }
                }
            }

            if (virtualMachineDescriptor == null) {
                for (VirtualMachineDescriptor vmDescriptor : VirtualMachine.list()) {
                    if (vmDescriptor.displayName().contains(descriptor)) {
                        virtualMachineDescriptor = vmDescriptor;
                        break;
                    }
                }
            }

            if (virtualMachineDescriptor != null) {
                // 加载agent
                attachAgent(virtualMachineDescriptor, targetJvmPid, agentJarPath, config);
            } else {
                logger.warn("AGENT: can't found attach target: {}", descriptor);
            }
            startMode = START_MODE_ATTACH;
            logger.info("AGENT: simulator with attach mode start successful.");
        } catch (UnsatisfiedLinkError e) {
            logger.warn("AGENT: attach model fail,try premain model", e);
            startWithPremain(agentJarPath, config);
            startMode = START_MODE_PREMAIN;
            logger.info("AGENT: simulator with premain mode start successful.");
        }
    }

    private static String encodeArg(String arg) {
        try {
            return URLEncoder.encode(arg, "utf-8");
        } catch (UnsupportedEncodingException e) {
            return arg;
        }
    }

    /**
     * 启动agent，返回 agent 访问地址
     *
     * @param startCommand 启动命令
     * @return
     * @throws Throwable 如果启动出现问题可能会抛出异常
     */
    public void startup(StartCommand<CommandPacket> startCommand) throws Throwable {
        AgentStatus.installing();
        try {
            if (HeartCommandConstants.startCommandId != startCommand.getPacket().getId() ){
                throw new IllegalArgumentException("startCommand commandId is wrong " + startCommand.getPacket().getId());
            }

            if (!HeartCommandConstants.PATH_TYPE_LOCAL_VALUE.equals(startCommand.getPacket().getExtras().get(HeartCommandConstants.PATH_TYPE_KEY))){
                //TODO
//                OssOperationClient ossOperationClient
//                FtpOperationClient ftpOperationClient
            }

            if (!isRunning.compareAndSet(false, true)) {
                return;
            }
            if (logger.isInfoEnabled()) {
                logger.info("prepare to startup agent.");
            }

            this.baseUrl = null;
            StringBuilder builder = new StringBuilder();
            if (StringUtils.isNotBlank(this.agentConfig.getAppName())) {
                builder.append(";app.name=").append(encodeArg(this.agentConfig.getAppName()));
            }
            if (StringUtils.isNotBlank(this.agentConfig.getAgentId())) {
                builder.append(";agentId=").append(encodeArg(this.agentConfig.getAgentId()));
            }
            if (StringUtils.isNotBlank(this.agentConfig.getLogPath())) {
                builder.append(";logPath=").append(encodeArg(this.agentConfig.getLogPath()));
            }
            if (StringUtils.isNotBlank(this.agentConfig.getLogLevel())) {
                builder.append(";logLevel=").append(encodeArg(this.agentConfig.getLogLevel()));
            }
            if (StringUtils.isNotBlank(this.agentConfig.getZkServers())) {
                builder.append(";zkServers=").append(encodeArg(this.agentConfig.getZkServers()));
            }
            if (StringUtils.isNotBlank(this.agentConfig.getRegisterPath())) {
                builder.append(";registerPath=").append(encodeArg(this.agentConfig.getRegisterPath()));
            }
            if (StringUtils.isNotBlank(this.agentConfig.getConfigFilePath())) {
                builder.append(";agentConfigPath=").append(encodeArg(this.agentConfig.getConfigFilePath()));
            }
            builder.append(";zkConnectionTimeout=").append(this.agentConfig.getZkConnectionTimeout());
            builder.append(";zkSessionTimeout=").append(this.agentConfig.getZkSessionTimeout());
            builder.append(";agentVersion=").append(this.agentConfig.getAgentVersion());
            builder.append(";troWebUrl=").append(this.agentConfig.getTroWebUrl());
            builder.append(";userAppKey=").append(this.agentConfig.getUserAppKey());
            builder.append(";userId=").append(this.agentConfig.getUserId());
            /**
             * 指定simulator配置文件的获取地址
             */
            final String simulatorConfigUrl = agentConfig.getProperty("simulator.config.url", null);
            if (StringUtils.isNotBlank(simulatorConfigUrl)) {
                builder.append(";prop=").append(encodeArg(simulatorConfigUrl));
            }

            start0(descriptor, agentConfig.getAgentJarPath(), builder.toString());
            String content = null;
            long startWaitTime = System.currentTimeMillis();
            while (true) {
                File resultFile = new File(this.agentConfig.getAgentResultFilePath());
                if (!resultFile.exists()) {
                    if (System.currentTimeMillis() - startWaitTime > MAX_INSTALL_WAIT_TIME) {
                        logger.error("AGENT: launch on agent err. wait simulator install time out!");
                        AgentStatus.uninstall();
                        throw new RuntimeException("AGENT: launch on agent err. wait simulator install time out!");
                    }
                    Thread.sleep(100);
                    continue;
                }
                content = read(resultFile);
                break;
            }
            if (StringUtils.isBlank(content)) {
                logger.error("AGENT: launch on agent err. can't get a empty result from result file:{}",
                        this.agentConfig.getAgentResultFilePath());
                AgentStatus.uninstall();
                throw new RuntimeException(
                        "AGENT: launch on agent err. can't get a empty result from result file:" + this.agentConfig
                                .getAgentResultFilePath());
            }
            String[] result = StringUtils.split(content, ';');

            if (ArrayUtils.isEmpty(result) || result.length < 3) {
                logger.error("AGENT: launch on agent err. can't get a correct result from result file [{}] : {}",
                        content, this.agentConfig.getAgentResultFilePath());
                AgentStatus.uninstall();
                throw new RuntimeException(
                        "AGENT: launch on agent err. can't get a correct result from result file [" + content + "] :"
                                + this.agentConfig.getAgentResultFilePath());
            }
            AgentStatus.installed(result[2]);
            this.baseUrl = "http://" + result[0] + ":" + result[1] + "/simulator";

            logger.info("AGENT: got a available agent url: {} version : {}", this.baseUrl, result[2]);
            System.setProperty("ttl.disabled", "false");
        } catch (Throwable throwable) {
            try {
                if (throwable instanceof NoClassDefFoundError) {
                    NoClassDefFoundError e = (NoClassDefFoundError) throwable;
                    if (e.getMessage().contains("com/sun/tools/attach/VirtualMachine")) {
                        logger.error(
                                "add java start params : -Djdk.attach.allowAttachSelf=true "
                                        + "-Xbootclasspath/a:$JAVA_HOME/lib/tools.jar to fix this error");
                    }
                }
            } catch (Throwable e) {
                logger.error("", e);
            }
            isRunning.set(false);
            AgentStatus.uninstall();
            logger.error("AGENT: agent startup failed.", throwable);
            throw throwable;
        }
    }


    public CommandExecuteResponse commandModule(HeartCommand<CommandPacket> heartCommand){
        CommandExecuteResponse commandExecuteResponse = new CommandExecuteResponse();
        commandExecuteResponse.setCommandId(heartCommand.getPacket().getId());
        commandExecuteResponse.setTaskId(heartCommand.getPacket().getUuid());
        String moduleId = heartCommand.getPacket().getExtra(HeartCommandConstants.MODULE_ID_KEY);
        String moduleMethod = heartCommand.getPacket().getExtra(HeartCommandConstants.MODULE_METHOD_KEY);
        String sync = heartCommand.getPacket().getExtra(HeartCommandConstants.MODULE_EXECUTE_COMMAND_TASK_SYNC_KEY);
        String requests = heartCommand.getPacket().getExtra(HeartCommandConstants.REQUEST_COMMAND_TASK_RESULT_KEY);
        String commandId = heartCommand.getPacket().getExtra(HeartCommandConstants.COMMAND_ID_KEY);
        String taskId = heartCommand.getPacket().getExtra(HeartCommandConstants.TASK_ID_KEY);
        if (StringUtils.isBlank(moduleId) || StringUtils.isBlank(moduleId) || StringUtils.isBlank(sync)
            || StringUtils.isBlank(commandId) || StringUtils.isBlank(taskId)){
            throw new IllegalArgumentException("command 参数不完整!");
        }
        if (logger.isInfoEnabled()) {
            logger.info("prepare to load module from path={}.", moduleId);
        }
        try {
            String loadUrl = baseUrl + File.separator + moduleId + File.separator + moduleMethod + "?useApi=true&path="
                    + moduleId + "&extrasString=" + heartCommand.getPacket().getExtrasString() + "&sync=" + sync
                    + "&commandId=" + commandId + "&taskId=" + taskId;
            if (StringUtils.isNotBlank(requests)){
                loadUrl = loadUrl + "&requests=" + requests;
            }

            HttpUtils.HttpResult content = HttpUtils.doPost(loadUrl, agentConfig.getUserAppKey(), heartCommand.getPacket().getExtrasString());

            if (content == null) {
                commandExecuteResponse.setSuccess(false);
                commandExecuteResponse.setResult(null);
                commandExecuteResponse.setMsg("请求模块ID:" + moduleId + "数据返回null");
                return commandExecuteResponse;
            }

            Response response = JSON.parseObject(content.getResult(), Response.class);

            if (logger.isInfoEnabled()) {
                logger.info("commandModule successful from path={}.", moduleId);
            }
            commandExecuteResponse.setSuccess(response.isSuccess());
            commandExecuteResponse.setResult(response.getResult());
            commandExecuteResponse.setMsg(response.getMessage());
            return commandExecuteResponse;
        } catch (Throwable e) {
            logger.error("AGENT: commandModule failed.", e);
            commandExecuteResponse.setResult(e.getMessage());
            commandExecuteResponse.setSuccess(false);
            return commandExecuteResponse;
        }

    }

    /**
     * 启动模块
     *
     * @param command
     * @throws Throwable
     */
    public void loadModule(LoadModuleCommand<CommandPacket> command) throws Throwable {
        String moduleId = command.getPacket().getExtra("moduleId");
        if (logger.isInfoEnabled()) {
            logger.info("prepare to load module from path={}.", moduleId);
        }
        try {
            String loadUrl = baseUrl + File.separator + "management" + File.separator + "load??useApi=true&path="
                    + moduleId;
            String content = HttpUtils.doGet(loadUrl, agentConfig.getUserAppKey());
            /**
             * 如果返回为空则视为已经停止
             */
            if (content == null) {
                AgentStatus.setError(
                        "AGENT: unload module err. got empty content from unload api url, path=" + moduleId);
                throw new RuntimeException(
                        "AGENT: unload module err. got empty content from unload api url, path=" + moduleId);
            }

            Response response = JSON.parseObject(content, Response.class);
            if (response.isSuccess()) {
                if (logger.isInfoEnabled()) {
                    logger.info("load module successful from path={}.", moduleId);
                }
                return;
            }

            AgentStatus.setError(
                    "AGENT: load module failed. load module got a error response from agent. " + response.getMessage());
            throw new RuntimeException(
                    "AGENT: load module failed. load module got a error response from agent. " + response.getMessage());
        } catch (Throwable e) {
            String errorMessage = ThrowableUtils.toString(e, 1000);
            AgentStatus.setError("AGENT: agent shutdown failed. " + errorMessage);
            logger.error("AGENT: agent shutdown failed.", e);
            throw e;
        }
    }

    public void unloadModule(UnloadModuleCommand<CommandPacket> command) throws Throwable {
        String moduleId = command.getPacket().getExtra("moduleId");
        if (logger.isInfoEnabled()) {
            logger.info("prepare to unload module {}.", moduleId);
        }
        try {
            String loadUrl = baseUrl + File.separator + "management" + File.separator + "unload??useApi=true&moduleId="
                    + moduleId;
            String content = HttpUtils.doGet(loadUrl, agentConfig.getUserAppKey());
            /**
             * 如果返回为空则视为已经停止
             */
            if (content == null) {
                AgentStatus.setError(
                        "AGENT: unload module err. got empty content from unload api url, moduleId=" + moduleId);
                throw new RuntimeException(
                        "AGENT: unload module err. got empty content from unload api url, moduleId=" + moduleId);
            }

            Response response = JSON.parseObject(content, Response.class);
            if (response.isSuccess()) {
                if (logger.isInfoEnabled()) {
                    logger.info("unload module successful {}.", moduleId);
                }
                return;
            }

            AgentStatus.setError(
                    "AGENT: unload moudule failed. unload moudule got a error response from agent. " + response
                            .getMessage());
            throw new RuntimeException(
                    "AGENT: unload moudule failed. unload moudule got a error response from agent. " + response
                            .getMessage());
        } catch (Throwable e) {
            String errorMessage = ThrowableUtils.toString(e, 1000);
            AgentStatus.setError("AGENT: unload module failed. " + errorMessage);
            logger.error("AGENT: unload module failed.", e);
            throw e;
        }
    }

    public void reloadModule(ReloadModuleCommand<CommandPacket> command) throws Throwable {
        final String moduleId = command.getPacket().getExtra("moduleId");
        if (logger.isInfoEnabled()) {
            logger.info("prepare to reload module {}.", moduleId);
        }
        try {
            String loadUrl = baseUrl + File.separator + "management" + File.separator + "reload??useApi=true&moduleId="
                    + moduleId;
            String content = HttpUtils.doGet(loadUrl, agentConfig.getUserAppKey());
            /**
             * 如果返回为空则视为已经停止
             */
            if (content == null) {
                AgentStatus.setError(
                        "AGENT: reload module err. got empty content from reload api url, moduleId=" + moduleId);
                throw new RuntimeException(
                        "AGENT: reload module err. got empty content from reload api url, moduleId=" + moduleId);
            }

            Response response = JSON.parseObject(content, Response.class);
            if (response.isSuccess()) {
                if (logger.isInfoEnabled()) {
                    logger.info("reload module successful {}.", moduleId);
                }
                return;
            }
            AgentStatus.setError(
                    "AGENT: reload module failed. reload module got a error response from agent. moduleId=" + moduleId
                            + ", loadUrl=" + loadUrl + " " + response.getMessage());
            throw new RuntimeException(
                    "AGENT: reload module failed. reload module got a error response from agent. moduleId=" + moduleId
                            + ", loadUrl=" + loadUrl + " " + response.getMessage());
        } catch (Throwable e) {
            String errorMessage = ThrowableUtils.toString(e, 1000);
            AgentStatus.setError("AGENT: reload module failed. " + errorMessage);
            logger.error("AGENT: reload module failed.", e);
            throw e;
        }
    }

    public boolean isInstalled() {
        if (this.baseUrl == null) {
            return false;
        }
        String heartbeatResult = HttpUtils.doGet(baseUrl + File.separator + "heartbeat?useApi=true",
                agentConfig.getUserAppKey());
        if (heartbeatResult == null) {
            if (logger.isInfoEnabled()) {
                logger.info("shutdown agent successful.");
            }
            return false;
        }
        return true;
    }

    /**
     * 返回模块是否已经安装
     *
     * @param moduleId 模块 ID
     * @return
     */
    public boolean isModuleInstalled(String moduleId) {
        return false;
    }

    /**
     * 停止 agent
     */
    public void shutdown(StopCommand<CommandPacket> command) throws Throwable {
        if (!isRunning.compareAndSet(true, false)) {
            System.setProperty("ttl.disabled", "true");
            return;
        }

        if (logger.isInfoEnabled()) {
            logger.info("prepare to shutdown agent.");
        }

        if (this.baseUrl == null) {
            logger.error("AGENT: agent shutdown failed. agent access url is blank.");
            AgentStatus.installed(AgentStatus.getSimulatorVersion());
            throw new RuntimeException("AGENT: agent shutdown failed. agent access url is blank.");
        }
        try {
            /**
             * 如果已经检测不到，则说明已经被关闭了
             */
            String heartbeatResult = HttpUtils.doGet(baseUrl + File.separator + "heartbeat?useApi=true",
                    agentConfig.getUserAppKey());
            if (heartbeatResult == null) {
                shutdownWithPremain();
                if (logger.isInfoEnabled()) {
                    logger.info("shutdown agent successful.");
                }
                return;
            }

            AgentStatus.uninstalling();
            String shutdownUrl = baseUrl + File.separator + "control" + File.separator + "shutdown?useApi=true";
            String content = HttpUtils.doGet(shutdownUrl, agentConfig.getUserAppKey());

            /**
             * 如果返回为空则视为已经停止
             */
            if (content == null) {
                shutdownWithPremain();
                if (logger.isInfoEnabled()) {
                    logger.info("shutdown agent successful.");
                }
                AgentStatus.uninstall();
                return;
            }

            Response response = JSON.parseObject(content, Response.class);
            if (response.isSuccess()) {
                while (true) {
                    /**
                     * 如果没有响应，则说明关闭成功了
                     */
                    heartbeatResult = HttpUtils.doGet(baseUrl + File.separator + "heartbeat?useApi=true",
                            agentConfig.getUserAppKey());
                    if (heartbeatResult == null) {
                        shutdownWithPremain();
                        if (logger.isInfoEnabled()) {
                            logger.info("shutdown agent successful.");
                        }
                        /**
                         * 重新将 url 置空
                         */
                        this.baseUrl = null;
                        AgentStatus.uninstall();
                        System.setProperty("ttl.disabled", "true");
                        return;
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                }
            }
            throw new RuntimeException(
                    "AGENT: got a error response from agent. shutdown agent failed.  " + response.getMessage());
        } catch (Throwable e) {
            isRunning.set(true);
            AgentStatus.installed(AgentStatus.getSimulatorVersion());
            logger.error("AGENT: agent shutdown failed.", e);
            throw e;
        }
    }

    private static String read(File file) throws Throwable {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            return reader.readLine();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
    }

    // 加载Agent
    private void attachAgent(final VirtualMachineDescriptor virtualMachineDescriptor,
                             final String targetJvmPid,
                             final String agentJarPath,
                             final String config) throws Throwable {

        VirtualMachine vmObj = null;
        try {
            if (virtualMachineDescriptor != null) {
                vmObj = VirtualMachine.attach(virtualMachineDescriptor);
            } else {
                if (!isDigits(targetJvmPid)) {
                    logger.error(
                            "AGENT: illegal args[0], can't found a vm instance with {} by name. and it is also not a "
                                    + "valid digits. agentJarPath={}, config={}",
                            targetJvmPid, agentJarPath, config);
                    throw new IllegalArgumentException("illegal args[0], can't found a vm instance with " + targetJvmPid
                            + " by name. and it is also not a valid digits.");
                }
                vmObj = VirtualMachine.attach(targetJvmPid);
            }
            if (vmObj != null) {
                try {
                    vmObj.loadAgent(agentJarPath, config);
                    logger.info("AGENT: attached to agent success. descriptor={}, agentJarPath={}, config={}",
                            targetJvmPid, agentJarPath, config);
                } catch (Throwable e) {
                    logger.error(
                            "AGENT: attach failed. can't found attach target agent.  descriptor={}, agentJarPath={}, "
                                    + "config={}",
                            targetJvmPid, agentJarPath, config);
                    throw e;
                }
            } else {
                logger.error(
                        "AGENT: attach failed. can't found attach target agent.  descriptor={}, agentJarPath={}, config={}",
                        targetJvmPid, agentJarPath, config);
            }
        } finally {
            if (null != vmObj) {
                vmObj.detach();
            }
        }

    }

    /**
     * 使用 premain 方式启动
     *
     * @param agentJarPath
     * @param config
     * @throws MalformedURLException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private void startWithPremain(String agentJarPath, String config)
            throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        this.frameworkClassLoader = new FrameworkClassLoader(new URL[]{new File(agentJarPath).toURI().toURL()},
                parent);
        Class classOfAgentLauncher = frameworkClassLoader.loadClass(
                "com.shulie.instrument.simulator.agent.AgentLauncher");
        Method premainMethod = classOfAgentLauncher.getDeclaredMethod("premain", String.class, Instrumentation.class);
        premainMethod.setAccessible(true);
        premainMethod.invoke(null, config, instrumentation);
    }

    private void shutdownWithPremain() {
        if (!usePremain) {
            return;
        }
        if (this.frameworkClassLoader == null) {
            return;
        }
        this.frameworkClassLoader.closeIfPossible();
        this.frameworkClassLoader = null;
    }
}
