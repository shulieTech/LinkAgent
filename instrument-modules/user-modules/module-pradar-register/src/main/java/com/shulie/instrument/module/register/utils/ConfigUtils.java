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
package com.shulie.instrument.module.register.utils;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.common.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/20 3:19 下午
 */
public class ConfigUtils {
    private final static Logger logger = LoggerFactory.getLogger(ConfigUtils.class);

    //配置控制台地址
    private static final String AGENT_CONFIG_URL = "/api/fast/agent/access/config/agentConfig";

    /**
     * 获取控制台的固定配置信息
     */
    public static Map<String, Object> getFixedSimulatorConfigFromUrl(String troWebUrl, String appName,
        String configVersion) {
        return getAgentConfigFromUrl(troWebUrl, appName, configVersion, "0");
    }

    /**
     * 获取控制台的动态配置信息
     */
    public static Map<String, Object> getDynamicSimulatorConfigFromUrl(String troWebUrl, String appName,
        String configVersion) {
        return getAgentConfigFromUrl(troWebUrl, appName, configVersion, "1");
    }

    private static Map<String, Object> getAgentConfigFromUrl(String troWebUrl, String appName, String configVersion,
        String type) {
        if (Pradar.isLite) {
            return new HashMap<String, Object>();
        }
        final StringBuilder url = new StringBuilder(troWebUrl).append(AGENT_CONFIG_URL);

        Map<String, String> params = new HashMap<String, String>();
        params.put("projectName", appName);
        params.put("version", configVersion);
        params.put("effectMechanism", type);//固定参数
        HttpUtils.HttpResult httpResult = HttpUtils.doPost(url.toString(), JSON.toJSONString(params));
        if (!httpResult.isSuccess()) {
            logger.error("获取控制台固定配置信息失败 url={}, result={}, 参数类型={}", url, httpResult.getResult(),
                type.equals("1") ? "动态" : "固定");
            return null;
        }
        return JSON.parseObject(httpResult.getResult(), Map.class);
    }

}
