/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shulie.instrument.simulator.agent.core.config;

import com.shulie.instrument.simulator.agent.api.model.HeartRequest;
import com.shulie.instrument.simulator.agent.core.CoreLauncher;
import com.shulie.instrument.simulator.agent.core.register.AgentStatus;
import com.shulie.instrument.simulator.agent.core.util.AddressUtils;
import com.shulie.instrument.simulator.agent.core.util.PidUtils;
import com.shulie.instrument.simulator.agent.spi.config.AgentConfig;
import com.shulie.instrument.simulator.agent.api.utils.HeartCommandConstants;
import com.shulie.instrument.simulator.agent.api.utils.HeartCommandUtils;
import com.shulie.instrument.simulator.agent.spi.impl.utils.SimulatorStatus;

import java.io.File;

/**
 * @author angju
 * @date 2021/11/17 19:29
 */
public class HeartRequestUtil {
    private static String agentStatus = null;
    private static String agentErrorMsg = null;

    private static final String pid = String.valueOf(PidUtils.getPid());


    public static String getUpgradeFileTempSaveDir() {
        return new File(CoreLauncher.class.getProtectionDomain().getCodeSource().getLocation().getFile())
                .getParent();
    }


    public static void configHeartRequest(HeartRequest heartRequest, AgentConfig agentConfig) {
        heartRequest.setProjectName(agentConfig.getAppName());
        heartRequest.setAgentId(agentConfig.getAgentId());
        heartRequest.setIpAddress(AddressUtils.getLocalAddress());
        heartRequest.setProgressId(pid);
        heartRequest.setAgentStatus(AgentStatus.getAgentStatus());
        heartRequest.setSimulatorStatus(SimulatorStatus.getAgentStatus());
        heartRequest.setAgentErrorInfo(AgentStatus.getErrorMessage());
        heartRequest.setSimulatorErrorInfo(SimulatorStatus.getErrorMsg());
        heartRequest.setAgentVersion(agentConfig.getAgentVersion());
        heartRequest.setSimulatorVersion(HeartCommandUtils.SIMULATOR_VERSION);
        if (HeartCommandConstants.UN_INIT_UPGRADE_BATCH.equals(heartRequest.getCurUpgradeBatch())) {
            String agentBasePath = getUpgradeFileTempSaveDir().replace("core", "");
            File localFile = new File(agentBasePath + "agent/simulator_-1");
            if (!localFile.exists()) {
                heartRequest.setDependencyInfo(HeartCommandUtils.allModuleVersionDetail);
            }
        } else {
            HeartCommandUtils.allModuleVersionDetail = null;
        }
    }


//    private static void checkAgentStatus(AgentConfig agentConfig){
//        if (agentStatus != null){
//            return;
//        }
//        //后面的代码只需要校验一次
//        String inputArgs = JSON.toJSONString(ManagementFactory.getRuntimeMXBean().getInputArguments());
//
//        String jvmArgsCheckResult = JSON.toJSONString(
//                JvmArgsCheckUtils.checkJvmArgs(System.getProperty("java.version"), inputArgs, agentConfig));
//
//
//        StringBuilder errorMsg = new StringBuilder();
//        if (!JvmArgsCheckUtils.getCheckJvmArgsStatus()) {
//            agentStatus = AgentStatus.INSTALL_FAILED;
//            errorMsg.append("启动参数校验失败：").append(jvmArgsCheckResult);
//        }
//        //校验日志目录是否存在并且有权限
//        String checkSimulatorLogPathResult = checkSimulatorLogPath(agentConfig.getLogPath());
//        if (checkSimulatorLogPathResult != null) {
//            agentStatus = AgentStatus.INSTALL_FAILED;
//            errorMsg.append("启动参数日志目录校验异常：").append(checkSimulatorLogPathResult);
//        }
//        agentErrorMsg = errorMsg.toString();
//
//    }


//    private static String checkSimulatorLogPath(String simulatorLogPath) {
//        if (StringUtils.isBlank(simulatorLogPath)) {
//            return String.format(JvmArgsConstants.simulatorLogPathCodeErrorMsg_4, simulatorLogPath);
//        }
//        File file = new File(simulatorLogPath);
//        if (!file.exists()) {
//            return String.format(JvmArgsConstants.simulatorLogPathCodeErrorMsg_1, simulatorLogPath);
//        }
//        if (!file.canWrite()) {
//            return String.format(JvmArgsConstants.simulatorLogPathCodeErrorMsg_3, simulatorLogPath);
//        }
//        if (!file.canRead()) {
//            return String.format(JvmArgsConstants.simulatorLogPathCodeErrorMsg_2, simulatorLogPath);
//        }
//        return null;
//    }
}
