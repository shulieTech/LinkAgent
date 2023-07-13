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

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.attach.plugin.shadowjob.ShadowJobConstants;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.pressurement.agent.shared.util.PradarSpringUtil;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @package: com.pamirs.attach.plugin.shadowjob.interceptor
 * @Date 2020-03-18 19:31
 */
public class SpringContextInterceptor extends AroundInterceptor {

    private static AtomicBoolean isInited = new AtomicBoolean(false);

    /**
     * 该方法可优化
     * 现状：spring上下文每次刷新时都会执行该方法。
     *
     * @param advice
     */
    @Override
    public void doBefore(Advice advice) {
        Object target = advice.getTarget();
        if (!isInited.compareAndSet(false, true)) {
            return;
        }
        ConfigurableApplicationContext applicationContext = null;
        if (ReflectionUtils.existsField(target, ShadowJobConstants.DYNAMIC_FIELD_APPLICATION_CONTEXT)) {
            applicationContext = ReflectionUtils.get(target, ShadowJobConstants.DYNAMIC_FIELD_APPLICATION_CONTEXT);
        }
        if (applicationContext == null) {
            isInited.set(false);
            return;
        }
        PradarSpringUtil.refreshBeanFactory(applicationContext);

    }
}
