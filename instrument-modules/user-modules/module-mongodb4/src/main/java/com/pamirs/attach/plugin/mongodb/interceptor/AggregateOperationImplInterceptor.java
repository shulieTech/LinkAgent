package com.pamirs.attach.plugin.mongodb.interceptor;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author jiangjibo
 * @date 2022/3/14 6:04 下午
 * @description:
 */
public class AggregateOperationImplInterceptor extends ParametersWrapperInterceptorAdaptor {

    @Override
    protected Object[] getParameter0(Advice advice) throws Throwable {
        if (!Pradar.isClusterTest()) {
            return super.getParameter0(advice);
        }
        Object[] parameterArray = advice.getParameterArray();
        for (int i = 0; i < parameterArray.length; i++) {
            Object o = parameterArray[i];
            if (!(o instanceof String)) {
                continue;
            }
            String collectionName = (String) o;
            if (!Pradar.isClusterTestPrefix(collectionName)) {
                parameterArray[i] = Pradar.CLUSTER_TEST_PREFIX + collectionName;
            }
        }
        return parameterArray;
    }
}
