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
package com.shulie.instrument.simulator.agent.core.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.shulie.instrument.simulator.agent.api.ExternalAPI;
import com.shulie.instrument.simulator.agent.api.model.CommandPacket;
import com.shulie.instrument.simulator.agent.api.model.HeartRequest;
import com.shulie.instrument.simulator.agent.api.model.Result;
import com.shulie.instrument.simulator.agent.core.util.ConfigUtils;
import com.shulie.instrument.simulator.agent.core.util.DownloadUtils;
import com.shulie.instrument.simulator.agent.core.util.HttpUtils;
import com.shulie.instrument.simulator.agent.spi.config.AgentConfig;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/23 7:21 下午
 */
public class ExternalAPIImpl implements ExternalAPI {
    private final static Logger logger = LoggerFactory.getLogger(ExternalAPIImpl.class);

    private AgentConfig agentConfig;
    private AtomicBoolean isWarnAlready;

    private final static String COMMAND_URL = "api/agent/application/node/probe/operate";

    /**
     * 心跳接口
     */
    private final static String HEART_URL = "api/agent/heartbeat";
    private final static String REPORT_URL = "api/agent/application/node/probe/operateResult";




    public ExternalAPIImpl(AgentConfig agentConfig) {
        this.agentConfig = agentConfig;
        isWarnAlready = new AtomicBoolean(false);
    }

    @Override
    public void onlineUpgrade(CommandPacket commandPacket) {

    }

    @Override
    public File downloadModule(String agentDownloadUrl, String targetPath) {
        if (StringUtils.isNotBlank(agentDownloadUrl)) {
            StringBuilder builder = new StringBuilder(agentDownloadUrl);
            if (StringUtils.indexOf(agentDownloadUrl, '?') != -1) {
                builder.append("&appName=").append(agentConfig.getAppName()).append("&agentId=").append(
                    agentConfig.getAgentId());
            } else {
                builder.append("?appName=").append(agentConfig.getAppName()).append("&agentId=").append(
                    agentConfig.getAgentId());
            }
            return DownloadUtils.download(builder.toString(), targetPath, agentConfig.getUserAppKey());
        }
        return null;
    }

    @Override
    public void reportCommandResult(long commandId, boolean isSuccess, String errorMsg) {
        String webUrl = agentConfig.getTroWebUrl();
        if (StringUtils.isBlank(webUrl)) {
            logger.warn("AGENT: tro.web.url is not assigned.");
            return;
        }
        String url = joinUrl(webUrl, REPORT_URL);
        Map<String, String> body = new HashMap<String, String>();
        body.put("appName", agentConfig.getAppName());
        body.put("agentId", agentConfig.getAgentId());
        body.put("operateResult", isSuccess ? "1" : "0");
        if (StringUtils.isNotBlank(errorMsg)) {
            body.put("errorMsg", errorMsg);
        }
        HttpUtils.doPost(url, agentConfig.getUserAppKey(), JSON.toJSONString(body));
    }

    @Override
    public CommandPacket getLatestCommandPacket() {
        String webUrl = agentConfig.getTroWebUrl();
        if (StringUtils.isBlank(webUrl)) {
            logger.warn("AGENT: tro.web.url is not assigned.");
            return CommandPacket.NO_ACTION_PACKET;
        }
        //todo file模式？
        String agentConfigUrl = joinUrl(webUrl, COMMAND_URL);
        if (StringUtils.isBlank(agentConfigUrl)) {
            if (isWarnAlready.compareAndSet(false, true)) {
                logger.warn("AGENT: agent.command.url is not assigned.");
            }
            return CommandPacket.NO_ACTION_PACKET;
        }
        String url = agentConfigUrl;
        if (StringUtils.indexOf(url, '?') != -1) {
            url += "&appName=" + agentConfig.getAppName() + "&agentId=" + agentConfig.getAgentId();
        } else {
            url += "?appName=" + agentConfig.getAppName() + "&agentId=" + agentConfig.getAgentId();
        }

        String resp = ConfigUtils.doConfig(url, agentConfig.getUserAppKey());
        if (StringUtils.isBlank(resp)) {
            logger.warn("AGENT: fetch agent command got a err response. {}", url);
            return CommandPacket.NO_ACTION_PACKET;
        }

        /**
         * 这个地方如果服务端没有最新需要执行的命令，则建议返回空,也可以返回最后一次的命令
         */
        try {
            Type type = new TypeReference<Result<CommandPacket>>() {}.getType();
            Result<CommandPacket> response = JSON.parseObject(resp, type);
            if (!response.isSuccess()) {
                logger.error("fetch agent command got a fault response. resp={}", resp);
                throw new RuntimeException(response.getError());
            }
            return response.getData();
        } catch (Throwable e) {
            logger.error("AGENT: parse command err. {}", resp, e);
            return CommandPacket.NO_ACTION_PACKET;
        }
    }



    @Override
    public List<CommandPacket> sendHeart(HeartRequest heartRequest) {
        HeartRequestUtil.configHeartRequest(heartRequest, agentConfig);
        String webUrl = agentConfig.getTroWebUrl();
        if (StringUtils.isBlank(webUrl)) {
            logger.warn("AGENT: tro.web.url is not assigned.");
            return null;
        }
        String agentHeartUrl = joinUrl(webUrl, HEART_URL);

        HttpUtils.HttpResult resp = HttpUtils.doPost(agentHeartUrl, agentConfig.getUserAppKey(), JSON.toJSONString(heartRequest));

        if (null == resp) {
            logger.warn("AGENT: sendHeart got a err response. {}", agentHeartUrl);
            return null;
        }

        try {
            Type type = new TypeReference<Result<List<CommandPacket>>>() {}.getType();
            Result<List<CommandPacket>> response = JSON.parseObject(resp.getResult(), type);
            if (!response.isSuccess()) {
                logger.error("sendHeart got a err response. resp={}", resp);
                throw new RuntimeException(response.getError());
            }
            return response.getData();
        } catch (Throwable e) {
            logger.error("AGENT: parse command err. {}", resp, e);
            return null;
        }
    }

    @Override
    public List<String> getAgentProcessList() {
        String url = agentConfig.getUploadAgentProcesslistUrl();
        if (StringUtils.isBlank(url)) {
            return Collections.EMPTY_LIST;
        }

        List<String> processlist = new ArrayList<String>();
        final List<VirtualMachineDescriptor> list = VirtualMachine.list();
        for (VirtualMachineDescriptor descriptor : list) {
            processlist.add(descriptor.displayName());
        }
        return processlist;
    }

    private String joinUrl(String troWebUrl, String endpoint) {
        return troWebUrl.endsWith("/") ? troWebUrl + endpoint : troWebUrl + "/" + endpoint;
    }
}
