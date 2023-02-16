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
package com.shulie.instrument.simulator.agent.api.model;

import com.shulie.instrument.simulator.agent.spi.model.CommandExecuteResponse;

import java.util.List;

/**
 * @author angju
 * @date 2021/11/17 16:50
 */
public class HeartRequest {
    /**
     * 应用名称
     */
    private String projectName;

    /**
     * agentID
     */
    private String agentId;


    /**
     * 应用id地址
     */
    private String ipAddress;

    /**
     * 进程号
     */
    private String progressId;

    /**
     * 当前升级批次号
     * 默认-1
     */
    private String curUpgradeBatch;

    /**
     * agent状态
     */
    private String agentStatus;

    /**
     * agent错误信息
     */
    private String agentErrorInfo;

    /**
     * simulator状态
     */
    private String simulatorStatus;


    /**
     * simulator异常信息
     */
    private String simulatorErrorInfo;

    /**
     * 卸载状态，0:未卸载，1:已卸载
     */
    private int uninstallStatus = 0;

    /**
     * 休眠状态，0:未休眠，1:已休眠
     */
    private int dormantStatus = 0;

    /**
     * agent版本
     */
    private String agentVersion;


    /**
     * simulator版本
     */
    private String simulatorVersion;

    /**
     * 模块版本信息
     */
    private String dependencyInfo;


    /**
     * 标志
     */
    private String flag = "shulieEnterprise";

    private boolean taskExceed = false;

    /**
     *
     */
    List<CommandExecuteResponse> commandResult;


    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getProgressId() {
        return progressId;
    }

    public void setProgressId(String progressId) {
        this.progressId = progressId;
    }

    public String getCurUpgradeBatch() {
        return curUpgradeBatch;
    }

    public void setCurUpgradeBatch(String curUpgradeBatch) {
        this.curUpgradeBatch = curUpgradeBatch;
    }

    public String getAgentStatus() {
        return agentStatus;
    }

    public void setAgentStatus(String agentStatus) {
        this.agentStatus = agentStatus;
    }

    public String getAgentErrorInfo() {
        return agentErrorInfo;
    }

    public void setAgentErrorInfo(String agentErrorInfo) {
        this.agentErrorInfo = agentErrorInfo;
    }

    public String getSimulatorStatus() {
        return simulatorStatus;
    }

    public void setSimulatorStatus(String simulatorStatus) {
        this.simulatorStatus = simulatorStatus;
    }

    public String getSimulatorErrorInfo() {
        return simulatorErrorInfo;
    }

    public void setSimulatorErrorInfo(String simulatorErrorInfo) {
        this.simulatorErrorInfo = simulatorErrorInfo;
    }

    public int getUninstallStatus() {
        return uninstallStatus;
    }

    public void setUninstallStatus(int uninstallStatus) {
        this.uninstallStatus = uninstallStatus;
    }

    public int getDormantStatus() {
        return dormantStatus;
    }

    public void setDormantStatus(int dormantStatus) {
        this.dormantStatus = dormantStatus;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
    }

    public List<CommandExecuteResponse> getCommandResult() {
        return commandResult;
    }

    public void setCommandResult(List<CommandExecuteResponse> commandExecuteResponseList) {
        this.commandResult = commandExecuteResponseList;
    }


    public String getDependencyInfo() {
        return dependencyInfo;
    }

    public void setDependencyInfo(String dependencyInfo) {
        this.dependencyInfo = dependencyInfo;
    }


    public String getFlag() {
        return flag;
    }


    public boolean isTaskExceed() {
        return taskExceed;
    }

    public void setTaskExceed(boolean taskExceed) {
        this.taskExceed = taskExceed;
    }

    public String getSimulatorVersion() {
        return simulatorVersion;
    }

    public void setSimulatorVersion(String simulatorVersion) {
        this.simulatorVersion = simulatorVersion;
    }
}
