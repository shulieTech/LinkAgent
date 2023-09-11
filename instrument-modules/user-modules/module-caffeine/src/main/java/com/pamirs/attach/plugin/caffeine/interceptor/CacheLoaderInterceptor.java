package com.pamirs.attach.plugin.caffeine.interceptor;

import com.pamirs.pradar.cache.ClusterTestCacheWrapperKey;
import com.pamirs.pradar.interceptor.ModificationInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

public class CacheLoaderInterceptor extends ModificationInterceptorAdaptor {

    @Override
    public Object[] getParameter0(Advice advice) {
        Object[] args = advice.getParameterArray();
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof ClusterTestCacheWrapperKey) {
                args[i] = ((ClusterTestCacheWrapperKey)args[i]).getKey();
                break;
            }
        }
        return args;
    }

}
