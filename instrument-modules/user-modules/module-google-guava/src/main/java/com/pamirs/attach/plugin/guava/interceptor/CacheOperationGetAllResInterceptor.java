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
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

import java.util.*;

/**
 * @author wangjian
 * @since 2021/3/9 14:54
 */
public class CacheOperationGetAllResInterceptor extends ParametersWrapperInterceptorAdaptor {

    @Override
    protected Object[] getParameter0(Advice advice) throws Throwable {
        if (GuavaCacheSkipFlag.skip.get()) {
            return advice.getParameterArray();
        }

        /**
         * 压测状态为关闭,如果当前为压测流量则直接报错
         */
        if (!PradarSwitcher.isClusterTestEnabled()) {
            if (Pradar.isClusterTest()) {
                throw new PressureMeasureError(PradarSwitcher.PRADAR_SWITCHER_OFF + ":" + AppNameUtils.appName());
            }
            return advice.getParameterArray();
        }
        if (!Pradar.isClusterTest()) {
            // 非压测流量
            return advice.getParameterArray();
        }
        Object[] parameterArray = advice.getParameterArray();
        if (null == parameterArray || parameterArray.length == 0) {
            return advice.getParameterArray();
        }
        if (parameterArray[0] instanceof Map) {
            Map o = (Map) parameterArray[0];
            Set<Map.Entry> set = o.entrySet();
            LinkedHashMap<Object, Object> objectObjectLinkedHashMap = new LinkedHashMap();
            for (Map.Entry o1 : set) {
                if (o1.getKey() instanceof ClusterTestCacheWrapperKey) {
                    objectObjectLinkedHashMap.put(((ClusterTestCacheWrapperKey) o1.getKey()).getKey(), o1.getValue());
                } else {
                    objectObjectLinkedHashMap.put(o1.getKey(), o1.getValue());
                }
            }
            parameterArray[0] = objectObjectLinkedHashMap;
        } else if (parameterArray[0] instanceof Iterable) {
            Iterable<? extends Map.Entry> o = (Iterable) parameterArray[0];
            if (null != o) {
                ArrayList<? extends Map.Entry> objects = new ArrayList();
                Iterator iterator = o.iterator();
                HashMap objectObjectHashMap = new HashMap();
                while (iterator.hasNext()) {
                    Map.Entry next = (Map.Entry) iterator.next();
                    Object key = next.getKey();
                    if (key instanceof ClusterTestCacheWrapperKey) {
                        objectObjectHashMap.put(((ClusterTestCacheWrapperKey) key).getKey(), next.getValue());
                    } else {
                        objectObjectHashMap.put(key, next.getValue());
                    }
                }
                objects.addAll(objectObjectHashMap.entrySet());
                parameterArray[0] = objects;
            }
        }
        return parameterArray;
    }

}
