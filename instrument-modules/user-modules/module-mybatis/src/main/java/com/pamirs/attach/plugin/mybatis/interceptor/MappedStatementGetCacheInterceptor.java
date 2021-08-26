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
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import org.apache.ibatis.cache.Cache;

import javax.annotation.Resource;

/**
 * @author angju
 * @date 2020/10/22 15:33
 */
public class MappedStatementGetCacheInterceptor extends CutoffInterceptorAdaptor {
    @Resource
    protected DynamicFieldManager manager;

    @Override
    public CutOffResult cutoff0(Advice advice) {
        if (!Pradar.isClusterTest()) {
            return CutOffResult.passed();
        }
        try {
            Object cache = Reflect.on(advice.getTarget()).get(MybatisConstants.DYNAMIC_FIELD_CACHE);
            if (cache == null) {
                return CutOffResult.passed();
            }
        } catch (ReflectException e) {
            return CutOffResult.passed();
        }

        String currentNamespace = null;
        try {
            currentNamespace = Reflect.on(advice.getTarget()).get(MybatisConstants.DYNAMIC_FIELD_CURRENT_NAMESPACE);
        } catch (ReflectException e) {
            currentNamespace = manager.getDynamicField(advice.getTarget(), MybatisConstants.DYNAMIC_FIELD_CURRENT_NAMESPACE);
        }
        Cache cache = MybatisConstants.currentName2PtCacheMap.get(currentNamespace);
        return CutOffResult.cutoff(cache);
    }
}
