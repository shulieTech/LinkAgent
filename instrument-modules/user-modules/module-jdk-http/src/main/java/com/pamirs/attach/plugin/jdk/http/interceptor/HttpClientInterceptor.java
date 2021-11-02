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
package com.pamirs.attach.plugin.jdk.http.interceptor;

import java.lang.reflect.Field;
import java.util.Map;

import com.pamirs.attach.plugin.jdk.http.JdkHttpConstants;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.utils.InnerWhiteListCheckUtil;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.net.www.MessageHeader;
import sun.net.www.http.HttpClient;

/**
 * @Auther: vernon
 * @Date: 2020/3/24 23:33
 * @Description:
 */
public class HttpClientInterceptor extends AroundInterceptor {
    private final static Logger LOGGER = LoggerFactory.getLogger(HttpClientInterceptor.class);

    private static volatile Field FIELD = null;

    @Override
    public void doBefore(Advice advice) {
        InnerWhiteListCheckUtil.check();
        Object target = advice.getTarget();
        ClusterTestUtils.validateClusterTest();
        HttpClient client = (HttpClient)target;

        MessageHeader header = null;
        try {
            initField(client);
            header = Reflect.on(client).get(FIELD);
        } catch (ReflectException e) {
            LOGGER.warn("{} has not field {}", client.getClass().getName(), JdkHttpConstants.DYNAMIC_FIELD_REQUESTS);
        }
        if (header == null) {
            header = new MessageHeader();
        }

        Map<String, String> ctx = Pradar.getInvokeContextTransformMap();
        for (Map.Entry<String, String> entry : ctx.entrySet()) {
            header.set(entry.getKey(), entry.getValue());
        }
        try {
            Reflect.on(client).set(FIELD, header);
        } catch (ReflectException e) {
            LOGGER.warn("{} has not field {}", client.getClass().getName(), JdkHttpConstants.DYNAMIC_FIELD_REQUESTS);
        }
    }

    private void initField(HttpClient client) {
        if (FIELD == null) {
            synchronized (HttpClientInterceptor.class) {
                if (FIELD == null) {
                    FIELD = Reflect.on(client).field0(JdkHttpConstants.DYNAMIC_FIELD_REQUESTS);
                }
            }
        }
    }
}
