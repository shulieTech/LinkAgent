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
package com.shulie.instrument.module.config.fetcher.config.event.model;

import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.pressurement.agent.event.impl.ClusterTestSwitchOffEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ClusterTestSwitchOnEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.SilenceSwitchOffEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.SilenceSwitchOnEvent;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.shulie.instrument.module.config.fetcher.config.impl.ClusterTestConfig;
import com.shulie.instrument.module.config.fetcher.config.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SilenceSwitch implements IChange<Boolean, ClusterTestConfig> {
    private final static Logger LOGGER = LoggerFactory.getLogger(SilenceSwitch.class);
    private static SilenceSwitch INSTANCE;

    public static SilenceSwitch getInstance() {
        if (INSTANCE == null) {
            synchronized (SilenceSwitch.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SilenceSwitch();
                }
            }
        }
        return INSTANCE;
    }

    public static void release() {
        INSTANCE = null;
    }

    @Override
    public Boolean compareIsChangeAndSet(ClusterTestConfig config, Boolean newValue) {
        boolean silenceSwitchOn = PradarSwitcher.silenceSwitchOn();
        if (ObjectUtils.equals(newValue, silenceSwitchOn)) {
            return Boolean.FALSE;
        }
        // 存在配置变更
        // 变更后配置更新到内存
        if (newValue) {
            SilenceSwitchOnEvent event = new SilenceSwitchOnEvent(this);
            EventRouter.router().publish(event);
            PradarSwitcher.turnSilenceSwitchOn();
            config.setSilenceSwitchOn(true);
        } else {
            SilenceSwitchOffEvent event = new SilenceSwitchOffEvent(this);
            EventRouter.router().publish(event);
            PradarSwitcher.turnSilenceSwitchOff();
            config.setSilenceSwitchOn(false);
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("publish silence switch config successful. config={}", newValue);
        }
        return Boolean.TRUE;
    }
}
