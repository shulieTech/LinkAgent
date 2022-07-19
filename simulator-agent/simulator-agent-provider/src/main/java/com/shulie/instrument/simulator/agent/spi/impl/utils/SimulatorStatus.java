/*
 * *
 *  * Copyright 2021 Shulie Technology, Co.Ltd
 *  * Email: shulie@shulie.io
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.shulie.instrument.simulator.agent.spi.impl.utils;

/**
 * @author angju
 * @date 2021/11/25 11:46
 */
public class SimulatorStatus {
    private final static String INSTALLED = "INSTALLED";
    public final static String INSTALL_FAILED = "INSTALL_FAILED";

    private static String errorMsg = null;

    private static volatile String AGENT_STATUS = null;


    public static void agentInstalled(){
        AGENT_STATUS = INSTALLED;
    }

    public static void agentInstallFailed(){
        AGENT_STATUS = INSTALL_FAILED;
    }

    public static String getAgentStatus() {
        return AGENT_STATUS;
    }

    public static boolean isUnInstall(){
        return null == AGENT_STATUS;
    }

    public static void setAgentStatus(String agentStatus){
        AGENT_STATUS = agentStatus;
    }

    public static void setAgentStatus(String agentStatus, String errorMsg){
        AGENT_STATUS = agentStatus;
        SimulatorStatus.errorMsg = errorMsg;
    }

    public static String getErrorMsg() {
        return errorMsg;
    }
}
