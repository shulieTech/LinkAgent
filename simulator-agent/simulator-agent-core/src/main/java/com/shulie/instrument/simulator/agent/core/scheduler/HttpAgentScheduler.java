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
package com.shulie.instrument.simulator.agent.core.scheduler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.shulie.instrument.simulator.agent.api.ExternalAPI;
import com.shulie.instrument.simulator.agent.api.model.CommandExecuteKey;
import com.shulie.instrument.simulator.agent.api.model.CommandPacket;
import com.shulie.instrument.simulator.agent.api.model.HeartRequest;
import com.shulie.instrument.simulator.agent.api.utils.HeartCommandConstants;
import com.shulie.instrument.simulator.agent.api.utils.HeartCommandUtils;
import com.shulie.instrument.simulator.agent.core.register.AgentStatus;
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
 * ?????? http ????????? agent ?????????????????? agent ?????????????????????????????????????????????????????????
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/17 8:07 ??????
 */
public class HttpAgentScheduler implements AgentScheduler {
    private Logger logger = LoggerFactory.getLogger(HttpAgentScheduler.class);

    @Resource
    private ExternalAPI externalAPI;

    /**
     * agent ??????
     */
    private AgentConfig agentConfig;

    /**
     * ???????????????
     */
    private CommandExecutor commandExecutor;

    /**
     * ????????????
     */
    private SchedulerArgs schedulerArgs;

    /**
     * ???????????????
     */
    private ScheduledExecutorService scheduledExecutorService;

    private boolean isShutdown;

    /**
     * ??????????????? commandId,???????????? commandId ???????????????????????????
     */
    private long executeCommandId;





    /**
     * ?????????????????? simulator
     */
    private AtomicBoolean isInit = new AtomicBoolean(false);

    public HttpAgentScheduler() {
    }

    private Result uninstallModule(CommandPacket commandPacket) {
        Map<String, Object> extras = commandPacket.getExtras();
        if (extras == null || extras.isEmpty()) {
            return Result.errorResult("????????????????????????[moduleId ??????]");
        }
        String moduleId = (String) extras.get("moduleId");
        if (StringUtils.isBlank(moduleId)) {
            return Result.errorResult("??????????????? ID ??????");
        }
        if (commandExecutor.isModuleInstalled(moduleId)) {
            try {
                commandExecutor.execute(new UnloadModuleCommand(moduleId));
            } catch (Throwable throwable) {
                logger.error("execute module uninstall error. moduleId={}", moduleId, throwable);
                return Result.errorResult("?????????????????????????????????????????????????????????." + throwable.getMessage());
            }
        }
        return Result.SUCCESS;
    }

