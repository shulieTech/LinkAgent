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
package com.shulie.instrument.simulator.core.extension;

import java.util.ArrayList;
import java.util.List;

import com.shulie.instrument.simulator.api.extension.AdviceListenerWrapBuilder;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/07/29 11:46 上午
 */
public class GlobalAdviceWrapBuilders {

    private static List<AdviceListenerWrapBuilder> adviceListenerWrapBuilders = new ArrayList<AdviceListenerWrapBuilder>();

    public synchronized static void addAdviceListenerWrapBuilder(AdviceListenerWrapBuilder adviceListenerWrapBuilder) {
        GlobalAdviceWrapBuilders.adviceListenerWrapBuilders.add(adviceListenerWrapBuilder);
    }

    synchronized static List<AdviceListenerWrapBuilder> getAdviceListenerWrapBuilders() {
        return GlobalAdviceWrapBuilders.adviceListenerWrapBuilders;
    }

}
