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
package com.shulie.instrument.simulator.agent.core.register;

import java.util.HashSet;
import java.util.Set;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/22 7:57 下午
 */
public class AgentStatus {
    private final static String INSTALLED = "INSTALLED";
    private final static String UNINSTALL = "UNINSTALL";
    private final static String INSTALLING = "INSTALLING";
    public final static String INSTALL_FAILED = "INSTALL_FAILED";
    private final static String UNINSTALL_FAILED = "UNINSTALL_FAILED";
    private final static String UNINSTALLING = "UNINSTALLING";


    private static Set<AgentStatusListener> listeners = new HashSet<AgentStatusListener>();

    /**
     * agent 状态
     */
    private static volatile String AGENT_STATUS = "UNINSTALL";
    /**
     * 错误码
     */
    private static volatile String ERROR_CODE = "";
    /**
     * 错误信息
     */
    private static volatile String ERROR_MESSAGE = "";

    private static volatile String SIMULATOR_VERSION = "";

    /**
     * 设置错误
     *
     * @param errorMsg
     */
    public static void setError(String errorMsg) {
        ERROR_CODE = "AGENT-00000";
        ERROR_MESSAGE = errorMsg;
        trigger();
    }

    public static void registerListener(AgentStatusListener listener) {
        listeners.add(listener);
    }

    /**
     * 清理错误
     */
    public static void clearError() {
        ERROR_CODE = "";
        ERROR_MESSAGE = "";
        trigger();
    }

    /**
     * 获取错误编码
     *
     * @return 错误编码
     */
    public static String getErrorCode() {
        return ERROR_CODE;
    }

    /**
     * 获取错误信息
     *
     * @return 错误信息
     */
    public static String getErrorMessage() {
        return ERROR_MESSAGE;
    }

    /**
     * 将 agent 状态置为已安装状态
     */
    public static void installed(String simulatorVersion) {
        AGENT_STATUS = INSTALLED;
        SIMULATOR_VERSION = simulatorVersion;
        clearError();
        trigger();
    }

    /**
     * 将 agent 状态置为未安装状态
     */
    public static void uninstall() {
        AGENT_STATUS = UNINSTALL;
        SIMULATOR_VERSION = "";
        clearError();
        trigger();
    }

    /**
     * 将 agent 状态置成安装中状态
     */
    public static void installing() {
        AGENT_STATUS = INSTALLING;
        SIMULATOR_VERSION = "";
        clearError();
        trigger();
    }

    /**
     * 将 agent 状态置成安装中状态
     */
    public static void uninstalling() {
        AGENT_STATUS = UNINSTALLING;
        clearError();
        trigger();
    }

    /**
     * 将 agent 状态置成安装失败状态
     */
    public static void installFailed(String errorMessage) {
        AGENT_STATUS = INSTALL_FAILED;
        SIMULATOR_VERSION = "";
        setError(errorMessage);
        trigger();
    }

    public static void uninstallFailed(String errorMessage) {
        AGENT_STATUS = UNINSTALL_FAILED;
        setError(errorMessage);
        trigger();
    }

    /**
     * 获取 agent 状态
     *
     * @return
     */
    public static String getAgentStatus() {
        return AGENT_STATUS;
    }

    public static String getSimulatorVersion() {
        return SIMULATOR_VERSION;
    }

    private static void trigger() {
        for (AgentStatusListener listener : listeners) {
            listener.onListen();
        }
    }

    /**
     * 是否安装操作完成，安装失败或者安装成功
     * @return
     */
    public static boolean doInstall(){
        return INSTALLED.equals(AGENT_STATUS) || INSTALL_FAILED.equals(AGENT_STATUS);
    }


}
