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
package com.pamirs.attach.plugin.shadowjob.interceptor;

import java.lang.reflect.Method;

import com.pamirs.attach.plugin.shadowjob.destory.JobDestroy;
import com.pamirs.pradar.Pradar;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

/**
 * @author angju
 * @date 2020/7/19 19:57
 */
@Destroyable(JobDestroy.class)
public class LtsInterceptor implements MethodInterceptor {
    private static Logger logger = LoggerFactory.getLogger(LtsInterceptor.class.getName());

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        Pradar.setClusterTest(true);
        logger.info("设置成了压测线程!");
        Object value = methodProxy.invokeSuper(o, objects);
        Pradar.setClusterTest(false);
        logger.info("设置成了正常线程!");
        return value;
    }
}
