/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pamirs.attach.plugin.webflux.interceptor;

import com.pamirs.attach.plugin.webflux.common.WebFluxConstants;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.springframework.http.HttpHeaders;

import java.util.Map;

public class RequestBuildInterceptor extends TraceInterceptorAdaptor {
    @Override
    public String getPluginName() {
        return WebFluxConstants.MODULE_NAME;
    }

    @Override
    public int getPluginType() {
        return MiddlewareType.TYPE_RPC;
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        if (advice.getParameterArray().length < 6 && !(advice.getParameterArray()[2] instanceof HttpHeaders)) {
            return null;
        }
        HttpHeaders headers = (HttpHeaders) advice.getParameterArray()[2];
        Map<String, String> contextMap = Pradar.getInvokeContextTransformMap();
        for (Map.Entry<String, String> entry : contextMap.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                headers.add(entry.getKey(), entry.getValue());
            }
        }
        return null;
    }
}
