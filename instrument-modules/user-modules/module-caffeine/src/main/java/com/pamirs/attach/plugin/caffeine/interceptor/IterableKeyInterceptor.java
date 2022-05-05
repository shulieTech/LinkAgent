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
package com.pamirs.attach.plugin.caffeine.interceptor;

import java.util.ArrayList;
import java.util.List;

import com.pamirs.pradar.cache.ClusterTestCacheWrapperKey;
import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/03/22 5:25 下午
 */
@ListenerBehavior(isFilterClusterTest = true)
public class IterableKeyInterceptor extends AbstractFilterInterceptor {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Object[] doGetParameter(Advice advice) {
        Object[] args = advice.getParameterArray();
        Object object = args[0];
        if (!(object instanceof Iterable)) {
            return args;
        }
        List list = new ArrayList();
        for (Object o : (Iterable)object) {
            if (o instanceof ClusterTestCacheWrapperKey) {
                list.add(o);
            } else {
                list.add(new ClusterTestCacheWrapperKey(o));
            }
        }
        args[0] = list;
        return args;
    }
}
