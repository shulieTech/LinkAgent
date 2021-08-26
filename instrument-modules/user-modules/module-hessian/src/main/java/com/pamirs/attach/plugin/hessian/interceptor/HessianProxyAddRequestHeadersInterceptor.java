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
package com.pamirs.attach.plugin.hessian.interceptor;

import com.caucho.hessian.client.HessianConnection;
import com.pamirs.attach.plugin.hessian.HessianConstants;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @Description 解决上下文传输问题
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/7/16 9:46 上午
 */
public class HessianProxyAddRequestHeadersInterceptor extends TraceInterceptorAdaptor {
    @Resource
    protected DynamicFieldManager manager;

    @Override
    public String getPluginName() {
        return HessianConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return HessianConstants.PLUGIN_TYPE;
    }

    @Override
    public void beforeLast(Advice advice) {
        Object[] args = advice.getParameterArray();
        Object target = advice.getTarget();
        if (args == null || args.length == 0) {
            return;
        }
        HessianConnection connection = (HessianConnection) args[0];
        Map<String, String> context = Pradar.getInvokeContextTransformMap();
        for (Map.Entry<String, String> entry : context.entrySet()) {
            connection.addHeader(entry.getKey(), entry.getValue());
        }

        Method method = manager.getDynamicField(target, HessianConstants.DYNAMIC_FIELD_METHOD);
        if (method != null) {
            connection.addHeader(HessianConstants.METHOD_HEADER, method.getName());
        }
    }
}
