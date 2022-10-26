/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.shulie.instrument.module.isolation.common;

import com.shulie.instrument.simulator.api.util.BehaviorDescriptor;
import io.shulie.instrument.module.isolation.exception.IsolationRuntimeException;
import io.shulie.instrument.module.isolation.resource.ShadowResourceLifecycle;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/18 13:56
 */
public class ShadowResourceLifecycleModule {

    private final ShadowResourceLifecycle shadowResourceLifecycle;
    private final Map<String, Method> methodMap;

    public ShadowResourceLifecycleModule(ShadowResourceLifecycle shadowResourceLifecycle) {
        this.shadowResourceLifecycle = shadowResourceLifecycle;
        methodMap = new ConcurrentHashMap<String, Method>();
    }

    public Method fetchMethod(String method, String methodDesc) {
        //todo@langyi 优化性能和内存.
        Object ptTarget = shadowResourceLifecycle.getTarget();
        String key = toString(ptTarget);
        String keyTemp = keyOfMethod(method, methodDesc);
        String methodKey = key + keyTemp;
        Method m = methodMap.get(methodKey);
        if (m == null) {
            prepareMethodMap(key, ptTarget.getClass());
            m = methodMap.get(methodKey);
        }
        if (m == null) {
            throw new IsolationRuntimeException("[isolation]can not found method {}" + methodKey + " in " + ptTarget);
        }
        return m;
    }

    private void prepareMethodMap(String key, Class c) {
        if (c == null) {
            return;
        }
        for (final Method temp : c.getDeclaredMethods()) {
            temp.setAccessible(true);
            methodMap.put(key + keyOfMethod(temp.getName(), new BehaviorDescriptor(temp).getDescriptor()), temp);
        }
        prepareMethodMap(key, c.getSuperclass());
    }

    private String keyOfMethod(String method, String methodDesc) {
        return ":" + method + methodDesc;
    }

    public ShadowResourceLifecycle getShadowResourceLifecycle() {
        return shadowResourceLifecycle;
    }

    public Map<String, Method> getMethodMap() {
        return methodMap;
    }

    public String toString(Object obj) {
        return obj.getClass().getName() + "@" + Integer.toHexString(obj.hashCode());
    }
}
