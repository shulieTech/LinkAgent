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
package com.shulie.instrument.simulator.thread.interceptor;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import com.shulie.instrument.simulator.thread.ThreadConstants;

import javax.annotation.Resource;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/1/22 8:12 下午
 */
public class RunnableRunInterceptor extends AroundInterceptor {
    @Resource
    protected DynamicFieldManager manager;

    @Override
    public void doBefore(Advice advice) throws Throwable {
        /**
         * 如果当前执行有上下文,则不再设置新的上下文
         */
        if (!Pradar.isEmptyContext()) {
            manager.setDynamicField(advice.getTarget(), ThreadConstants.DYNAMIC_FIELD_HAS_CONTEXT, true);
            return;
        }
        Object context = manager.getDynamicField(advice.getTarget(), ThreadConstants.DYNAMIC_FIELD_CONTEXT);
        Object threadId = manager.getDynamicField(advice.getTarget(), ThreadConstants.DYNAMIC_FIELD_THREAD_ID);
        Boolean isClusterTest = manager.getDynamicField(advice.getTarget(), ThreadConstants.DYNAMIC_FIELD_CLUSTER_TEST);
        Boolean isDebug = manager.getDynamicField(advice.getTarget(), ThreadConstants.DYNAMIC_FIELD_DEBUG);

        Long tid = getThreadId(threadId);
        if (context != null && tid != null && Thread.currentThread().getId() != tid) {
            Pradar.setInvokeContext(context);
        } else {
            if (isClusterTest != null) {
                Pradar.setClusterTest(isClusterTest);
            }
            if (isDebug != null) {
                Pradar.setDebug(isDebug);
            }
        }
    }

    private Long getThreadId(Object threadId) {
        if (threadId == null) {
            return null;
        }
        if (!(threadId instanceof Long)) {
            return null;
        }
        return (Long) threadId;
    }

    @Override
    public void doAfter(Advice advice) throws Throwable {
        try {
            Boolean hasContext = manager.getDynamicField(advice.getTarget(), ThreadConstants.DYNAMIC_FIELD_HAS_CONTEXT);
            if (hasContext != null && hasContext) {
                return;
            }
            Object context = manager.getDynamicField(advice.getTarget(), ThreadConstants.DYNAMIC_FIELD_CONTEXT);
            Object threadId = manager.getDynamicField(advice.getTarget(), ThreadConstants.DYNAMIC_FIELD_THREAD_ID);
            Boolean isClusterTest = manager.getDynamicField(advice.getTarget(), ThreadConstants.DYNAMIC_FIELD_CLUSTER_TEST);
            Boolean isDebug = manager.getDynamicField(advice.getTarget(), ThreadConstants.DYNAMIC_FIELD_DEBUG);
            Long tid = getThreadId(threadId);
            if (context != null && tid != null && Thread.currentThread().getId() != tid) {
                Pradar.clearInvokeContext();
            } else {
                if (isClusterTest != null || isDebug != null) {
                    Pradar.clearInvokeContext();
                }
            }
        } finally {
            manager.removeAll(advice.getTarget());
        }
        if (Pradar.isThreadCommit()) {
            Pradar.endServerInvoke(ResultCode.INVOKE_RESULT_SUCCESS);
        }
    }

    @Override
    public void doException(Advice advice) throws Throwable {
        try {
            Boolean hasContext = manager.getDynamicField(advice.getTarget(), ThreadConstants.DYNAMIC_FIELD_HAS_CONTEXT);
            if (hasContext != null && hasContext) {
                return;
            }
            Object context = manager.getDynamicField(advice.getTarget(), ThreadConstants.DYNAMIC_FIELD_CONTEXT);
            Object threadId = manager.getDynamicField(advice.getTarget(), ThreadConstants.DYNAMIC_FIELD_THREAD_ID);
            Boolean isClusterTest = manager.getDynamicField(advice.getTarget(), ThreadConstants.DYNAMIC_FIELD_CLUSTER_TEST);
            Boolean isDebug = manager.getDynamicField(advice.getTarget(), ThreadConstants.DYNAMIC_FIELD_DEBUG);
            Long tid = getThreadId(threadId);
            if (context != null && tid != null && Thread.currentThread().getId() != tid) {
                Pradar.clearInvokeContext();
            } else {
                if (isClusterTest != null || isDebug != null) {
                    Pradar.clearInvokeContext();
                }
            }
        } finally {
            manager.removeAll(advice.getTarget());
        }
        if (Pradar.isThreadCommit()) {
            Pradar.endServerInvoke(ResultCode.INVOKE_RESULT_FAILED);
        }
    }
}
