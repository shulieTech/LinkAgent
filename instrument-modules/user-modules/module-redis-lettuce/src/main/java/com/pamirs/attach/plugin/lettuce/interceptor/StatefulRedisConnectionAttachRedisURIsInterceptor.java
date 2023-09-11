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
package com.pamirs.attach.plugin.lettuce.interceptor;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.attach.plugin.lettuce.LettuceConstants;
import com.pamirs.attach.plugin.lettuce.destroy.LettuceDestroy;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;

import javax.annotation.Resource;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/9/7 11:44 上午
 */
@Destroyable(LettuceDestroy.class)
public class StatefulRedisConnectionAttachRedisURIsInterceptor extends AroundInterceptor {
    @Resource
    protected DynamicFieldManager manager;

    @Override
    public void doAfter(Advice advice) {
        Object target = advice.getTarget();
        Object result = advice.getReturnObj();

        final List<Object> list = manager.getDynamicField(target, LettuceConstants.DYNAMIC_FIELD_REDIS_URIS);
        if(list != null && !list.isEmpty()) {
            if(result instanceof Proxy){
                result = ReflectionUtils.getFieldValues(result, "h", "asyncApi");
            }
            manager.setDynamicField(result, LettuceConstants.DYNAMIC_FIELD_REDIS_URIS, list);
        }
    }
}
