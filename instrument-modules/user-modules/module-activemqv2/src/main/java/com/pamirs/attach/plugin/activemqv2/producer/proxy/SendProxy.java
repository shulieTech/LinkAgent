package com.pamirs.attach.plugin.activemqv2.producer.proxy;

import com.pamirs.attach.plugin.activemqv2.util.ActiveMQDestinationUtil;
import io.shulie.instrument.module.isolation.proxy.impl.ModifyParamShadowMethodProxy;
import org.apache.activemq.command.ActiveMQDestination;

import java.lang.reflect.Method;

/**
 * @author guann1n9
 * @date 2023/12/22 2:52 PM
 */
public class SendProxy extends ModifyParamShadowMethodProxy {

    @Override
    public Object[] fetchParam(Object shadowTarget, Method method, Object... args) {

        //若参数中包含Destination 需判断是否为影子queue
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if(!(arg instanceof ActiveMQDestination)){
                continue;
            }
            ActiveMQDestination destination = (ActiveMQDestination) arg;
            args[i] = ActiveMQDestinationUtil.getInstance().mappingShadowDestination(destination);
        }
        return args;
    }

}