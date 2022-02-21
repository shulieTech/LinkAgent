package com.pamirs.attach.plugin.alibaba.rocketmq.common;

import com.alibaba.rocketmq.client.hook.ConsumeMessageContext;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/12/21 2:29 PM
 */
public class OrderlyTraceContexts {

    private final static ThreadLocal<ConsumeMessageContext> contextThreadLocal = new ThreadLocal<ConsumeMessageContext>();

    public static ConsumeMessageContext get(){
        return contextThreadLocal.get();
    }

    public static void remove(){
        contextThreadLocal.remove();
    }

    public static void set(ConsumeMessageContext context){
        contextThreadLocal.set(context);
    }
}
