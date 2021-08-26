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

import com.pamirs.pradar.ConfigNames;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.pressurement.agent.event.impl.WhiteListSwitchOffEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.WhiteListSwitchOnEvent;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.shulie.instrument.module.config.fetcher.config.impl.ClusterTestConfig;
import com.shulie.instrument.module.config.fetcher.config.utils.ObjectUtils;

/**
 * @author: wangjian
 * @since : 2020/9/8 17:31
 */
public class WhiteListSwitch implements IChange<Boolean, ClusterTestConfig> {

    private static WhiteListSwitch INSTANCE;

    public static WhiteListSwitch getInstance() {
        if (INSTANCE == null) {
            synchronized (WhiteListSwitch.class) {
                if (INSTANCE == null) {
                    INSTANCE = new WhiteListSwitch();
                }
            }
        }
        return INSTANCE;
    }

    public static void release() {
        INSTANCE = null;
    }

    @Override
    public Boolean compareIsChangeAndSet(ClusterTestConfig clusterTestConfig, Boolean newValue) {
        boolean b = PradarSwitcher.whiteListSwitchOn();
        if (ObjectUtils.equals(newValue, b)) {
            return Boolean.FALSE;
        }
        // 存在配置变更
        // 变更后配置更新到内存
        if (newValue) {
            WhiteListSwitchOnEvent event = new WhiteListSwitchOnEvent(this);
            EventRouter.router().publish(event);
            PradarSwitcher.turnWhiteListSwitchOn();
            clusterTestConfig.setWhiteListSwitchOn(true);
        } else {
            WhiteListSwitchOffEvent event = new WhiteListSwitchOffEvent(this);
            EventRouter.router().publish(event);
            PradarSwitcher.turnWhiteListSwitchOff();
            clusterTestConfig.setWhiteListSwitchOn(false);
        }
        return Boolean.TRUE;
    }
}
