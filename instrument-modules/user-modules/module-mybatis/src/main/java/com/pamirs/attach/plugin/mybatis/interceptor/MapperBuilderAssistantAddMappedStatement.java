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
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;

import javax.annotation.Resource;

/**
 * @author angju
 * @date 2020/10/22 22:08
 */
public class MapperBuilderAssistantAddMappedStatement extends AroundInterceptor {
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

        try {
            Reflect.on(advice.getReturnObj()).set(MybatisConstants.DYNAMIC_FIELD_CURRENT_NAMESPACE, currentNamespace);
        } catch (ReflectException e) {
            manager.setDynamicField(advice.getReturnObj(), MybatisConstants.DYNAMIC_FIELD_CURRENT_NAMESPACE, currentNamespace);
        }
    }
}
