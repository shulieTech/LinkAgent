package com.pamirs.attach.plugin.jdk.http.interceptor;

import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

public class JavaNetHttpURLConnectionGetHeaderInterceptor extends CutoffInterceptorAdaptor {

    @Override
    public CutOffResult cutoff0(Advice advice) throws Throwable {
        if (!Pradar.isClusterTest()) {
            return super.cutoff0(advice);
        }
        Integer index = (Integer) advice.getParameterArray()[0];
        if (index == 0) {
            return CutOffResult.cutoff("HTTP/1.1 200");
        }
        return super.cutoff0(advice);
    }
}
