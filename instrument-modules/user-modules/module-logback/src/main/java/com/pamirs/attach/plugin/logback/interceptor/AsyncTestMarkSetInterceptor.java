package com.pamirs.attach.plugin.logback.interceptor;

import javax.annotation.Resource;

import com.pamirs.attach.plugin.logback.Constants;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/06/21 7:48 下午
 */
public class AsyncTestMarkSetInterceptor extends AroundInterceptor {

    @Resource
    protected DynamicFieldManager manager;

    @Override
    public void doBefore(Advice advice) throws Throwable {
        //异步打印日志信息打上压测标记
        Object logEvent = advice.getParameterArray()[0];
        manager.setDynamicField(logEvent, Constants.ASYNC_CLUSTER_TEST_MARK_FIELD, Pradar.isClusterTest());
        super.doBefore(advice);
    }
}