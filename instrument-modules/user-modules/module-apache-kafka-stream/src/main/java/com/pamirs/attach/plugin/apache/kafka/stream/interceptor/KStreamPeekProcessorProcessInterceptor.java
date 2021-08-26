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
package com.pamirs.attach.plugin.apache.kafka.stream.interceptor;

import com.pamirs.attach.plugin.apache.kafka.stream.common.KStreamProcessorProcessTypeEnum;
import com.pamirs.attach.plugin.apache.kafka.stream.destroy.KafkaStreamDestroy;
import com.shulie.instrument.simulator.api.annotation.Destroyable;

/**
 * @author angju
 * @date 2021/5/7 21:07
 */
@Destroyable(KafkaStreamDestroy.class)
public class KStreamPeekProcessorProcessInterceptor extends AbstractKStreamProcessorProcessInterceptor {

    @Override
    protected KStreamProcessorProcessTypeEnum getActionFieldName() {
        return KStreamProcessorProcessTypeEnum.FOREACH;
    }
}
