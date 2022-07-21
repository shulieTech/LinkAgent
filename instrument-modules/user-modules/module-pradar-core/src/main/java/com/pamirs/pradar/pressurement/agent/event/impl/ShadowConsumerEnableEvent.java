/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pamirs.pradar.pressurement.agent.event.impl;

import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.listener.model.ShadowConsumerEnableInfo;

import java.util.List;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/7/14 2:04 下午
 */
public class ShadowConsumerEnableEvent implements IEvent {

    private List<ShadowConsumerEnableInfo> enableList;

    public ShadowConsumerEnableEvent(List<ShadowConsumerEnableInfo> enableList) {
        this.enableList = enableList;
    }

    @Override
    public List<ShadowConsumerEnableInfo> getTarget() {
        return enableList;
    }
}