    /**
     * ????????????
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
     * ????????????
     *
     * @param commandPacket
     * @return
     */
    private Result installModule(CommandPacket commandPacket) {
        Map<String, Object> extras = commandPacket.getExtras();
        if (extras == null || extras.isEmpty()) {
            return Result.errorResult("????????????????????????[moduleId ??????]");
        }
        String moduleId = (String) extras.get("moduleId");
        if (StringUtils.isBlank(moduleId)) {
            return Result.errorResult("??????????????? ID ??????");
        }
        try {
            if (commandExecutor.isModuleInstalled(moduleId)) {
                try {
                    commandExecutor.execute(new UnloadModuleCommand(moduleId));
                } catch (Throwable throwable) {
                    logger.error("execute module uninstall error. moduleId={}", moduleId, throwable);
                    return Result.errorResult("?????????????????????????????????????????????????????????." + throwable.getMessage());
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
                return Result.errorResult("??????????????????????????????." + throwable.getMessage());
            }
            return Result.SUCCESS;
        } catch (Throwable throwable) {
            logger.error("execute module install occur unknow error. moduleId={}", moduleId, throwable);
            return Result.errorResult(throwable);
        }
    }

    /**
     * ????????????
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
     * ???????????????
     *
     * @param commandPacket
     */
    private Result executeCommandPacket(CommandPacket commandPacket) throws Throwable {
        /**
         * ???????????????????????????????????????
         */
        if (commandPacket.getId() <= executeCommandId) {
            return null;
        }
        /**
         * ?????????????????????
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
            return Result.errorResult("??????????????????????????????:" + operateType);
        } else if (commandPacket.getCommandType() == CommandPacket.COMMAND_TYPE_MODULE) {
            //????????????????????????
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
            return Result.errorResult("??????????????????????????????:" + operateType);
        } else {
            return Result.errorResult("????????????????????????:" + commandPacket.getCommandType());
        }
    }

    /**
     * ????????????????????????
     *
     * @param commandId ?????? ID
     * @param isSuccess ????????????
     */
    private void reportCommandExecuteResult(long commandId, boolean isSuccess, String errorMsg) {
        externalAPI.reportCommandResult(commandId, isSuccess, errorMsg);
    }

    @Override
    public void start() {
        //install local if no latest command packet found
        //?????????????????????????????????????????????????????????installLocal??????????????????
        CommandPacket commandPacket = getLatestCommandPacket();
        if (commandPacket == null) {
            installLocalOrRemote();
        } else {
            logger.error("?????????????????????,??????????????????,????????????:" + JSON.toJSONString(commandPacket));
        }


    startScheduler();
    }

    private List<CommandExecuteResponse> handleCommandExecuteResponse(CommandExecuteResponse commandExecuteResponse){
        List<CommandExecuteResponse> commandExecuteResponseList = new ArrayList<CommandExecuteResponse>(HeartCommandUtils.futureMapSize());
        //????????????????????????????????????????????????
        if (commandExecuteResponse.isSuccess()){
            try {
                //??????????????????????????????????????????????????????????????????
                if (commandExecuteResponse.getResult() == null){
                    HeartCommandUtils.getFutureMap().clear();
                }
                else if (null != commandExecuteResponse.getResult()){
                    Map<String, Object> map = (Map<String, Object>) commandExecuteResponse.getResult();
                    List<String> successList = (List<String>) map.get("success");
                    List<String> inExecuteList = (List<String>) map.get("inExecute");
                    Map<String, String> failedMap = (Map<String, String>) map.get("failed");

                    for (Map.Entry<CommandExecuteKey, CommandExecuteResponse> entry : HeartCommandUtils.getFutureMap().entrySet()){
                        CommandExecuteResponse response = entry.getValue();
                        if (null != successList && successList.contains(entry.getKey().toString())){
                            response.setExecuteStatus("finished");
                            response.setMsg(null);
                            response.setSuccess(true);
                            commandExecuteResponseList.add(response);
                        }
                        else if (null != failedMap && failedMap.containsKey(entry.getKey().toString())){
                            response.setSuccess(true);
                            response.setExecuteStatus("failed");
                            response.setMsg(failedMap.get(entry.getKey().toString()));
                            commandExecuteResponseList.add(response);
                        } else {//????????????????????????????????????????????????
                            if (inExecuteList == null
                                    || !inExecuteList.contains(entry.getKey().toString())){
                                response.setSuccess(entry.getValue().isSuccess());
                                response.setExecuteStatus(entry.getValue().getExecuteStatus());
                                response.setMsg(entry.getValue().getMsg());
                                commandExecuteResponseList.add(response);
                            } else if (entry.getValue().getWaitTimes() * schedulerArgs.getInterval() > 60 * 30){
                                response.setSuccess(entry.getValue().isSuccess());
                                response.setExecuteStatus("failed");
                                if (schedulerArgs.getInterval() > 60 * 30) {
                                    response.setMsg(entry.getValue().getMsg());
                                } else {
                                    response.setMsg("??????????????????????????????");
                                }
                                commandExecuteResponseList.add(response);
                            }
                            else {
                                logger.error("??????????????????????????????????????????+1?????????????????? {} s ",
                                        entry.getValue().getWaitTimes() * schedulerArgs.getInterval());
                                logger.error("taskId is ,", entry.getKey().toString());
                                entry.getValue().setWaitTimesAdd();
                            }
                        }
                    }


                }
            } catch (Throwable throwable) {
                logger.error("getCommandExecuteResponses error", throwable);
                return commandExecuteResponseList;
            }
        } else {//???????????????????????????
            for (Map.Entry<CommandExecuteKey, CommandExecuteResponse> entry : HeartCommandUtils.getFutureMap().entrySet()){
                if (!entry.getValue().isSuccess() || "failed".equals(entry.getValue().getExecuteStatus())){
                    commandExecuteResponseList.add(entry.getValue());
                }
            }
        }

        return commandExecuteResponseList;
    }

    /**
     * ????????????????????????????????????????????????
     * @return
     */
    private CommandExecuteResponse getCommandExecuteResponses(){
        if (HeartCommandUtils.futureMapEmpty()){
            return null;
        }
        //????????????????????????????????????????????????
        CommandPacket commandPacket = new CommandPacket();
        commandPacket.setId(-2);
        commandPacket.setSync(true);
        commandPacket.setUuid("-2");
        Map<String, Object> extras = new HashMap<String, Object>();
        extras.put(HeartCommandConstants.MODULE_ID_KEY, HeartCommandConstants.MODULE_ID_VALUE_COMMAND_EXECUTE_MODULE);
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
        if (null == commandPacketList){
            if (heartRequest.getDependencyInfo() == null){
                logger.error("?????????????????????????????????module.properties????????????");
            }
            return null;
        } else if (commandPacketList.isEmpty()){//??????????????????
            CommandPacket commandPacket = new CommandPacket();
            Map<String, Object> extra = new HashMap<String, Object>();
            extra.put(HeartCommandConstants.PATH_TYPE_KEY, -1);
            commandPacket.setExtras(extra);
            commandPacket.setId(HeartCommandConstants.startCommandId);
            commandPacketList.add(commandPacket);
        }
        if (commandPacketList.get(0).getExtras().get(HeartCommandConstants.UPGRADE_BATCH_KEY) == null){
            logger.error("??????????????????????????????????????????????????????!");
        } else {
            HeartCommandConstants.setCurUpgradeBatch((String) commandPacketList.get(0).getExtras().get(HeartCommandConstants.UPGRADE_BATCH_KEY));
        }
        return commandPacketList.get(0);
    }

    /**
     * ???????????????????????????
     * @return
     */
    private List<CommandPacket> getCommandPacketByHeart(HeartRequest heartRequest){
        //?????????????????????????????????????????????
        CommandExecuteResponse preCommandExecuteResultsResponse = getCommandExecuteResponses();
        List<CommandExecuteResponse> preCommandExecuteResponseList = new ArrayList<CommandExecuteResponse>();
        if (preCommandExecuteResultsResponse != null){
            preCommandExecuteResponseList = handleCommandExecuteResponse(preCommandExecuteResultsResponse);
            if (preCommandExecuteResponseList.size() > 0){
                for (CommandExecuteResponse commandExecuteResponse : preCommandExecuteResponseList){
                    logger.info("????????????????????????:{}", JSON.toJSONString(commandExecuteResponse));
                }
            }
        }
        if (HeartCommandUtils.futureMapSize() > 30 ||(preCommandExecuteResultsResponse != null && preCommandExecuteResultsResponse.isTaskExceed())){
            heartRequest.setTaskExceed(true);
        }
        heartRequest.setCommandResult(preCommandExecuteResponseList);
        heartRequest.setCurUpgradeBatch(HeartCommandConstants.getCurUpgradeBatch());
        List<CommandPacket> newCommandPacketList = externalAPI.sendHeart(heartRequest);
        if (newCommandPacketList != null){
            //???????????????????????????????????????????????????????????????????????????
            Iterator<CommandPacket> iterator = newCommandPacketList.iterator();
            while (iterator.hasNext()){
                CommandPacket commandPacket = iterator.next();
                CommandExecuteResponse commandExecuteResponse = HeartCommandUtils.getCommandExecuteResponseFuture(commandPacket.getId(), commandPacket.getUuid());
                if (commandExecuteResponse != null && commandExecuteResponse.isSuccess() && !"inExecute".equals(commandExecuteResponse.getExecuteStatus())){
                    iterator.remove();
                }
            }
        }

        //????????????????????????????????????????????????????????????????????????????????????????????????
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
        //??????????????????????????????????????????????????????????????????
        //TODO
        int time = 0;
        while ((startCommandPacket = getStartCommandPacket()) == null){
            try {
                time ++;
                if(time == 20){
                    logger.error("??????10s????????????simulator??????, ??????????????????????????????");
                    break;
                }
                logger.error("??????simulator??????????????????,??????500ms???????????????????????????????????????");
                Thread.sleep(500 );
            } catch (InterruptedException ignore) {
            }
        }
        install(startCommandPacket);
    }


    private void executeCommand(){
        try {

            /**
             * ????????????????????????
             */
            final CommandPacket c = getLatestCommandPacket();
            if (c != null) {
                try {
                    Result result = executeCommandPacket(c);
                    if (result != null) {
                        executeCommandId = c.getId();
                        reportCommandExecuteResult(c.getId(), result.isSuccess, result.errorMsg);
                    }
                } catch (Throwable throwable) {
                    logger.error("execute command failed. command={}", c, throwable);
                    reportCommandExecuteResult(c.getId(), false,
                            HttpAgentScheduler.toString(throwable));
                }
            }


            HeartRequest heartRequest = new HeartRequest();
            getSimulatorStatus();
            getSimulatorDetails(heartRequest);
            //???????????????
            if (c != null && c.getOperateType() == CommandPacket.OPERATE_TYPE_UNINSTALL){
                heartRequest.setUninstallStatus(1);
            }
            List<CommandPacket> heartCommandPackets = getCommandPacketByHeart(heartRequest);

            boolean asyncTaskResult = true;
            if (null != heartCommandPackets && !heartCommandPackets.isEmpty() && HeartCommandUtils.futureMapSize() < 30){
                for (final CommandPacket commandPacket : heartCommandPackets){
                    commandPacket.getExtras().put(HeartCommandConstants.MODULE_ID_KEY, HeartCommandConstants.MODULE_ID_VALUE_COMMAND_EXECUTE_MODULE);
                    commandPacket.getExtras().put(HeartCommandConstants.MODULE_METHOD_KEY, HeartCommandConstants.MODULE_METHOD_VALUE_PRADAR_CONFIG_FETCHER_DO_COMAAND);
                    commandPacket.getExtras().put(HeartCommandConstants.MODULE_EXECUTE_COMMAND_TASK_SYNC_KEY, String.valueOf(commandPacket.isSync()));
                    commandPacket.getExtras().put(HeartCommandConstants.COMMAND_ID_KEY, commandPacket.getId());
                    commandPacket.getExtras().put(HeartCommandConstants.TASK_ID_KEY, commandPacket.getUuid());
                    //??????????????????????????????????????????
                    if (commandPacket.isSync()){
                        CommandExecuteResponse commandExecuteResponse = commandExecutor.execute(new HeartCommand(commandPacket));
                        HeartCommandUtils.putCommandExecuteResponse(commandPacket.getId(), commandPacket.getUuid(), commandExecuteResponse);
                        //???????????????????????????????????????????????????
                        if (!commandExecuteResponse.isSuccess() || "failed".equals(commandExecuteResponse.getExecuteStatus())){
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
        //?????????????????????
        this.scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                while (!isShutdown && AgentStatus.doInstall()) {
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


    /**
     * ??????simulator????????????
     * @throws Throwable
     */
    private void getSimulatorStatus() throws Throwable {
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
            } else {
                logger.error("??????simulator??????",commandExecuteResponse.getMsg());
            }
        }
    }

    /**
     * ??????simulator??????????????????
     * @throws Throwable
     */
    private void getSimulatorDetails(HeartRequest heartRequest) throws Throwable {
            CommandPacket commandPacket = new CommandPacket();
            commandPacket.setId(HeartCommandConstants.getSimulatorDetailsCommandId);
            commandPacket.setSync(true);
            commandPacket.setUuid("-1");
            Map<String, Object> extras = new HashMap<String, Object>();
            extras.put(HeartCommandConstants.MODULE_ID_KEY, HeartCommandConstants.MODULE_ID_VALUE_PRADAR_CONFIG_FETCHER);
            extras.put(HeartCommandConstants.MODULE_METHOD_KEY, HeartCommandConstants.MODULE_METHOD_VALUE_PRADAR_CONFIG_FETCHER);
            extras.put(HeartCommandConstants.COMMAND_ID_KEY, commandPacket.getId());
            extras.put(HeartCommandConstants.TASK_ID_KEY, commandPacket.getUuid());
            extras.put(HeartCommandConstants.MODULE_EXECUTE_COMMAND_TASK_SYNC_KEY, "true");
            commandPacket.setExtras(extras);
            CommandExecuteResponse commandExecuteResponse = commandExecutor.execute(new HeartCommand(commandPacket));
            if (commandExecuteResponse.isSuccess()){
                JSONObject jsonObject = (JSONObject) commandExecuteResponse.getResult();
                heartRequest.setDormantStatus(jsonObject.getInteger("isSilent"));
            }
    }

}
