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
package com.pamirs.attach.plugin.rabbitmq.interceptor;

import com.pamirs.attach.plugin.rabbitmq.destroy.RabbitmqDestroy;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.rabbitmq.client.GetResponse;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.StringUtils;

import java.util.Map;


/**
 * @Author: guohz
 * @ClassName: ChannelBasicGetInterceptor
 * @Package: com.pamirs.attach.plugin.rabbitmq.interceptor
 * @Date: 2019-07-25 14:33
 * @Description:
 */
@Destroyable(RabbitmqDestroy.class)
public class ChannelNBasicGetInterceptor extends AroundInterceptor {

    @Override
    public void doBefore(Advice advice) {
        Object[] args = advice.getParameterArray();
        String queue = (String) args[0];
        //压测代码-
        try {
            if (PradarSwitcher.isClusterTestEnabled()) {
                validatePressureMeasurement(queue);
            }
        } catch (Throwable e) {
            throw new PressureMeasureError(e);
        }
    }

    @Override
    public void doAfter(Advice advice) {
        if (PradarSwitcher.isClusterTestEnabled()) {
            GetResponse result = (GetResponse) advice.getReturnObj();
            if (result == null || result.getProps() == null) {
                return;
            }
            Map<String, Object> headers = result.getProps().getHeaders();
            if (null != headers
                    && !headers.isEmpty()
                    && ClusterTestUtils.isClusterTestRequest((String) headers.get(PradarService.PRADAR_CLUSTER_TEST_KEY))) {
                Pradar.setClusterTest(true);
            }
        }
    }

    private void validatePressureMeasurement(String topic) {
        try {
            Pradar.setClusterTest(false);
            topic = StringUtils.trimToEmpty(topic);
            if (topic != null
                    && Pradar.isClusterTestPrefix(topic)) {
                Pradar.setClusterTest(true);
            }
        } catch (Throwable e) {
            LOGGER.error("", e);
            if (Pradar.isClusterTest()) {
                throw new PressureMeasureError(e);
            }
        }

    }
}
