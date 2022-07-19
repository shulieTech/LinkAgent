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

import com.pamirs.pradar.cache.ClusterTestCacheWrapperKey;
import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/03/22 5:25 下午
 */
@ListenerBehavior(isFilterClusterTest = true)
public class PutAllInterceptor extends AbstractFilterInterceptor {
    public static final int MAX_POWER_OF_TWO = 1 << (Integer.SIZE - 2);

    static int capacity(int expectedSize) {
        if (expectedSize < 3) {
            return expectedSize + 1;
        }
        if (expectedSize < MAX_POWER_OF_TWO) {
            return (int) ((float) expectedSize / 0.75F + 1.0F);
        }
        return Integer.MAX_VALUE;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Object[] doGetParameter(Advice advice) {
        Object[] args = advice.getParameterArray();
        Object object = args[0];
        if (!(object instanceof Map)) {
            return args;
        }
        Map source = (Map) object;
        Map result = new HashMap(capacity(source.size()));
        Set<Entry> entrySet = source.entrySet();
        boolean replace = false;
        for (Map.Entry entry : entrySet) {
            if (entry.getKey() instanceof ClusterTestCacheWrapperKey) {
                result.put(entry.getKey(), entry.getValue());
            } else {
                ClusterTestCacheWrapperKey clusterTestCacheWrapperKey = new ClusterTestCacheWrapperKey(entry.getKey());
                result.put(clusterTestCacheWrapperKey, entry.getValue());
                replace = true;
            }
        }
        if (replace) {
            args[0] = result;
        }
        return args;
    }
}
