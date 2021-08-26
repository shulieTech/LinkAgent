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
package com.pamirs.attach.plugin.hystrix.interceptor;

import com.pamirs.attach.plugin.hystrix.HystrixConstants;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;

import javax.annotation.Resource;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/3/17 2:08 下午
 */
public class InvokeInterceptor extends AroundInterceptor {
    @Resource
    protected DynamicFieldManager manager;

    @Override
    public void doBefore(Advice advice) throws Throwable {
        Object threadId = manager.getDynamicField(advice.getTarget(), HystrixConstants.DYNAMIC_FILED_THREAD_ID);
        if (threadId == null) {
            return;
        }
        if (!(threadId instanceof Long)) {
            return;
        }
        long tid = (Long) threadId;
        if (Thread.currentThread().getId() != tid) {
            Object context = manager.getDynamicField(advice.getTarget(), HystrixConstants.DYNAMIC_FILED_INVOKE_CONTEXT);
            Pradar.setInvokeContext(context);
        }
    }

    @Override
    public void doAfter(Advice advice) throws Throwable {
        try {
            Object threadId = manager.getDynamicField(advice.getTarget(), HystrixConstants.DYNAMIC_FILED_THREAD_ID);
            if (threadId == null) {
                return;
            }
            if (!(threadId instanceof Long)) {
                return;
            }
            long tid = (Long) threadId;
            /**
             * 线程不相等的时候才清理上下文，防止埋点错误导致把正常的上下文给清除掉导致报错
             */
            if (Thread.currentThread().getId() != tid) {
                Pradar.clearInvokeContext();
            }
        } finally {
            manager.removeAll(advice.getTarget());
        }
    }

    @Override
    public void doException(Advice advice) throws Throwable {
        try {
            Object threadId = manager.getDynamicField(advice.getTarget(), HystrixConstants.DYNAMIC_FILED_THREAD_ID);
            if (threadId == null) {
                return;
            }
            if (!(threadId instanceof Long)) {
                return;
            }
            long tid = (Long) threadId;
            /**
             * 线程不相等的时候才清理上下文，防止埋点错误导致把正常的上下文给清除掉导致报错
             */
            if (Thread.currentThread().getId() != tid) {
                Pradar.clearInvokeContext();
            }
        } finally {
            manager.removeAll(advice.getTarget());
        }
    }
}
