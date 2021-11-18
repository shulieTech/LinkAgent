package com.shulie.instrument.simulator.agent.core.config;

import com.alibaba.fastjson.JSON;
import com.shulie.instrument.simulator.agent.api.model.HeartRequest;
import com.shulie.instrument.simulator.agent.core.register.AgentStatus;
import com.shulie.instrument.simulator.agent.core.util.AddressUtils;
import com.shulie.instrument.simulator.agent.core.util.JvmArgsCheckUtils;
import com.shulie.instrument.simulator.agent.core.util.JvmArgsConstants;
import com.shulie.instrument.simulator.agent.core.util.PidUtils;
import com.shulie.instrument.simulator.agent.spi.config.AgentConfig;
import com.shulie.instrument.simulator.agent.spi.impl.model.UpgradeBatchConfig;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.lang.management.ManagementFactory;

/**
 * @author angju
 * @date 2021/11/17 19:29
 */
public class HeartRequestUtil {
    private static String agentStatus = null;
    private static String agentErrorMsg = null;

    private static final String pid = String.valueOf(PidUtils.getPid());





    public static void configHeartRequest(HeartRequest heartRequest, AgentConfig agentConfig, UpgradeBatchConfig upgradeBatchConfig){
        checkAgentStatus(agentConfig);
        heartRequest.setProjectName(agentConfig.getAppName());
        heartRequest.setAgentId(agentConfig.getAgentId());
        heartRequest.setIpAddress(AddressUtils.getLocalAddress());
        heartRequest.setProgressId(pid);
        heartRequest.setAgentStatus(agentStatus);
        heartRequest.setAgentErrorInfo(agentErrorMsg);
        heartRequest.setAgentVersion(agentConfig.getAgentVersion());
        heartRequest.setCurUpgradeBatch(upgradeBatchConfig.getCurUpgradeBatch());
    }


    private static void checkAgentStatus(AgentConfig agentConfig){

        if (agentStatus != null){
            return;
        }
        String inputArgs = JSON.toJSONString(ManagementFactory.getRuntimeMXBean().getInputArguments());

        String jvmArgsCheckResult = JSON.toJSONString(
                JvmArgsCheckUtils.checkJvmArgs(System.getProperty("java.version"), inputArgs, agentConfig));

        agentStatus = AgentStatus.getAgentStatus();
        StringBuilder errorMsg = new StringBuilder();
        if (!JvmArgsCheckUtils.getCheckJvmArgsStatus()) {
            agentStatus = AgentStatus.INSTALL_FAILED;
            errorMsg.append("启动参数校验失败：").append(jvmArgsCheckResult);
        }
        //校验日志目录是否存在并且有权限
        String checkSimulatorLogPathResult = checkSimulatorLogPath(agentConfig.getLogPath());
        if (checkSimulatorLogPathResult != null) {
            agentStatus = AgentStatus.INSTALL_FAILED;
            errorMsg.append("启动参数日志目录校验异常：").append(checkSimulatorLogPathResult);
        }
        agentErrorMsg = errorMsg.toString();

    }


    private static String checkSimulatorLogPath(String simulatorLogPath) {
        if (StringUtils.isBlank(simulatorLogPath)) {
            return String.format(JvmArgsConstants.simulatorLogPathCodeErrorMsg_4, simulatorLogPath);
        }
        File file = new File(simulatorLogPath);
        if (!file.exists()) {
            return String.format(JvmArgsConstants.simulatorLogPathCodeErrorMsg_1, simulatorLogPath);
        }
        if (!file.canWrite()) {
            return String.format(JvmArgsConstants.simulatorLogPathCodeErrorMsg_3, simulatorLogPath);
        }
        if (!file.canRead()) {
            return String.format(JvmArgsConstants.simulatorLogPathCodeErrorMsg_2, simulatorLogPath);
        }
        return null;
    }
}
