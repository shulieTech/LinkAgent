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
package com.shulie.instrument.module.config.fetcher.config.resolver.http;

import com.alibaba.fastjson.JSON;
import com.pamirs.pradar.AppNameUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.common.HttpUtils;
import com.pamirs.pradar.pressurement.agent.event.impl.ClusterTestSwitchOffEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ClusterTestSwitchOnEvent;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.SimulatorDynamicConfig;
import com.pamirs.pradar.pressurement.base.util.PropertyUtil;
import com.shulie.instrument.module.config.fetcher.config.event.FIELDS;
import com.shulie.instrument.module.config.fetcher.config.impl.ClusterTestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author shiyajian
 * @since 2020-08-11
 */
public class ClusterTestConfigHttpResolver extends AbstractHttpResolver<ClusterTestConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTestConfigHttpResolver.class.getName());

    public ClusterTestConfigHttpResolver(int interval, TimeUnit timeUnit) {
        super("cluster-test-config-fetch-scheduled", interval, timeUnit);
    }

    private static final String APP_PRESSURE_SWITCH_STATUS = "/api/application/center/app/switch/agent";
    private static final String APP_WHITE_LIST_SWITCH_STATUS = "/api/global/switch/whitelist";
    private static final String APP_CONFIG_CALLBACK = "/api/agent/push/application/config";

    private static final String CLOSE = "CLOSED";
    private static final String SWITCH_STATUS = "switchStatus";
    private static final String SILENCE_SWITCH_STATUS = "silenceSwitchOn";

    private AtomicBoolean pradarSwitchProcessing = new AtomicBoolean(false);

    @Override
    public ClusterTestConfig fetch() {
        // 获取配置
        String troControlWebUrl = PropertyUtil.getTroControlWebUrl();
        ClusterTestConfig clusterTestConfig = new ClusterTestConfig(this);
        /**
         * 初始化全局开关配置
         * premain初始化时 close Listener、open Listener都是0
         * 如果全局开关拉取不到则默认开关为打开，会向全局发开关开启事件
         * 静默开关默认为关闭
         */
        getApplicationSwitcher(troControlWebUrl, clusterTestConfig);
        // 白名单开关配置
        getWhiteListSwitcher(troControlWebUrl, clusterTestConfig);
        return clusterTestConfig;
    }

    @Override
    public ClusterTestConfig fetch(FIELDS... fields) {
        String troControlWebUrl = PropertyUtil.getTroControlWebUrl();
        ClusterTestConfig clusterTestConfig = new ClusterTestConfig(this);
        if (fields == null || fields.length == 0) {
            return null;
        }

        for (FIELDS field : fields) {
            switch (field) {
                case GLOBAL_SWITCHON:
                    /**
                     * 初始化全局开关配置
                     * premain初始化时 close Listener、open Listener都是0
                     * 如果全局开关拉取不到则默认开关为打开，会向全局发开关开启事件
                     */
                    getApplicationSwitcher(troControlWebUrl, clusterTestConfig);
                    break;
                case WHITE_LIST_SWITCHON:
                    // 白名单开关配置
                    getWhiteListSwitcher(troControlWebUrl, clusterTestConfig);
                    break;
            }
        }

        return clusterTestConfig;
    }

    /**
     * 白名单开关
     */
    private void getWhiteListSwitcher(String troWebUrl, ClusterTestConfig clusterTestConfig) {

        try {
            if (null == troWebUrl || "".equals(troWebUrl)) {
                troWebUrl = PropertyUtil.getTroControlWebUrl();
            }

            String url = troWebUrl + APP_WHITE_LIST_SWITCH_STATUS;
            HttpUtils.HttpResult httpResult = HttpUtils.doGet(url);
            if (!httpResult.isSuccess()) {
                LOGGER.warn(
                        "SIMULATOR: [FetchConfig] White list switcher status response error. status: {}, result: {}! tro "
                                + "url is {}",
                        httpResult.getStatus(), httpResult.getResult(), url);
                clusterTestConfig.setWhiteListSwitchOn(PradarSwitcher.whiteListSwitchOn());
                return;
            }
            Map<String, Object> resultMap = JSON.parseObject(httpResult.getResult());
            Map<String, Object> map = (Map<String, Object>) resultMap.get("data");
            if (map != null && map.get(SWITCH_STATUS) != null) {
                String status = (String) map.get(SWITCH_STATUS);
                clusterTestConfig.setWhiteListSwitchOn(!CLOSE.equals(status));
            }
        } catch (Throwable e) {
            LOGGER.warn("SIMULATOR: [FetchConfig] Admin console Init Failure!", e);
            clusterTestConfig.setWhiteListSwitchOn(PradarSwitcher.whiteListSwitchOn());

        }
    }

    /**
     * 加载控制台数据
     *
     * @param troWebUrl
     * @return 返回加载控制台配置成功还是失败
     */
    private void getApplicationSwitcher(String troWebUrl, ClusterTestConfig clusterTestConfig) {

        try {
            if (null == troWebUrl || "".equals(troWebUrl)) {
                troWebUrl = PropertyUtil.getTroControlWebUrl();
            }

            String url = troWebUrl + APP_PRESSURE_SWITCH_STATUS;
            HttpUtils.HttpResult httpResult = HttpUtils.doGet(url);
            if (!httpResult.isSuccess()) {
                LOGGER.warn(String.format(
                        "SIMULATOR: [FetchConfig] Admin console http response error. status: %s, result: %s! tro url is %s",
                        httpResult.getStatus(), httpResult.getResult(), url));
                return;
            }
            Map<String, Object> resultMap = JSON.parseObject(httpResult.getResult());
            Map<String, Object> map = (Map<String, Object>) resultMap.get("data");
            if (map != null && map.get(SWITCH_STATUS) != null) {
                String status = (String) map.get(SWITCH_STATUS);
                clusterTestConfig.setGlobalSwitchOn(!CLOSE.equals(status));
            }
            if (map != null && map.get(SILENCE_SWITCH_STATUS) != null) {
                // 全局静默开关
                boolean open = !CLOSE.equals(map.get(SILENCE_SWITCH_STATUS));
                SimulatorDynamicConfig dynamicConfig = GlobalConfig.getInstance().getSimulatorDynamicConfig();
                dynamicConfig.setBlockedPollAppConfig(open);

                // 单个应用静默开关
                boolean silenceSwitchOn = dynamicConfig.isSingleSilenceSwitchOn();
                clusterTestConfig.setSilenceSwitchOn(open || silenceSwitchOn);
            }

        } catch (Throwable e) {
            LOGGER.warn("SIMULATOR: [FetchConfig] Admin console Init Failure!", e);
            clusterTestConfig.setGlobalSwitchOn(PradarSwitcher.clusterTestSwitchOn());
            clusterTestConfig.setSilenceSwitchOn(PradarSwitcher.silenceSwitchOn());
        } finally {
            /**
             * 静默状态配置变更callback,不关心成功or失败
             *  后面配置多了再抽出来做个统一的配置上报
             */
            String callbackUrl = troWebUrl + APP_CONFIG_CALLBACK;
            Map<String, Object> param = new HashMap<String, Object>();
            List<Map<String, Object>> configList = new ArrayList<Map<String, Object>>();
            Map<String, Object> configMap = new HashMap<String, Object>();
            configMap.put("key", "silenceSwitchOn");
            configMap.put("value", String.valueOf(clusterTestConfig.isSilenceSwitchOn()));
            configMap.put("bizType", 0);
            configList.add(configMap);
            param.put("applicationName", AppNameUtils.appName());
            param.put("agentId", Pradar.getAgentId(false));
            param.put("globalConf", configList);
            HttpUtils.doPost(callbackUrl, JSON.toJSONString(param));
        }
    }

    /**
     * 压测开关开启,执行开关前，判断开关执行状态
     */
    private void pradarSwitchOpen() {
        try {
            ErrorReporter.getInstance().clear();
            ClusterTestSwitchOnEvent event = new ClusterTestSwitchOnEvent(this);
            boolean isSuccess = EventRouter.router().publish(event);
            if (!isSuccess) {
                PradarSwitcher.turnClusterTestSwitchOff();
            }
        } catch (Throwable e) {
            LOGGER.warn("SIMULATOR: [FetchConfig] switch do opened has error", e);
            /**
             * 将开关重置为关闭
             */
            PradarSwitcher.turnClusterTestSwitchOff();
        } finally {
            pradarSwitchProcessing.set(false);
        }
    }

    /**
     * 压测开关开启,执行开关前，判断开关执行状态
     */
    private void pradarSwitchClosed() {
        try {
            //执行前清空异常信息
            ErrorReporter.getInstance().clear();
            ClusterTestSwitchOffEvent event = new ClusterTestSwitchOffEvent(this);
            boolean isSuccess = EventRouter.router().publish(event);
            if (!isSuccess) {
                PradarSwitcher.turnClusterTestSwitchOn();
            }
        } catch (Throwable e) {
            LOGGER.warn("SIMULATOR: [FetchConfig] switch do closed has error", e);
            /**
             * 关闭失败则重置压测开关
             */
            PradarSwitcher.turnClusterTestSwitchOn();
        } finally {
            pradarSwitchProcessing.set(false);
        }
    }

}
