package com.pamirs.attach.plugin.netty.interceptor;

import com.pamirs.attach.plugin.netty.job.obj.TraceTimerTask;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import io.netty.util.TimerTask;

/**
 * @author angju
 * @date 2021/6/11 15:38
 */
public class HashedWheelTimerNewTimeoutInterceptor extends ParametersWrapperInterceptorAdaptor {

    @Override
    protected Object[] getParameter0(Advice advice) throws Throwable {
        Object[] args = super.getParameter0(advice);
        //org.asynchttpclient.netty.timeout.RequestTimeoutTimerTask async-http使用，不做增强操作
        if (args[0].getClass().getName().equals("org.asynchttpclient.netty.timeout.RequestTimeoutTimerTask")) {
            return args;
        }
        TimerTask BUSTimerTask = (TimerTask)args[0];
        TraceTimerTask ptTimerTask = new TraceTimerTask(BUSTimerTask, Pradar.getInvokeContextMap());
        args[0] = ptTimerTask;
        return args;
    }

}
