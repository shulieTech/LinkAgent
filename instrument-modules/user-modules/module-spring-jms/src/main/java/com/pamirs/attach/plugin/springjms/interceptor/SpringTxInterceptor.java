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

import com.pamirs.attach.plugin.springjms.SpringJmsConstants;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.springframework.jms.listener.AbstractPollingMessageListenerContainer;

import javax.jms.Destination;

public class SpringTxInterceptor extends TraceInterceptorAdaptor {
    @Override
    public String getPluginName() {
        return SpringJmsConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return SpringJmsConstants.PLUGIN_TYPE;
    }

    static boolean hasJmsMessage = true;

    @Override
    public void beforeFirst(Advice advice) {
        Object target = advice.getTarget();
        if (!PradarSwitcher.isClusterTestEnabled()) {
            return;
        }

        AbstractPollingMessageListenerContainer container = (AbstractPollingMessageListenerContainer) target;
        Pradar.setClusterTest(false);
        try {
            if (!hasJmsMessage) {
                return;
            }
            Class clazz = Class.forName("javax.jms.Destination");
            if (clazz != null) {
                Destination des = container.getDestination();
                String name = "";
                if (des != null) {
                    name = des.toString();
                }
                if (name == null || name.equals("")) {
                    name = container.getDestinationName();
                }

                if (name != null && Pradar.isClusterTestPrefix(name)) {
                    Pradar.setClusterTest(true);
                }

            } else {
                hasJmsMessage = false;
            }
        } catch (Throwable e) {
            hasJmsMessage = false;
        }
    }
}


