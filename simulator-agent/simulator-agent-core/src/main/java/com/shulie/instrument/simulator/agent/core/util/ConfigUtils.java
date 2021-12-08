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
package com.shulie.instrument.simulator.agent.core.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/20 3:19 下午
 */
public class ConfigUtils {
    private final static Logger logger = LoggerFactory.getLogger(ConfigUtils.class);
    private static final String AND_DELIMITER = "&";
    private static final String EQUAL_DELIMITER = "=";
    private static final String QUESTION_DELIMITER = "?";
    private static final String CHARSET = "utf-8";
    private static String tenantAppKey = "";
    private static String userId = "";
    private static String envCode = "";
    private static String agentId = "";
    private static String appName = "";

    //配置控制台地址
    private static final String AGENT_CONFIG_URL = "/api/fast/agent/access/config/agentConfig";


    public static String doConfig(String url, Map<String, String> headers) {
        if (StringUtils.startsWith(url, "http://") || StringUtils.startsWith(url, "https://")) {
            return HttpUtils.doGet(url, headers);
        } else {
            try {
                Map<String, String> args = Collections.EMPTY_MAP;
                if (StringUtils.indexOf(url, QUESTION_DELIMITER) != -1) {
                    args = createQueryParams(
                        StringUtils.substring(url, StringUtils.indexOf(url, QUESTION_DELIMITER) + 1));
                    url = StringUtils.substring(url, 0, StringUtils.indexOf(url, QUESTION_DELIMITER));
                }
                File file = new File(url);
                return FileUtils.readFileToString(file, "UTF-8");
            } catch (IOException e) {
                logger.error("Get config from remote server err. {}", url, e);
                throw new RuntimeException("Get config from remote server err. " + url, e);
            }
        }
    }

    private static Map<String, String> createQueryParams(String query) throws UnsupportedEncodingException {
        Map<String, String> result = new HashMap<String, String>();
        if (StringUtils.isBlank(query)) {
            return result;
        }

        String[] queryParams = query.split(AND_DELIMITER);
        if (ArrayUtils.isEmpty(queryParams)) {
            return result;
        }
        for (String qParam : queryParams) {
            if (qParam.indexOf(EQUAL_DELIMITER) == -1) {
                continue;
            }
            String[] param = qParam.split(EQUAL_DELIMITER);
            String key = URLDecoder.decode(param[0], CHARSET);
            String value = param.length > 1 ? URLDecoder.decode(param[1], CHARSET) : null;
            result.put(key, value);
        }
        return result;
    }

    /**
     * 获取控制台的固定配置信息
     */
    public static Map<String, Object> getFixedAgentConfigFromUrl(String troWebUrl, String appName, String configVersion,
        Map<String, String> headers) {
        return getAgentConfigFromUrl(troWebUrl, appName, configVersion, "0", headers);
    }

    /**
     * 获取控制台的动态配置信息
     */
    public static Map<String, Object> getDynamicAgentConfigFromUrl(String troWebUrl, String appName,
        String configVersion, Map<String, String> headers) {
        return getAgentConfigFromUrl(troWebUrl, appName, configVersion, "1", headers);
    }

    private static Map<String, Object> getAgentConfigFromUrl(String troWebUrl, String appName, String configVersion,
        String type, Map<String, String> headers) {
        final StringBuilder url = new StringBuilder(troWebUrl).append(AGENT_CONFIG_URL);

        Map<String, String> params = new HashMap<String, String>();
        params.put("projectName", appName);
        params.put("version", configVersion);
        params.put("effectMechanism", type);//固定参数
        HttpUtils.HttpResult httpResult = HttpUtils.doPost(url.toString(), headers, JSON.toJSONString(params));
        if (httpResult == null || !httpResult.isSuccess()) {
            logger.error("获取控制台配置信息失败 url={}, result={}, 参数类型={}", url,
                httpResult == null ? "null,接口请求异常" : httpResult.getResult(),
                type.equals("1") ? "动态" : "固定");
            return null;
        }
        return JSON.parseObject(httpResult.getResult(), Map.class);
    }

    public static String getTenantAppKey() {
        return tenantAppKey;
    }

    public static void setTenantAppKey(String tenantAppKey) {
        ConfigUtils.tenantAppKey = tenantAppKey;
    }

    public static String getAgentId() {
        return agentId;
    }

    public static void setAgentId(String agentId) {
        ConfigUtils.agentId = agentId;
    }

    public static String getAppName() {
        return appName;
    }

    public static void setAppName(String appName) {
        ConfigUtils.appName = appName;
    }

    public static String getUserId() {
        return userId;
    }

    public static void setUserId(String userId) {
        ConfigUtils.userId = userId;
    }

    public static String getEnvCode() {
        return envCode;
    }

    public static void setEnvCode(String envCode) {
        ConfigUtils.envCode = envCode;
    }
}
