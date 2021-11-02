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
package com.pamirs.pradar.pressurement.agent.listener;

import com.pamirs.pradar.pressurement.agent.listener.model.ShadowConsumerDisableInfo;

import java.util.List;

/**
 * @author angju
 * @date 2021/10/11 10:40
 */
public interface ShadowConsumerDisableListener {

    /**
     * 批量禁止特定的消费者
     * @param list
     * @return
     */
    boolean disableBatch(List<ShadowConsumerDisableInfo> list);

    /**
     * 禁止所有已启动的消费者
     * @return
     */
    boolean disableAll();
}
