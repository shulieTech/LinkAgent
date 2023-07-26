package com.pamirs.attach.plugin.ehcache.interceptor;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.cache.ClusterTestCacheWrapperKey;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

@ListenerBehavior(isFilterClusterTest = true)
public class PreferredLoaderObjectInputStreamResolveClassInterceptor extends ParametersWrapperInterceptorAdaptor {

    @Override
    protected Object[] getParameter0(Advice advice) throws Throwable {
        Object arg = advice.getParameterArray()[0];
        if (arg instanceof ClusterTestCacheWrapperKey) {
            arg = ((ClusterTestCacheWrapperKey) arg).getKey();
        }
        Class<?> className = Class.forName(String.valueOf(ReflectionUtils.invoke(arg, "getName")));
        ClassLoader loader = ReflectionUtils.get(advice.getTarget(), "loader");
        return new Object[]{className, false, loader};
    }
}
