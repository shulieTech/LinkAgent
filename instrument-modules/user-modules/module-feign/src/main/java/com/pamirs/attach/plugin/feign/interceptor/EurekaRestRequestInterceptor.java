package com.pamirs.attach.plugin.feign.interceptor;

import com.pamirs.pradar.InvokeContext;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

public class EurekaRestRequestInterceptor extends AroundInterceptor {

    private ThreadLocal<InvokeContext> threadLocalContext = new ThreadLocal<InvokeContext>();

    @Override
    public void doBefore(Advice advice) throws Throwable {
        if (Pradar.isClusterTest()) {
            threadLocalContext.set(Pradar.getInvokeContext());
            Pradar.clearInvokeContext();
        }
    }

    @Override
    public void doAfter(Advice advice) throws Throwable {
        if (threadLocalContext.get() != null) {
            Pradar.setInvokeContext(threadLocalContext.get());
        }
        threadLocalContext.remove();
    }

    @Override
    public void doException(Advice advice) throws Throwable {
        if (threadLocalContext.get() != null) {
            Pradar.setInvokeContext(threadLocalContext.get());
        }
        threadLocalContext.remove();
    }
}
