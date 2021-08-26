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

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.pamirs.attach.plugin.caffeine.utils.WrapEntry;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.cache.ClusterTestCacheWrapperKey;
import com.pamirs.pradar.interceptor.ModificationInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/03/22 5:25 下午
 */
public class EntrySetInterceptor extends ModificationInterceptorAdaptor {

    @Override
    public Object getResult0(Advice advice) throws Throwable {
        Object returnObj = advice.getReturnObj();
        if (!(returnObj instanceof Set)) {
            return new Object[0];
        }
        Set<Map.Entry> returnSet = (Set<Entry>)returnObj;
        if (returnSet.size() == 0) {
            return new Object[0];
        }
        Set<Map.Entry> resultSet = new HashSet();
        for (Entry entry : returnSet) {
            if (Pradar.isClusterTest()) {
                if (entry.getKey() instanceof ClusterTestCacheWrapperKey) {
                    resultSet.add(new WrapEntry(entry));
                }
            } else {
                if (!(entry.getKey() instanceof ClusterTestCacheWrapperKey)) {
                    resultSet.add(entry);
                }
            }
        }
        return resultSet;
    }
}
