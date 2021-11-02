/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.springjms.interceptor;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.config.*;

import java.lang.reflect.Field;

/**
 * Create by xuyh at 2020/3/10 11:34.
 */
public class SpringJmsListenerEndpointRegisterInterceptor extends ParametersWrapperInterceptorAdaptor {
    private static Logger logger = LoggerFactory.getLogger(SpringJmsListenerEndpointRegisterInterceptor.class.getName());

    @Override
    public Object[] getParameter0(Advice advice) {
        Object[] args = advice.getParameterArray();
        String methodName = advice.getBehaviorName();
        Object target = advice.getTarget();
        if (!PradarSwitcher.isClusterTestEnabled()) {
            return args;
        }
        if (!"registerEndpoint".equals(methodName) || args == null || args.length != 2) {
            return args;
        }
        if (!(target instanceof JmsListenerEndpointRegistrar)) {
            return args;
        }

        JmsListenerEndpointRegistrar registrar = (JmsListenerEndpointRegistrar) target;
        AbstractJmsListenerEndpoint endpoint = (AbstractJmsListenerEndpoint) args[0];
        JmsListenerContainerFactory factory = (JmsListenerContainerFactory) args[1];
        // -- copy properties
        JmsListenerEndpoint endpointNew;
        if (!(endpoint instanceof MethodJmsListenerEndpoint)) {
            return args;
        }

        if (endpoint instanceof MethodJmsListenerEndpoint) {
            MethodJmsListenerEndpoint endpointOld = (MethodJmsListenerEndpoint) endpoint;
            if (Pradar.isClusterTestSuffix(endpointOld.getId())) {
                return args;
            }
            MethodJmsListenerEndpoint endPointTarget = new MethodJmsListenerEndpoint();
            try {
                Field messageHandlerMethodFactoryField = endpointOld.getClass().getDeclaredField("messageHandlerMethodFactory");
                messageHandlerMethodFactoryField.setAccessible(true);
                Object messageHandlerMethodFactoryValue = messageHandlerMethodFactoryField.get(endpointOld);
                messageHandlerMethodFactoryField.set(endPointTarget, messageHandlerMethodFactoryValue);
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            }
            endPointTarget.setId(Pradar.addClusterTestSuffixLower(endpointOld.getId()));
            endPointTarget.setSubscription(endpointOld.getSubscription());
            endPointTarget.setSelector(endpointOld.getSelector());
            endPointTarget.setDestination(Pradar.addClusterTestPrefixLower(endpointOld.getDestination()));
            endPointTarget.setConcurrency(endpointOld.getConcurrency());
            endPointTarget.setMethod(endpointOld.getMethod());
            endPointTarget.setBean(endpointOld.getBean());
            if (endpointOld.getConcurrency() != null) {
                endPointTarget.setConcurrency(endpointOld.getConcurrency());
            } else {
                endPointTarget.setConcurrency("1");
            }
            endpointNew = endPointTarget;
        } else {
            SimpleJmsListenerEndpoint endpointOld = (SimpleJmsListenerEndpoint) endpoint;
            if (Pradar.isClusterTestSuffix(endpointOld.getId())) {
                return args;
            }
            SimpleJmsListenerEndpoint endPointTarget = new SimpleJmsListenerEndpoint();
            endPointTarget.setId(Pradar.addClusterTestSuffixLower(endpointOld.getId()));
            endPointTarget.setMessageListener(endpointOld.getMessageListener());
            endPointTarget.setConcurrency(endpointOld.getConcurrency());
            endPointTarget.setDestination(Pradar.addClusterTestPrefixLower(endpointOld.getDestination()));
            endPointTarget.setSelector(endpointOld.getSelector());
            endPointTarget.setSubscription(endpointOld.getSubscription());
            if (endpointOld.getConcurrency() != null) {
                endPointTarget.setConcurrency(endpointOld.getConcurrency());
            } else {
                endPointTarget.setConcurrency("1");
            }
            endpointNew = endPointTarget;
        }
        // -- register new
        registrar.registerEndpoint(endpointNew, factory);
        return args;
    }
}
