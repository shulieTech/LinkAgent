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
package com.shulie.instrument.simulator.agent.spi.impl;

import com.shulie.instrument.simulator.agent.api.ExternalAPI;
import com.shulie.instrument.simulator.agent.api.model.CommandExecuteKey;
import com.shulie.instrument.simulator.agent.api.model.CommandPacket;
import com.shulie.instrument.simulator.agent.api.model.HeartRequest;
import com.shulie.instrument.simulator.agent.api.utils.HeartCommandConstants;
import com.shulie.instrument.simulator.agent.api.utils.HeartCommandUtils;
import com.shulie.instrument.simulator.agent.spi.AgentScheduler;
import com.shulie.instrument.simulator.agent.spi.CommandExecutor;
import com.shulie.instrument.simulator.agent.spi.command.impl.*;
import com.shulie.instrument.simulator.agent.spi.config.AgentConfig;
import com.shulie.instrument.simulator.agent.spi.config.SchedulerArgs;
import com.shulie.instrument.simulator.agent.spi.impl.utils.FileUtils;
import com.shulie.instrument.simulator.agent.spi.impl.utils.SimulatorStatus;
import com.shulie.instrument.simulator.agent.spi.model.CommandExecuteResponse;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 http 模式的 agent 调试器，负责 agent 加载、卸载、升级等一系列操作的调度工作
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/17 8:07 下午
 */
public class HttpAgentScheduler implements AgentScheduler {
    private Logger logger = LoggerFactory.getLogger(HttpAgentScheduler.class);

    @Resource
    private ExternalAPI externalAPI;

    /**
     * agent 配置
     */
    private AgentConfig agentConfig;

    /**
     * 命令执行器
     */
    private CommandExecutor commandExecutor;

    /**
     * 调度参数
     */
    private SchedulerArgs schedulerArgs;

    /**
     * 调度线程池
     */
    private ScheduledExecutorService scheduledExecutorService;

    private boolean isShutdown;

    /**
     * 已经执行的 commandId,只有新的 commandId 比当前的大才会执行
     */
    private long executeCommandId;





    /**
     * 是否是初始化 simulator
     */
    private AtomicBoolean isInit = new AtomicBoolean(false);

    public HttpAgentScheduler() {
    }

    private Result uninstallModule(CommandPacket commandPacket) {
        Map<String, Object> extras = commandPacket.getExtras();
        if (extras == null || extras.isEmpty()) {
            return Result.errorResult("未找到指定的模块[moduleId 为空]");
        }
        String moduleId = (String) extras.get("moduleId");
        if (StringUtils.isBlank(moduleId)) {
            return Result.errorResult("卸载的模块 ID 为空");
        }
        if (commandExecutor.isModuleInstalled(moduleId)) {
            try {
                commandExecutor.execute(new UnloadModuleCommand(moduleId));
            } catch (Throwable throwable) {
                logger.error("execute module uninstall error. moduleId={}", moduleId, throwable);
                return Result.errorResult("因为模块已经加载，执行卸载模块操作失败." + throwable.getMessage());
            }
        }
        return Result.SUCCESS;
    }

    /**
     * 卸载框架
     */
    private Result uninstall(CommandPacket commandPacket) {
        try {
            if (!commandExecutor.isInstalled()) {
                return Result.SUCCESS;
            }
            commandExecutor.execute(new StopCommand(commandPacket));
            logger.info("AGENT: simulator uninstall successful! ");
            return Result.SUCCESS;
        } catch (Throwable e) {
            logger.error("AGENT: shutdown agent err. ", e);
            return Result.errorResult(e);
        }
    }

