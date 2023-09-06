package com.pamirs.attach.plugin.ehcache.interceptor;

import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.cache.ClusterTestCacheWrapperKey;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

import java.io.ObjectStreamClass;

@ListenerBehavior(isFilterClusterTest = true)
public class PreferredLoaderObjectInputStreamResolveClassInterceptor extends CutoffInterceptorAdaptor {

    @Override
    public CutOffResult cutoff0(Advice advice) throws Throwable {
        ObjectStreamClass arg = (ObjectStreamClass) advice.getParameterArray()[0];
        String name = arg.getName();
        if ("com.pamirs.pradar.cache.ClusterTestCacheWrapperKey".equals(name)) {
            return CutOffResult.cutoff(ClusterTestCacheWrapperKey.class);
        }
        return CutOffResult.passed();
    }

}
