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
package com.pamirs.attach.plugin.apache.kafka.stream.obj;

import com.pamirs.pradar.Pradar;
import org.apache.kafka.streams.kstream.ForeachAction;

/**
 * @author angju
 * @date 2021/5/7 21:16
 */
public class PTForeachAction implements ForeachAction {

    private ForeachAction busForeachAction;

    public PTForeachAction(ForeachAction busForeachAction){
        this.busForeachAction = busForeachAction;
    }

    @Override
    public void apply(Object key, Object value) {
        Pradar.setClusterTest(true);
        busForeachAction.apply(key, value);
        Pradar.setClusterTest(false);
    }
}
