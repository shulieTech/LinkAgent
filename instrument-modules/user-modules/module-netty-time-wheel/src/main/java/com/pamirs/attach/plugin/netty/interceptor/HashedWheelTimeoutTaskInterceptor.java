package com.pamirs.attach.plugin.netty.interceptor;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.attach.plugin.netty.job.obj.TraceTimerTask;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;


public class HashedWheelTimeoutTaskInterceptor extends CutoffInterceptorAdaptor {

    @Override
    public CutOffResult cutoff0(Advice advice) throws Throwable {
        Timeout timeout = (Timeout) advice.getTarget();
        TimerTask timerTask = ReflectionUtils.get(timeout, "task");
        if(timerTask instanceof TraceTimerTask){
            timerTask = ((TraceTimerTask)timerTask).getBusTimerTask();
        }
        return CutOffResult.cutoff(timerTask);
    }
}

