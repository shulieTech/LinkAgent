package com.pamirs.attach.plugin.feign.interceptor;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

public class EurekaRestRequestInterceptor extends AroundInterceptor {

    @Override
    public void doBefore(Advice advice) throws Throwable {
        // 如果当前线程存在压测标，要去除掉
        if(Pradar.isClusterTest()){
            Pradar.clearInvokeContext();
        }
    }
}
