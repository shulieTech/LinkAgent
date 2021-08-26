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
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.activemq.command.ActiveMQDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Create by xuyh at 2020/3/9 22:40.
 */
public class SpringJmsTemplateInterceptor extends ParametersWrapperInterceptorAdaptor {
    private static Logger logger = LoggerFactory.getLogger(SpringJmsTemplateInterceptor.class);

    @Override
    public Object[] getParameter0(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (!PradarSwitcher.isClusterTestEnabled()) {
            return args;
        }

        if (!Pradar.isClusterTest()) {
            return args;
        }
        if (args != null && args.length == 2) {
            try {
                Object destinationObj = args[1];
                if (destinationObj instanceof ActiveMQDestination) {
                    ActiveMQDestination destination = (ActiveMQDestination) destinationObj;
                    String name = destination.getPhysicalName();
                    String namePt = Pradar.addClusterTestPrefixLower(name);
                    destination.setPhysicalName(namePt);
                    Properties properties = destination.getProperties();
                    properties.setProperty(PradarService.PRADAR_CLUSTER_TEST_KEY, Boolean.TRUE.toString());
                }
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            }
        }
        return args;

    }
}
