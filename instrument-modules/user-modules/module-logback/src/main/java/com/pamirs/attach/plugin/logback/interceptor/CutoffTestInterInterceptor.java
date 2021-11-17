package com.pamirs.attach.plugin.logback.interceptor;

import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/11/15 10:34 上午
 */
public class CutoffTestInterInterceptor extends CutoffInterceptorAdaptor {

    @Override
    public CutOffResult cutoff0(Advice advice) throws Throwable {
        return CutOffResult.cutoff(0);
    }
}
