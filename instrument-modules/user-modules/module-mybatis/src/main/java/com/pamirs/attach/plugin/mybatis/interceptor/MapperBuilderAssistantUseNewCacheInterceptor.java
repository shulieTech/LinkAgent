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
package com.pamirs.attach.plugin.mybatis.interceptor;

import com.pamirs.attach.plugin.mybatis.MybatisConstants;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.mapping.CacheBuilder;

import javax.annotation.Resource;
import java.util.Properties;

/**
 * @author angju
 * @date 2020/10/22 15:22
 */
public class MapperBuilderAssistantUseNewCacheInterceptor extends AroundInterceptor {
    @Resource
    protected DynamicFieldManager manager;

    @Override
    public void doAfter(Advice advice) {
        String currentNamespace = null;
        try {
            currentNamespace = Reflect.on(advice.getTarget()).get(MybatisConstants.DYNAMIC_FIELD_CURRENT_NAMESPACE);
        } catch (ReflectException e) {
            currentNamespace = manager.getDynamicField(advice.getTarget(), MybatisConstants.DYNAMIC_FIELD_CURRENT_NAMESPACE);
        }
        Object[] args = advice.getParameterArray();
        Class<? extends Cache> typeClass = (Class<? extends Cache>) args[0];
        Class<? extends Cache> evictionClass = (Class<? extends Cache>) args[1];
        Long flushInterval = (Long) args[2];
        Integer size = (Integer) args[3];
        boolean readWrite = (Boolean) args[4];
        boolean blocking = (Boolean) args[5];
        Properties props = (Properties) args[6];
        Cache ptCache = initPtCache(Pradar.CLUSTER_TEST_PREFIX + currentNamespace, typeClass, evictionClass,
                flushInterval, size, readWrite, blocking, props);
        MybatisConstants.currentName2PtCacheMap.put(currentNamespace, ptCache);
    }

    private Cache initPtCache(String currentNamespace, Class<? extends Cache> typeClass, Class<? extends Cache> evictionClass, Long flushInterval, Integer size,
                              boolean readWrite, boolean blocking, Properties props) {
        Cache ptCache = new CacheBuilder(currentNamespace)
                .implementation(valueOrDefault(typeClass, PerpetualCache.class))
                .addDecorator(valueOrDefault(evictionClass, LruCache.class))
                .clearInterval(flushInterval)
                .size(size)
                .readWrite(readWrite)
                .blocking(blocking)
                .properties(props)
                .build();
        return ptCache;
    }

    private <T> T valueOrDefault(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }
}
