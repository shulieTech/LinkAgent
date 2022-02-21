package com.pamirs.attach.plugin.rabbitmq.interceptor;

import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.listener.ext.Behavior;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;

public class StreamListenerMessageHandlerIntercepter extends CutoffInterceptorAdaptor {
    private static Logger logger = LoggerFactory.getLogger(StreamListenerMessageHandlerIntercepter.class.getName());
    InvocableHandlerMethod invocableHandlerMethod;
    @Override
    public CutOffResult cutoff0(Advice advice) throws Throwable {
        if(!Pradar.isClusterTest()){
            return CutOffResult.passed();
        }
        try {
            invocableHandlerMethod = (Reflect.on(
                advice.getTarget()).get("invocableHandlerMethod"));
            final Behavior behavior = advice.getBehavior();
            behavior.setAccessible(true);
            // final Object invoke = behavior.invoke(advice.getCallTarget(), advice.getParameterArray());
            final Object invoke = invocableHandlerMethod.invoke((Message<?>)advice.getParameterArray()[0]);
            return CutOffResult.cutoff(invoke);
        }catch (Exception e) {
            if (e instanceof MessagingException) {
                throw (MessagingException) e;
            }
            else {
                throw new MessagingException((Message<?>)advice.getParameterArray()[0],
                    "Exception thrown while invoking "
                        + this.invocableHandlerMethod.getShortLogMessage(),
                    e);
            }
        } // catch (PressureMeasureError e) {
        //    logger.error("[/RabbitMQ] invokeListener error PressureMeasureError", e);
        //}
        //return CutOffResult.cutoff(null);
    }
}
