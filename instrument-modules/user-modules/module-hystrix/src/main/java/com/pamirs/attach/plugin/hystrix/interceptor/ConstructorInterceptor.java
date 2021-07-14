package com.pamirs.attach.plugin.hystrix.interceptor;

import com.pamirs.attach.plugin.hystrix.HystrixConstants;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;

import javax.annotation.Resource;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/3/17 2:05 下午
 */
public class ConstructorInterceptor extends AroundInterceptor {
    @Resource
    protected DynamicFieldManager manager;

    @Override
    public void doBefore(Advice advice) throws Throwable {
        manager.setDynamicField(advice.getTarget(), HystrixConstants.DYNAMIC_FILED_INVOKE_CONTEXT, Pradar.getInvokeContextMap());
        manager.setDynamicField(advice.getTarget(), HystrixConstants.DYNAMIC_FILED_THREAD_ID, Thread.currentThread().getId());
    }
}
