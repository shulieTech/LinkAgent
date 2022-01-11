package com.pamirs.attach.plugin.jms.interceptor;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

public class QueueSessionInterceptor extends ParametersWrapperInterceptorAdaptor {
    @Override
    protected Object[] getParameter0(Advice advice) throws Throwable {
        final Object[] args = advice.getParameterArray();
        ClusterTestUtils.validateClusterTest();
        if (args == null || args.length == 0) {
            return args;
        }
        if (!Pradar.isClusterTest()) {
            return args;
        }
        args[0] = LookupInterceptor.applyFunctionOfGetQueueThreadLocal.get().apply();
        return args;
    }

}
