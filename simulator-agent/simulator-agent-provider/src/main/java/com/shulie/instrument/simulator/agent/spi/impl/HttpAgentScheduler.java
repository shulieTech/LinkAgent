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
import com.shulie.instrument.simulator.agent.api.model.CommandExecuteResponse;
import com.shulie.instrument.simulator.agent.api.model.CommandPacket;
import com.shulie.instrument.simulator.agent.api.model.HeartRequest;
import com.shulie.instrument.simulator.agent.spi.AgentScheduler;
import com.shulie.instrument.simulator.agent.spi.CommandExecutor;
import com.shulie.instrument.simulator.agent.spi.command.impl.LoadModuleCommand;
import com.shulie.instrument.simulator.agent.spi.command.impl.StartCommand;
import com.shulie.instrument.simulator.agent.spi.command.impl.StopCommand;
import com.shulie.instrument.simulator.agent.spi.command.impl.UnloadModuleCommand;
import com.shulie.instrument.simulator.agent.spi.config.AgentConfig;
import com.shulie.instrument.simulator.agent.spi.config.SchedulerArgs;
import com.shulie.instrument.simulator.agent.spi.impl.model.UpgradeBatchConfig;
import com.shulie.instrument.simulator.agent.spi.impl.utils.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.*;
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
     * 在线升级的指令，只有这个指令由当前agent操作
     */
    private final long onlineUpgradeCommandId = 110000;

    /**
     * 一批指令中包涵这个和升级的指令，需要优先处理这个指令，忽略升级指令
     */
    private final long checkStorageCommandId = 100100;

    private UpgradeBatchConfig upgradeBatchConfig = new UpgradeBatchConfig();


    private ExecutorService commandExecutorService = new ThreadPoolExecutor(1, 3,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(10), new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("command-executor-" + t.getId());
            return t;
        }
    });


    private Map<CommandExecuteKey, FutureTask<CommandExecuteResponse>> futureTaskMap = new ConcurrentHashMap<CommandExecuteKey, FutureTask<CommandExecuteResponse>>(16, 1);

    /**
     * 是否是初始化 simulator
     */
    private AtomicBoolean isInit = new AtomicBoolean(false);

    public HttpAgentScheduler() {
    }

    private Result uninstallModule(CommandPacket commandPacket) {
        Map<String, String> extras = commandPacket.getExtras();
        if (extras == null || extras.isEmpty()) {
            return Result.errorResult("未找到指定的模块[moduleId 为空]");
        }
        String moduleId = extras.get("moduleId");
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
        Map<String, String> extras = commandPacket.getExtras();
        if (extras == null || extras.isEmpty()) {
            return Result.errorResult("未找到指定的模块[moduleId 为空]");
        }
        String moduleId = extras.get("moduleId");
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
            if (StringUtils.isBlank(commandPacket.getDataPath()) && !commandPacket.isUseLocal()) {
                throw new RuntimeException("missing agent download path from command packet!");
            }
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
            installLocal();
        }
        startScheduler();
    }

    /**
     * 检查一遍在运行任务的结果
     * @return
     */
    private List<CommandExecuteResponse> getCommandExecuteResponses(){
        if (futureTaskMap.isEmpty()){
            return Collections.EMPTY_LIST;
        }
        List<CommandExecuteResponse> commandExecuteResponseList = new ArrayList<CommandExecuteResponse>(futureTaskMap.size());
        List<CommandExecuteKey> failedCommandIds = new ArrayList<CommandExecuteKey>(futureTaskMap.size());
        for (Map.Entry<CommandExecuteKey, FutureTask<CommandExecuteResponse>> entry : futureTaskMap.entrySet()){
            boolean success = false;
            String errorMsg = null;
            if (entry.getValue().isDone()){
                try {
                    commandExecuteResponseList.add(entry.getValue().get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    success = true;
                    errorMsg = e.getMessage();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    success = true;
                    errorMsg = e.getMessage();
                } finally {
                    if (success){
                        CommandExecuteResponse commandExecuteResponse = new CommandExecuteResponse();
                        commandExecuteResponse.setCommandId(entry.getKey().getCommandId());
                        commandExecuteResponse.setTaskId(entry.getKey().getTaskId());
                        commandExecuteResponse.setResult(false);
                        commandExecuteResponse.setMsg(errorMsg);
                        commandExecuteResponseList.add(commandExecuteResponse);
                        entry.getValue().cancel(true);
                        failedCommandIds.add(entry.getKey());
                    }
                }
            }
        }
        //失败的直接去除掉
        if (!failedCommandIds.isEmpty()){
            for (CommandExecuteKey id : failedCommandIds){
                futureTaskMap.remove(id);
            }
        }
        return commandExecuteResponseList;
    }




    /**
     * 上报心跳，获取指令
     * @return
     */
    private CommandPacket getCommandPacketByHeart(){
        List<CommandExecuteResponse> commandExecuteResponseList = getCommandExecuteResponses();
        HeartRequest heartRequest = new HeartRequest();
        heartRequest.setCommandExecuteResponseList(commandExecuteResponseList);
        List<CommandPacket> commandPacketList = externalAPI.sendHeart(heartRequest);
        if (commandPacketList == null || commandPacketList.isEmpty()) {
            return null;
        }

        if (commandPacketList.size() > 1){
            Map<Long, CommandPacket> commandPacketMap = new HashMap<Long, CommandPacket>(commandPacketList.size(), 1);
            for (CommandPacket commandPacket : commandPacketList){
                commandPacketMap.put(commandPacket.getId(), commandPacket);
            }
            if (commandPacketMap.containsKey(onlineUpgradeCommandId) &&
                commandPacketMap.containsKey(checkStorageCommandId)){
                commandPacketMap.remove(onlineUpgradeCommandId);
                commandPacketList = (List<CommandPacket>) commandPacketMap.values();
            }
        }

        for (CommandPacket commandPacket : commandPacketList){

        }
        return null;
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
    private void installLocal() {
        CommandPacket commandPacket = new CommandPacket();
        commandPacket.setCommandType(CommandPacket.COMMAND_TYPE_FRAMEWORK);
        commandPacket.setOperateType(CommandPacket.OPERATE_TYPE_INSTALL);
        commandPacket.setUseLocal(true);
        install(commandPacket);
    }

    private void startScheduler() {
        this.scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                while (!isShutdown) {
                    try {
                        /**
                         * 获取最新的命令包
                         */
                        CommandPacket commandPacket = getLatestCommandPacket();
                        if (commandPacket == null) {
                            continue;
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
