package com.pamirs.attach.plugin.jms.interceptor;

import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

public class TaskWorkerRunInterceptor extends AroundInterceptor {

    @Override
    public void doAfter(Advice advice) throws Throwable {
        LookupInterceptor.JNDI_NAME.remove();
        LookupInterceptor.applyFunctionOfGetQueueThreadLocal.remove();
        if(Pradar.getInvokeContext() != null){
            Pradar.endServerInvoke(ResultCode.INVOKE_RESULT_FAILED, MiddlewareType.TYPE_MQ);
        }
    }

    @Override
    public void doException(Advice advice) throws Throwable {
        LookupInterceptor.JNDI_NAME.remove();
        LookupInterceptor.applyFunctionOfGetQueueThreadLocal.remove();
        if(Pradar.getInvokeContext() != null){
            Pradar.getInvokeContext().setResponse(advice.getThrowable());
            Pradar.endServerInvoke(ResultCode.INVOKE_RESULT_FAILED, MiddlewareType.TYPE_MQ);
        }
    }
}