    @Override
    public void init(SchedulerArgs schedulerArgs) {
        this.schedulerArgs = schedulerArgs;
        this.scheduledExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "Agent-Scheduler-Service");
                t.setDaemon(true);
                t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        logger.error("Agent-Scheduler-Service caught a unknow error.", e);
                    }
                });
                return t;
            }
        });
    }

    @Override
    public void setAgentConfig(AgentConfig agentConfig) {
        this.agentConfig = agentConfig;
    }

    @Override
    public void setCommandExecutor(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    /**
     * 加载模块
     *
     * @param commandPacket
     * @return
     */
    private Result installModule(CommandPacket commandPacket) {
        Map<String, Object> extras = commandPacket.getExtras();
        if (extras == null || extras.isEmpty()) {
            return Result.errorResult("未找到指定的模块[moduleId 为空]");
        }
        String moduleId = (String) extras.get("moduleId");
        if (StringUtils.isBlank(moduleId)) {
            return Result.errorResult("升级的模块 ID 为空");
        }
        try {
            if (commandExecutor.isModuleInstalled(moduleId)) {
                try {
                    commandExecutor.execute(new UnloadModuleCommand(moduleId));
                } catch (Throwable throwable) {
                    logger.error("execute module uninstall error. moduleId={}", moduleId, throwable);
                    return Result.errorResult("因为模块已经加载，执行卸载模块操作失败." + throwable.getMessage());
                }
            }
            File file = new File(agentConfig.getSimulatorHome(), "simulator");
            File modulesFile = new File(file, "modules");
            if (!modulesFile.exists()) {
                modulesFile.mkdirs();
            }
            File moduleDir = new File(modulesFile, moduleId);
            if (!moduleDir.exists()) {
                moduleDir.mkdirs();
            }
            File f = externalAPI.downloadModule(commandPacket.getDataPath(), moduleDir.getAbsolutePath() + "_tmp");
            if (f != null) {
                if (moduleDir.exists()) {
                    FileUtils.delete(moduleDir);
                }
                f.renameTo(moduleDir);
            }
            try {
                commandExecutor.execute(new LoadModuleCommand(f.getAbsolutePath()));
            } catch (Throwable throwable) {
                logger.error("execute module install error. moduleId={}", moduleId, throwable);
                return Result.errorResult("因为加载模块操作失败." + throwable.getMessage());
            }
            return Result.SUCCESS;
        } catch (Throwable throwable) {
            logger.error("execute module install occur unknow error. moduleId={}", moduleId, throwable);
            return Result.errorResult(throwable);
        }
    }

    /**
     * 安装框架
     */
    private Result install(CommandPacket commandPacket) {
        try {
//            if (StringUtils.isBlank(commandPacket.getDataPath()) && !commandPacket.isUseLocal()) {
//                throw new RuntimeException("missing agent download path from command packet!");
//            }
            if (commandExecutor.isInstalled()) {
                uninstall(commandPacket);
            }
            commandExecutor.execute(new StartCommand(commandPacket));
            logger.info("AGENT: simulator install successful! ");
            return Result.SUCCESS;
        } catch (Throwable t) {
            logger.error("AGENT: prepare to start agent failed.", t);
            return Result.errorResult(t);
        }
    }




    /**
     * 执行命令包
     *
     * @param commandPacket
     */
    private Result executeCommandPacket(CommandPacket commandPacket) throws Throwable {
        /**
         * 如果是历史命令，则直接忽略
         */
        if (commandPacket.getId() <= executeCommandId) {
            return null;
        }
        /**
         * 如果是框架命令
         */
        if (commandPacket.getCommandType() == CommandPacket.COMMAND_TYPE_FRAMEWORK) {
            int operateType = commandPacket.getOperateType();
            switch (operateType) {
                case CommandPacket.OPERATE_TYPE_INSTALL:
                    return install(commandPacket);
                case CommandPacket.OPERATE_TYPE_UNINSTALL:
                    return uninstall(commandPacket);
                case CommandPacket.OPERATE_TYPE_UPGRADE:
                    Result result = uninstall(commandPacket);
                    if (!result.isSuccess) {
                        return result;
                    }
                    return install(commandPacket);
            }
            return Result.errorResult("不支持的框架操作类型:" + operateType);
        } else if (commandPacket.getCommandType() == CommandPacket.COMMAND_TYPE_MODULE) {
            //支持模块单独升级
            int operateType = commandPacket.getOperateType();
            switch (operateType) {
                case CommandPacket.OPERATE_TYPE_INSTALL:
                    return installModule(commandPacket);
                case CommandPacket.OPERATE_TYPE_UNINSTALL:
                    return uninstallModule(commandPacket);
                case CommandPacket.OPERATE_TYPE_UPGRADE:
                    Result result = uninstallModule(commandPacket);
                    if (!result.isSuccess) {
                        return result;
                    }
                    return installModule(commandPacket);

            }
            return Result.errorResult("不支持的模块操作类型:" + operateType);
        } else {
            return Result.errorResult("不支持的命令类型:" + commandPacket.getCommandType());
        }
    }

    /**
     * 上报命令执行状态
     *
     * @param commandId 命令 ID
     * @param isSuccess 是否成功
     */
    private void reportCommandExecuteResult(long commandId, boolean isSuccess, String errorMsg) {
        externalAPI.reportCommandResult(commandId, isSuccess, errorMsg);
    }

    @Override
    public void start() {
        //install local if no latest command packet found
        //卸载的命令在的时候，重启的时候这里这个installLocal会无法执行，
        CommandPacket commandPacket = getLatestCommandPacket();
        if (commandPacket == null) {
            installLocalOrRemote();
        }

        startScheduler();
    }

    private List<CommandExecuteResponse> handleCommandExecuteResponse(CommandExecuteResponse commandExecuteResponse){
        List<CommandExecuteResponse> commandExecuteResponseList = new ArrayList<CommandExecuteResponse>(HeartCommandUtils.futureMapSize());
        //获取上次异步的数据，请求执行结果
        if (commandExecuteResponse.isSuccess()){
            try {
                //说明没有在执行的数据，清空当前的任务列表数据
                if (commandExecuteResponse.getResult() == null){
                    HeartCommandUtils.getFutureMap().clear();
                }
                else if (null != commandExecuteResponse.getResult()){
                    Map<String, Object> map = (Map<String, Object>) commandExecuteResponse.getResult();
                    if (!map.isEmpty()){
                        List<String> successList = (List<String>) map.get("success");
                        List<String> inExecuteList = (List<String>) map.get("inExecute");
                        Map<String, String> failedMap = (Map<String, String>) map.get("failed");

                        for (Map.Entry<CommandExecuteKey, CommandExecuteResponse> entry : HeartCommandUtils.getFutureMap().entrySet()){
                            CommandExecuteResponse response = entry.getValue();
                            if (successList.contains(entry.getKey().toString())){
                                response.setExecuteStatus("finished");
                                response.setMsg(null);
                                response.setSuccess(true);
                                commandExecuteResponseList.add(response);
                            }
                            else if (failedMap.containsKey(entry.getKey().toString())){
                                response.setSuccess(true);
                                response.setExecuteStatus("failed");
                                response.setMsg(failedMap.get(entry.getKey().toString()));
                                commandExecuteResponseList.add(response);
                            } else {//要么是执行中，要么是unknown
                                if (!inExecuteList.contains(entry.getKey().toString())){
                                    response.setSuccess(true);
                                    response.setExecuteStatus("failed");
                                    response.setMsg(failedMap.get(entry.getKey().toString()));
                                    commandExecuteResponseList.add(response);
                                }
                            }
                        }

                    }
                }
            } catch (Throwable throwable) {
                logger.error("getCommandExecuteResponses error", throwable);
                return commandExecuteResponseList;
            }
        }

        return commandExecuteResponseList;
    }

    /**
     * 获取一遍在运行任务的结果进行上报
     * @return
     */
    private CommandExecuteResponse getCommandExecuteResponses(){
        if (HeartCommandUtils.futureMapEmpty()){
            return null;
        }
        //获取上次异步的数据，请求执行结果
        CommandPacket commandPacket = new CommandPacket();
        commandPacket.setId(-2);
        commandPacket.setSync(true);
        commandPacket.setUuid("-2");
        Map<String, Object> extras = new HashMap<String, Object>();
        extras.put(HeartCommandConstants.MODULE_ID_KEY, HeartCommandConstants.MODULE_ID_VALUE_PRADAR_CONFIG_FETCHER);
        extras.put(HeartCommandConstants.MODULE_METHOD_KEY, HeartCommandConstants.MODULE_METHOD_VALUE_PRADAR_CONFIG_FETCHER_GET_COMMAND_RESULT);
        extras.put(HeartCommandConstants.MODULE_EXECUTE_COMMAND_TASK_SYNC_KEY, String.valueOf(commandPacket.isSync()));
        extras.put(HeartCommandConstants.COMMAND_ID_KEY, commandPacket.getId());
        extras.put(HeartCommandConstants.TASK_ID_KEY, commandPacket.getUuid());
        commandPacket.setExtras(extras);
        HeartCommand<CommandPacket> requestResultsCommand = new HeartCommand<CommandPacket>(commandPacket);
        try {
            return commandExecutor.execute(requestResultsCommand);
        } catch (Throwable throwable) {
            return null;
        }
    }



    private CommandPacket getStartCommandPacket(){
        HeartRequest heartRequest = new HeartRequest();
        heartRequest.setCommandResult(Collections.EMPTY_LIST);
        heartRequest.setCurUpgradeBatch(HeartCommandConstants.getCurUpgradeBatch());
        List<CommandPacket> commandPacketList = externalAPI.sendHeart(heartRequest);
        if (null == commandPacketList || commandPacketList.isEmpty()){
            return null;
        }
        if (commandPacketList.get(0).getExtras().get(HeartCommandConstants.UPGRADE_BATCH_KEY) == null){
            logger.error("版本批次号获取失败，无法在线升级操作!");
        } else {
            HeartCommandConstants.setCurUpgradeBatch((String) commandPacketList.get(0).getExtras().get(HeartCommandConstants.UPGRADE_BATCH_KEY));
        }
        return commandPacketList.get(0);
    }

    /**
     * 上报心跳，获取指令
     * @return
     */
    private List<CommandPacket> getCommandPacketByHeart(){
        HeartRequest heartRequest = new HeartRequest();
        //查询一次上次执行任务的执行结果
        CommandExecuteResponse preCommandExecuteResultsResponse = getCommandExecuteResponses();
        List<CommandExecuteResponse> preCommandExecuteResponseList = new ArrayList<CommandExecuteResponse>();
        if (preCommandExecuteResultsResponse != null){
            preCommandExecuteResponseList = handleCommandExecuteResponse(preCommandExecuteResultsResponse);
        }
        if (HeartCommandUtils.futureMapSize() > 30 ||(preCommandExecuteResultsResponse != null && preCommandExecuteResultsResponse.isTaskExceed())){
            heartRequest.setTaskExceed(true);
        }
        heartRequest.setCommandResult(preCommandExecuteResponseList);
        heartRequest.setCurUpgradeBatch(HeartCommandConstants.getCurUpgradeBatch());
        List<CommandPacket> newCommandPacketList = externalAPI.sendHeart(heartRequest);
        if (newCommandPacketList != null){
            //所有已经存在的任务，是成功或者进行中的，不要再下发
            Iterator<CommandPacket> iterator = newCommandPacketList.iterator();
            while (iterator.hasNext()){
                CommandPacket commandPacket = iterator.next();
                CommandExecuteResponse commandExecuteResponse = HeartCommandUtils.getCommandExecuteResponseFuture(commandPacket.getId(), commandPacket.getUuid());
                if (commandExecuteResponse != null && commandExecuteResponse.isSuccess() && !"inExecute".equals(commandExecuteResponse.getExecuteStatus())){
                    iterator.remove();
                }
            }
        }

        //说明交互成功，清楚本地已经失败或者成功的任务，说明已经上报成功了
        if (null != newCommandPacketList && !preCommandExecuteResponseList.isEmpty()){
            for (CommandExecuteResponse commandExecuteResponse : preCommandExecuteResponseList){
                HeartCommandUtils.removeCommandExecuteResponseFuture(commandExecuteResponse.getId(), commandExecuteResponse.getTaskId());
            }
        }
        return newCommandPacketList;
    }


    /**
     * get latest command packet
     *
     * @return
     */
    private CommandPacket getLatestCommandPacket() {
        CommandPacket commandPacket = externalAPI.getLatestCommandPacket();
        if (commandPacket == null || commandPacket == CommandPacket.NO_ACTION_PACKET) {
            return null;
        }
        if (commandPacket.getLiveTime() != -1
                && System.currentTimeMillis() - commandPacket.getCommandTime() > commandPacket
                .getLiveTime()) {
            return null;
        }
        return commandPacket;
    }

    /**
     * use local jar installed if exists
     */
    private void installLocalOrRemote() {
        CommandPacket startCommandPacket;
        //直到成功为止，控制台可能重启、网络问题等原因
        //TODO
        while ((startCommandPacket = getStartCommandPacket()) == null){
            try {
                logger.warn("启动simulator获取远程失败,休眠500ms重试，请确认控制台是否正常");
                Thread.sleep(500 );
            } catch (InterruptedException ignore) {
            }
        }
        install(startCommandPacket);
    }



    private void executeCommand(){
        try {
            //获取simulator状态数据
            if (SimulatorStatus.isUnInstall()){
                CommandPacket commandPacket = new CommandPacket();
                commandPacket.setId(HeartCommandConstants.getSimulatorStatusCommandId);
                commandPacket.setSync(true);
                commandPacket.setUuid("-1");
                Map<String, Object> extras = new HashMap<String, Object>();
                extras.put(HeartCommandConstants.MODULE_ID_KEY, HeartCommandConstants.MODULE_ID_VALUE_PRADAR_REGISTER);
                extras.put(HeartCommandConstants.MODULE_METHOD_KEY, HeartCommandConstants.MODULE_METHOD_VALUE_PRADAR_REGISTER);
                extras.put(HeartCommandConstants.COMMAND_ID_KEY, commandPacket.getId());
                extras.put(HeartCommandConstants.TASK_ID_KEY, commandPacket.getUuid());
                extras.put(HeartCommandConstants.MODULE_EXECUTE_COMMAND_TASK_SYNC_KEY, "true");
                commandPacket.setExtras(extras);
                CommandExecuteResponse commandExecuteResponse = commandExecutor.execute(new HeartCommand(commandPacket));
                if (commandExecuteResponse.isSuccess()){
                    SimulatorStatus.setAgentStatus((String)commandExecuteResponse.getResult(), commandExecuteResponse.getMsg());
                }
            }
            List<CommandPacket> heartCommandPackets = getCommandPacketByHeart();
            boolean asyncTaskResult = true;
            if (null != heartCommandPackets && !heartCommandPackets.isEmpty() && HeartCommandUtils.futureMapSize() < 30){
                for (final CommandPacket commandPacket : heartCommandPackets){
                    commandPacket.getExtras().put(HeartCommandConstants.MODULE_ID_KEY, HeartCommandConstants.MODULE_ID_VALUE_PRADAR_CONFIG_FETCHER);
                    commandPacket.getExtras().put(HeartCommandConstants.MODULE_METHOD_KEY, HeartCommandConstants.MODULE_METHOD_VALUE_PRADAR_CONFIG_FETCHER_DO_COMAAND);
                    commandPacket.getExtras().put(HeartCommandConstants.MODULE_EXECUTE_COMMAND_TASK_SYNC_KEY, String.valueOf(commandPacket.isSync()));
                    commandPacket.getExtras().put(HeartCommandConstants.COMMAND_ID_KEY, commandPacket.getId());
                    commandPacket.getExtras().put(HeartCommandConstants.TASK_ID_KEY, commandPacket.getUuid());
                    //目前同步的只有一个启动的操作
                    if (commandPacket.isSync()){
                        CommandExecuteResponse commandExecuteResponse = commandExecutor.execute(new HeartCommand(commandPacket));
                        HeartCommandUtils.putCommandExecuteResponse(commandPacket.getId(), commandPacket.getUuid(), commandExecuteResponse);
                        //同步失败则直接跳出，所有任务不执行
                        if (!commandExecuteResponse.isSuccess() || "1".equals(commandExecuteResponse.getExecuteStatus())){
                            asyncTaskResult = false;
                            break;
                        } else if (HeartCommandConstants.startCommandId == commandPacket.getId()){
                            HeartCommandUtils.startCommandResult = true;
                        }
                    } else {
                        CommandExecuteResponse commandExecuteResponse = null;
                        try {
                            commandExecuteResponse = commandExecutor.execute(new HeartCommand(commandPacket));
                        } catch (Throwable t){
                            commandExecuteResponse.setId(commandPacket.getId());
                            commandExecuteResponse.setTaskId(commandPacket.getUuid());
                            commandExecuteResponse.setSuccess(false);
                            commandExecuteResponse.setMsg(t.getMessage());
                            commandExecuteResponse.setExecuteStatus("unknown");
                        }
                        commandExecuteResponse.setExtrasString(commandPacket.getExtrasString());
                        HeartCommandUtils.putCommandExecuteResponseFuture(commandPacket.getId(), commandPacket.getUuid(),
                                commandExecuteResponse);
                    }
                }
            }

            //目前只有一个启动的任务，未成功无法启动任何操作
            if (!asyncTaskResult){
                return;
            }

            /**
             * 获取最新的命令包
             */
            final CommandPacket commandPacket = getLatestCommandPacket();
            if (commandPacket == null) {
                return;
            }
            try {
                Result result = executeCommandPacket(commandPacket);
                if (result != null) {
                    executeCommandId = commandPacket.getId();
                    reportCommandExecuteResult(commandPacket.getId(), result.isSuccess, result.errorMsg);
                }
            } catch (Throwable throwable) {
                logger.error("execute command failed. command={}", commandPacket, throwable);
                reportCommandExecuteResult(commandPacket.getId(), false,
                        HttpAgentScheduler.toString(throwable));
            }
        } catch (Throwable t) {
            logger.error("execute scheduler failed. ", t);
        } finally {
            try {
                schedulerArgs.getIntervalUnit().sleep(schedulerArgs.getInterval());
            } catch (InterruptedException e) {
            }
        }
    }

    private void startScheduler() {
        //启动先执行一次
        this.scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                while (!isShutdown) {
                    executeCommand();
                }
            }
        }, Math.max(schedulerArgs.getDelay(), 0), schedulerArgs.getDelayUnit());
    }





    public static String toString(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        StringWriter sw = new StringWriter();
        PrintWriter writer = new PrintWriter(sw);
        try {
            throwable.printStackTrace(writer);
            return sw.toString();
        } finally {
            try {
                sw.close();
            } catch (IOException e) {
            }
            try {
                writer.close();
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void stop() {
        isShutdown = true;
        if (this.scheduledExecutorService != null) {
            this.scheduledExecutorService.shutdown();
        }
    }

    static class Result {
        public final static Result SUCCESS = successResult();
        boolean isSuccess;
        String errorMsg;

        public static Result successResult() {
            Result result = new Result();
            result.isSuccess = true;
            return result;
        }

        public static Result errorResult(String errorMsg) {
            Result result = new Result();
            result.isSuccess = false;
            result.errorMsg = errorMsg;
            return result;
        }

        public static Result errorResult(Throwable throwable) {
            Result result = new Result();
            result.isSuccess = false;
            result.errorMsg = HttpAgentScheduler.toString(throwable);
            return result;
        }
    }
}
