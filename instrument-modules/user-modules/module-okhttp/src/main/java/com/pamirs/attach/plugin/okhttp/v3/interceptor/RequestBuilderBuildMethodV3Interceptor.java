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
package com.pamirs.attach.plugin.okhttp.v3.interceptor;

import com.pamirs.attach.plugin.okhttp.OKHttpConstants;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.internal.config.MatchConfig;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.shulie.instrument.simulator.api.ProcessControlException;
import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Request;

import java.util.Map;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/6/29 8:40 下午
 */
@ListenerBehavior(isFilterBusinessData = true)
public class RequestBuilderBuildMethodV3Interceptor extends TraceInterceptorAdaptor {
    @Override
    public String getPluginName() {
        return OKHttpConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return MiddlewareType.TYPE_RPC;
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        Object target = advice.getTarget();
        Request.Builder builder = (Request.Builder) target;
        Map<String, String> contextMap = Pradar.getInvokeContextTransformMap();
        for (Map.Entry<String, String> entry : contextMap.entrySet()) {
            builder.removeHeader(entry.getKey());
            builder.addHeader(entry.getKey(), entry.getValue());
        }
        return null;
    }
}
