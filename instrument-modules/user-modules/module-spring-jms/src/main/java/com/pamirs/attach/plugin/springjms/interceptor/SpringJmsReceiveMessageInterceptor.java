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
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

import javax.jms.JMSException;
import javax.jms.Message;

/**
 * @ClassName: SpringJmsReceiveMessageInterceptor
 * @author: wangjian
 * @Date: 2020/9/15 15:44
 * @Description:
 */
public class SpringJmsReceiveMessageInterceptor extends TraceInterceptorAdaptor {
    @Override
    public String getPluginName() {
        return SpringJmsConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return SpringJmsConstants.PLUGIN_TYPE;
    }

    @Override
    public void afterFirst(Advice advice) {
        Object result = advice.getReturnObj();
        Message result1 = (Message) result;
        try {
            String key = result1.getStringProperty(PradarService.PRADAR_CLUSTER_TEST_KEY);
            if (ClusterTestUtils.isClusterTestRequest(key)) {
                Pradar.setClusterTest(true);
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
