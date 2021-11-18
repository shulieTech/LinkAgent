package com.shulie.instrument.simulator.agent.api.model;

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
    private String uninstallStatus;

    /**
     * 休眠状态，0:为休眠，1:已休眠
     */
    private String dormantStatus;

    /**
     * agent版本
     */
    private String agentVersion;

    /**
     * 模块版本信息
     */
    private String dependencyInfo;


    /**
     *
     */
    List<CommandExecuteResponse> commandExecuteResponseList;


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

    public String getUninstallStatus() {
        return uninstallStatus;
    }

    public void setUninstallStatus(String uninstallStatus) {
        this.uninstallStatus = uninstallStatus;
    }

    public String getDormantStatus() {
        return dormantStatus;
    }

    public void setDormantStatus(String dormantStatus) {
        this.dormantStatus = dormantStatus;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
    }

    public List<CommandExecuteResponse> getCommandExecuteResponseList() {
        return commandExecuteResponseList;
    }

    public void setCommandExecuteResponseList(List<CommandExecuteResponse> commandExecuteResponseList) {
        this.commandExecuteResponseList = commandExecuteResponseList;
    }


    public String getDependencyInfo() {
        return dependencyInfo;
    }

    public void setDependencyInfo(String dependencyInfo) {
        this.dependencyInfo = dependencyInfo;
    }
}
