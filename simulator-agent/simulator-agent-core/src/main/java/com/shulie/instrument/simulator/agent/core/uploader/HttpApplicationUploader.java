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
package com.shulie.instrument.simulator.agent.core.uploader;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;

import com.shulie.instrument.simulator.agent.core.util.HttpUtils;
import com.shulie.instrument.simulator.agent.spi.config.AgentConfig;
import io.shulie.takin.sdk.kafka.HttpSender;
import io.shulie.takin.sdk.kafka.MessageSendCallBack;
import io.shulie.takin.sdk.kafka.MessageSendService;
import io.shulie.takin.sdk.pinpoint.impl.PinpointSendServiceFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/06/10 9:27 下午
 */
public class HttpApplicationUploader implements ApplicationUploader {
    private final static Logger logger = LoggerFactory.getLogger(HttpApplicationUploader.class);

    private final AgentConfig agentConfig;

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpApplicationUploader.class.getName());

    private static final String APP_INSERT_URL = "/api/application/center/app/info";

    private static final String APP_COLUMN_APPLICATION_NAME = "applicationName";
    private static final String APP_COLUMN_DDL_PATH = "ddlScriptPath";
    private static final String APP_COLUMN_CLEAN_PATH = "cleanScriptPath";
    private static final String APP_COLUMN_READY_PATH = "readyScriptPath";
    private static final String APP_COLUMN_BASIC_PATH = "basicScriptPath";
    private static final String APP_COLUMN_CACHE_PATH = "cacheScriptPath";
    private static final String CLUSTER_NAME = "clusterName";

    public HttpApplicationUploader(AgentConfig agentConfig) {
        this.agentConfig = agentConfig;
    }

    @Override
    public void checkAndGenerateApp() {
        String webUrl = agentConfig.getTroWebUrl();
        String register = agentConfig.getProperty("register.name", "zookeeper");
        if (register.equals("zookeeper")) {
            if (StringUtils.isBlank(webUrl)) {
                logger.error("AGENT: tro.web.url is not assigned.");
                return;
            }

            if (StringUtils.isBlank(webUrl)) {
                logger.error("AGENT: user.app.key is not assigned.");
                return;
            }
        }

        String appName = agentConfig.getAppName();
        if (StringUtils.isBlank(appName)) {
            logger.error("AGENT: -Dpradar.project.name is not assigned.");
            return;
        }

        final Map<String, Object> map = new HashMap<String, Object>();
        map.put(APP_COLUMN_APPLICATION_NAME, appName);
        map.put(CLUSTER_NAME, this.getClusterName());
        map.put(APP_COLUMN_DDL_PATH, appName + "/ddl.sh");
        map.put(APP_COLUMN_CLEAN_PATH, appName + "/clean.sh");
        map.put(APP_COLUMN_READY_PATH, appName + "/ready.sh");
        map.put(APP_COLUMN_BASIC_PATH, appName + "/basic.sh");
        map.put(APP_COLUMN_CACHE_PATH, appName + "/cache.sh");
        final StringBuilder url = new StringBuilder(webUrl != null ? webUrl : "").append(APP_INSERT_URL);
        try {
            MessageSendService messageSendService = new PinpointSendServiceFactory().getKafkaMessageInstance();
            messageSendService.send(APP_INSERT_URL, agentConfig.getHttpMustHeaders(), JSON.toJSONString(map), new MessageSendCallBack() {
                @Override
                public void success() {
                    LOGGER.info("上报应用成功 url={} ", url);
                }

                @Override
                public void fail(String errorMessage) {
                    LOGGER.error("上报应用失败 url={}, errorMessage={}", url, errorMessage);
                }
            }, new HttpSender() {
                @Override
                public void sendMessage() {
                    HttpUtils.HttpResult httpResult = HttpUtils.doPost(url.toString(), agentConfig.getHttpMustHeaders(),
                            JSON.toJSONString(map));
                    if (httpResult == null) {
                        LOGGER.error("上报应用失败 url={}, result is null", url);
                    } else if (!httpResult.isSuccess()) {
                        LOGGER.error("上报应用失败 url={}, result={}", url, httpResult.getResult());
                    } else {
                        LOGGER.info("上报应用成功 url={}, result={}", url, httpResult.getResult());
                    }
                }
            });

        } catch (Throwable e) {
            LOGGER.error("自动增加应用失败 url={}", url, e);
        }
    }

    private String getClusterName(){
        String clusterName = System.getProperty("cluster.name", null);
        if (clusterName != null){
            return clusterName;
        }
        return agentConfig.getProperty("cluster.name", null);
    }
}
