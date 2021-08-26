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
package com.pamirs.attach.plugin.shadowjob.obj;

import com.pamirs.pradar.Pradar;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * @author angju
 * @date 2021/3/23 17:04
 */
public class MethodInterceptorImpl implements MethodInterceptor {
    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {

//                        if (!StringUtils.equals(method.getName(), "execute")) {
//                            return proxy.invokeSuper(obj, args);
//                        }
//                        if (method.getParameterTypes().length != 1) {
//                            return proxy.invokeSuper(obj, args);
//                        }
//                        if (!StringUtils.equals(method.getParameterTypes()[0].getName(), "com.dangdang.ddframe.job.api.ShardingContext")) {
//                            return proxy.invokeSuper(obj, args);
//                        }
            Pradar.setClusterTest(true);
            System.out.println("打印:" + Pradar.isClusterTest());
            try {
                return methodProxy.invokeSuper(o, objects);
            } finally {
                Pradar.setClusterTest(false);
            }

    }
}
