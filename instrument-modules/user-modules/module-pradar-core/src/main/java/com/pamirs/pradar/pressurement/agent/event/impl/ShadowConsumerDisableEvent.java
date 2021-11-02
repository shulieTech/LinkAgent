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
package com.pamirs.pradar.pressurement.agent.event.impl;

import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.listener.model.ShadowConsumerDisableInfo;

import java.util.List;

/**
 * @author jiangjibo
 * @date 2021/10/9 4:25 下午
 * @description: 影子MQ消费者禁用事件
 */
public class ShadowConsumerDisableEvent implements IEvent {

    private List<ShadowConsumerDisableInfo> disableInfos;

    public ShadowConsumerDisableEvent(List<ShadowConsumerDisableInfo> disableInfos) {
        this.disableInfos = disableInfos;
    }
/**/
    @Override
    public List<ShadowConsumerDisableInfo> getTarget() {
        return disableInfos;
    }
}
