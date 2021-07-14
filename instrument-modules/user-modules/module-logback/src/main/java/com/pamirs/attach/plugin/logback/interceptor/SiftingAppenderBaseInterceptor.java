package com.pamirs.attach.plugin.logback.interceptor;

import javax.annotation.Resource;

import com.pamirs.attach.plugin.logback.Constants;
import com.pamirs.attach.plugin.logback.utils.ClusterTestMarker;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/06/22 12:00 下午
 */
public class SiftingAppenderBaseInterceptor extends AroundInterceptor {

    private final boolean isBusinessLogOpen;

    @Resource
    protected DynamicFieldManager manager;

    public SiftingAppenderBaseInterceptor(boolean isBusinessLogOpen) {this.isBusinessLogOpen = isBusinessLogOpen;}

    @Override
    public void doBefore(Advice advice) throws Throwable {
        if (!isBusinessLogOpen) {
            return;
        }
        Object[] args = advice.getParameterArray();
        if (args == null || args.length != 1) {
            return;
        }
        if (isClusterTest(args[0])) {
            ClusterTestMarker.mark(true);
        }
    }

    private boolean isClusterTest(Object event) {
        if (Pradar.isClusterTest()) {
            return true;
        }
        return manager.getDynamicField(event, Constants.ASYNC_CLUSTER_TEST_MARK_FIELD, false);
    }
}
