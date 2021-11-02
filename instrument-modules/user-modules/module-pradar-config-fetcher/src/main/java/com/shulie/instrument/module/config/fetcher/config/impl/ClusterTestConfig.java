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
package com.shulie.instrument.module.config.fetcher.config.impl;


import com.shulie.instrument.module.config.fetcher.config.AbstractConfig;
import com.shulie.instrument.module.config.fetcher.config.event.FIELDS;
import com.shulie.instrument.module.config.fetcher.config.resolver.ConfigResolver;

/**
 * 压测总配置项
 *
 * @author shiyajian
 * @since 2020-08-11
 */
public class ClusterTestConfig extends AbstractConfig<ClusterTestConfig> {
    /**
     * 当以下开关配置被设置之后才需要同步该开关配置，否则不同步该开关配置，防止因为没有拉到开关配置将老配置刷掉
     */
    /**
     * 是否需要同步全局开关配置
     */
    private boolean syncGlobalSwitch;
    /**
     * 是否需要同步白名单开关配置
     */
    private boolean syncWhiteListSwitch;
    /**
     * 是否需要同步静默开关配置
     */
    private boolean syncSilenceSwitch;


    /**
     * 全局压测总开关
     */
    private boolean globalSwitchOn;

    /**
     * 静默开关
     */
    private boolean silenceSwitchOn;

    /**
     * 白名单总开关
     */
    private boolean whiteListSwitchOn;

    public ClusterTestConfig(ConfigResolver resolver) {
        super(resolver);
        this.globalSwitchOn = false;
        this.whiteListSwitchOn = false;
        this.silenceSwitchOn = false;
        this.type = ClusterTestConfig.class;
    }

    public void setSyncGlobalSwitch(boolean syncGlobalSwitch) {
        this.syncGlobalSwitch = syncGlobalSwitch;
    }

    public void setSyncWhiteListSwitch(boolean syncWhiteListSwitch) {
        this.syncWhiteListSwitch = syncWhiteListSwitch;
    }

    public void setSyncSilenceSwitch(boolean syncSilenceSwitch) {
        this.syncSilenceSwitch = syncSilenceSwitch;
    }

    @Override
    public void trigger(FIELDS... fields) {
        if (fields == null || fields.length == 0) {
            return;
        }
        ClusterTestConfig clusterTestConfig = (ClusterTestConfig) resolver.fetch(fields);
        if (clusterTestConfig != null) {
            refreshFields(clusterTestConfig, fields);
        }
    }

    public boolean isGlobalSwitchOn() {
        return globalSwitchOn;
    }

    public boolean isSilenceSwitchOn() {
        return silenceSwitchOn;
    }

    public void setGlobalSwitchOn(boolean globalSwitchOn) {
        this.globalSwitchOn = globalSwitchOn;
        this.syncGlobalSwitch = true;
    }

    public void setSilenceSwitchOn(boolean silenceSwitchOn) {
        System.setProperty("pradar.switch.silence", String.valueOf(silenceSwitchOn));
        System.setProperty("ttl.disabled", String.valueOf(silenceSwitchOn));
        this.silenceSwitchOn = silenceSwitchOn;
        this.syncSilenceSwitch = true;
    }

    public boolean isWhiteListSwitchOn() {
        return whiteListSwitchOn;
    }

    public void setWhiteListSwitchOn(boolean whiteListSwitchOn) {
        this.whiteListSwitchOn = whiteListSwitchOn;
        syncWhiteListSwitch = true;
    }

    public void refreshFields(ClusterTestConfig clusterTestConfig, FIELDS... fields) {
        if (fields == null || fields.length == 0) {
            return;
        }
        for (FIELDS field : fields) {
            switch (field) {
                case GLOBAL_SWITCHON:
                    if (clusterTestConfig.syncGlobalSwitch) {
                        change(FIELDS.GLOBAL_SWITCHON, clusterTestConfig.isGlobalSwitchOn());
                    }
                    break;
                case WHITE_LIST_SWITCHON:
                    if (clusterTestConfig.syncWhiteListSwitch) {
                        change(FIELDS.WHITE_LIST_SWITCHON, clusterTestConfig.isWhiteListSwitchOn());
                    }
                    break;
                case SILENCE_SWITCH:
                    if (clusterTestConfig.syncSilenceSwitch) {
                        change(FIELDS.SILENCE_SWITCH, clusterTestConfig.isSilenceSwitchOn());
                    }
                    break;
            }
        }

    }

    @Override
    public void refresh(ClusterTestConfig clusterTestConfig) {
        if (clusterTestConfig.syncGlobalSwitch) {
            change(FIELDS.GLOBAL_SWITCHON, clusterTestConfig.isGlobalSwitchOn());
        }
        if (clusterTestConfig.syncWhiteListSwitch) {
            change(FIELDS.WHITE_LIST_SWITCHON, clusterTestConfig.isWhiteListSwitchOn());
        }
        if (clusterTestConfig.syncSilenceSwitch) {
            change(FIELDS.SILENCE_SWITCH, clusterTestConfig.isSilenceSwitchOn());
        }
    }
}
