/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.guava.interceptor;

import com.pamirs.pradar.AppNameUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.cache.ClusterTestCacheWrapperKey;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.flag.GuavaCacheSkipFlag;
import com.pamirs.pradar.interceptor.ResultInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author wangjian
 * @since 2021/3/9 21:19
 */
public class CacheAsMapInterceptor extends ResultInterceptorAdaptor {

    @Override
    protected Object getResult0(Advice advice) {
        if (GuavaCacheSkipFlag.skip.get()) {
            return advice.getReturnObj();
        }
        /**
         * 压测状态为关闭,如果当前为压测流量则直接报错
         */
        if (!PradarSwitcher.isClusterTestEnabled()) {
            if (Pradar.isClusterTest()) {
                throw new PressureMeasureError(PradarSwitcher.PRADAR_SWITCHER_OFF + ":" + AppNameUtils.appName());
            }
            return advice.getReturnObj();
        }
        boolean clusterTest = Pradar.isClusterTest();
        ConcurrentMap returnObj = (ConcurrentMap) advice.getReturnObj();
        Set<Map.Entry> set = returnObj.entrySet();
        ConcurrentMap concurrentMap = new ConcurrentHashMap();
        for (Map.Entry o : set) {
            if (clusterTest && o.getKey() instanceof ClusterTestCacheWrapperKey) {
                concurrentMap.put(((ClusterTestCacheWrapperKey) o.getKey()).getKey(), o.getValue());
            } else if (!clusterTest && !(o.getKey() instanceof ClusterTestCacheWrapperKey)) {
                concurrentMap.put(o.getKey(), o.getValue());
            }
        }
        return concurrentMap;
    }
}
