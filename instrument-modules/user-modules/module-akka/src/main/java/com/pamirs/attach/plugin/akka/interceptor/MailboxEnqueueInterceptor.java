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
package com.pamirs.attach.plugin.akka.interceptor;

import akka.dispatch.Envelope;
import com.pamirs.attach.plugin.akka.AkkaConstants;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import org.apache.commons.lang.ArrayUtils;

import javax.annotation.Resource;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/5/28 6:02 下午
 */
public class MailboxEnqueueInterceptor extends AroundInterceptor {
    @Resource
    private DynamicFieldManager manager;

    @Override
    public void doBefore(Advice advice) throws Throwable {
        Object[] args = advice.getParameterArray();
        if (ArrayUtils.isEmpty(args) || args.length != 2) {
            return;
        }
        Object target = args[1];
        if (!(target instanceof Envelope)) {
            return;
        }
        if (manager.hasDynamicField(target, AkkaConstants.DYNAMIC_FIELD_CONTEXT)) {
            return;
        }

        if (!Pradar.isEmptyContext() || Pradar.isClusterTest()) {
            manager.setDynamicField(target, AkkaConstants.DYNAMIC_FIELD_CONTEXT, Pradar.getInvokeContextMap());
        }
    }
}
