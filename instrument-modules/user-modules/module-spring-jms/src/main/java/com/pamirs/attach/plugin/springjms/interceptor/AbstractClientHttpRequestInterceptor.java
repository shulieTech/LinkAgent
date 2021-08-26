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
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import java.lang.reflect.Field;
import java.util.Map;

public class AbstractClientHttpRequestInterceptor extends AroundInterceptor {

    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractClientHttpRequestInterceptor.class);

    private volatile Field headersField;

    private synchronized void initHeadersField() {
        try {
            Class clazz = org.springframework.http.client.AbstractClientHttpRequest.class;
            headersField = clazz.getDeclaredField(SpringJmsConstants.REFLECT_FIELD_HEADERS);
            headersField.setAccessible(true);
        } catch (Throwable e) {
        }
    }

    /**
     * 优化性能，增强访问效率
     *
     * @param target
     * @param <T>
     * @return
     */
    private <T> T getHeadersField(Object target) {
        try {
            if (headersField == null) {
                return Reflect.on(target).get(SpringJmsConstants.REFLECT_FIELD_HEADERS);
            } else {
                return (T) headersField.get(target);
            }
        } catch (Throwable e) {
            if (e instanceof ReflectException) {
                throw (ReflectException) e;
            }
            return Reflect.on(target).get(SpringJmsConstants.REFLECT_FIELD_HEADERS);
        }
    }

    @Override
    public void doBefore(Advice advice) {
        if (!PradarSwitcher.isClusterTestEnabled()) {
            return;
        }

        org.springframework.http.client.AbstractClientHttpRequest clientHttpRequest = (org.springframework.http.client.AbstractClientHttpRequest) advice.getTarget();
        try {
            if (headersField == null) {
                initHeadersField();
            }
            HttpHeaders headers = null;
            try {
                headers = getHeadersField(clientHttpRequest);
            } catch (ReflectException e) {
                headers = clientHttpRequest.getHeaders();
            }

            Map<String, String> contextMap = Pradar.getInvokeContextTransformMap();
            for (Map.Entry<String, String> entry : contextMap.entrySet()) {
                headers.set(entry.getKey(), entry.getValue());
            }

        } catch (Throwable e) {
            LOGGER.error("", e);
        }
    }

}
