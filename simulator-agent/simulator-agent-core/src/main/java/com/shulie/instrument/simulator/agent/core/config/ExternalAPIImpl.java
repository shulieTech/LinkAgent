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
import com.shulie.instrument.simulator.agent.api.model.AppConfig;
import com.shulie.instrument.simulator.agent.api.model.CommandPacket;
import com.shulie.instrument.simulator.agent.api.model.Result;
import com.shulie.instrument.simulator.agent.core.util.ConfigUtils;
import com.shulie.instrument.simulator.agent.core.util.DownloadUtils;
import com.shulie.instrument.simulator.agent.spi.config.AgentConfig;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/23 7:21 下午
 */
public class ExternalAPIImpl implements ExternalAPI {
    private final static Logger logger = LoggerFactory.getLogger(ExternalAPIImpl.class);

    private AgentConfig agentConfig;
    private AtomicBoolean isWarnAlready;

    public ExternalAPIImpl(AgentConfig agentConfig) {
        this.agentConfig = agentConfig;
        isWarnAlready = new AtomicBoolean(false);
    }

    @Override
    public File downloadAgent(String agentDownloadUrl, String targetPath) {
        if (StringUtils.isNotBlank(agentDownloadUrl)) {
            if (StringUtils.indexOf(agentDownloadUrl, '?') != -1) {
                agentDownloadUrl += "&appName=" + agentConfig.getAppName() + "&agentId=" + agentConfig.getAgentId();
            } else {
                agentDownloadUrl += "?appName=" + agentConfig.getAppName() + "&agentId=" + agentConfig.getAgentId();
            }
            return DownloadUtils.download(agentDownloadUrl, targetPath);
        }
        return null;
    }

    @Override
    public File downloadModule(String agentDownloadUrl, String targetPath) {
        if (StringUtils.isNotBlank(agentDownloadUrl)) {
            StringBuilder builder = new StringBuilder(agentDownloadUrl);
            if (StringUtils.indexOf(agentDownloadUrl, '?') != -1) {
                builder.append("&appName=").append(agentConfig.getAppName()).append("&agentId=").append(agentConfig.getAgentId());
            } else {
                builder.append("?appName=").append(agentConfig.getAppName()).append("&agentId=").append(agentConfig.getAgentId());
            }
            return DownloadUtils.download(builder.toString(), targetPath);
        }
        return null;
    }

    @Override
    public void reportCommandResult(long commandId, boolean isSuccess, String errorMsg) {

    }

    @Override
    public CommandPacket getLatestCommandPacket() {
        String agentConfigUrl = agentConfig.getProperty("agent.command.url", null);
        if (StringUtils.isBlank(agentConfigUrl)) {
            if (isWarnAlready.compareAndSet(false, true)) {
                logger.warn("AGENT: agent.command.url is not assigned.");
            }
            return CommandPacket.DEFAULT_PACKET;
        }
        String url = agentConfigUrl;
        if (StringUtils.indexOf(url, '?') != -1) {
            url += "&appName=" + agentConfig.getAppName() + "&agentId=" + agentConfig.getAgentId();
        } else {
            url += "?appName=" + agentConfig.getAppName() + "&agentId=" + agentConfig.getAgentId();
        }

        String resp = ConfigUtils.doConfig(url, agentConfig.getUserAppKey());
        if (StringUtils.isBlank(resp)) {
            logger.error("AGENT: fetch agent command got a err response. {}", url);
            throw new RuntimeException("fetch agent command got a err response. " + url);
        }

        /**
         * 这个地方如果服务端没有最新需要执行的命令，则建议返回空,也可以返回最后一次的命令
         */
        try {
            Type type = new TypeReference<Result<CommandPacket>>() {
            }.getType();
            Result<CommandPacket> response = JSON.parseObject(resp, type);
            if (!response.isSuccess()) {
                logger.error("fetch agent command got a fault response. resp={}", resp);
                throw new RuntimeException(response.getErrorMsg());
            }
            return response.getResult();
        } catch (Throwable e) {
            logger.error("AGENT: parse command err. {}", resp, e);
            throw new RuntimeException("AGENT: parse command err.. " + resp, e);
        }
    }

    @Override
    public AppConfig getAppConfig() throws RuntimeException {
        String agentConfigUrl = agentConfig.getProperty("agent.config.url", null);
        if (StringUtils.isBlank(agentConfigUrl)) {
            if (isWarnAlready.compareAndSet(false, true)) {
                logger.warn("AGENT: agent.config.url is not assigned.");
            }
            /**
             * 如果没有配置则给一个默认值
             */
            AppConfig appConfig = new AppConfig();
            appConfig.setRunning(true);
            appConfig.setAgentVersion("1.0.0");
            return appConfig;
        }
        String url = agentConfigUrl;
        if (StringUtils.indexOf(url, '?') != -1) {
            url += "&appName=" + agentConfig.getAppName() + "&agentId=" + agentConfig.getAgentId();
        } else {
            url += "?appName=" + agentConfig.getAppName() + "&agentId=" + agentConfig.getAgentId();
        }

        String resp = ConfigUtils.doConfig(url, agentConfig.getUserAppKey());
        if (StringUtils.isBlank(resp)) {
            logger.error("AGENT: fetch agent config got a err response. {}", url);
            throw new RuntimeException("fetch agent config got a err response. " + url);
        }

        try {
            Type type = new TypeReference<Result<AppConfig>>() {
            }.getType();
            Result<AppConfig> response = JSON.parseObject(resp, type);
            if (!response.isSuccess()) {
                logger.error("fetch agent config got a fault response. resp={}", resp);
                throw new RuntimeException(response.getErrorMsg());
            }
            return response.getResult();
        } catch (Throwable e) {
            logger.error("AGENT: parse app config err. {}", resp, e);
            throw new RuntimeException("AGENT: parse app config err.. " + resp, e);
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
}
