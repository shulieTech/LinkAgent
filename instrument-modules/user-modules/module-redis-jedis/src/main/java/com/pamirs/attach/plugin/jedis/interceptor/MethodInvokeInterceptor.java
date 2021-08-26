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
package com.pamirs.attach.plugin.jedis.interceptor;
import com.pamirs.attach.plugin.jedis.shadowserver.JedisFactory;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
public class MethodInvokeInterceptor extends CutoffInterceptorAdaptor {
    @Override
    public CutOffResult cutoff0(Advice advice) throws Throwable {
        if (!Pradar.isClusterTest()) {
            return CutOffResult.passed();
        }
        if (!GlobalConfig.getInstance().isShadowDbRedisServer()) {
            return CutOffResult.passed();
        }
        Object target = JedisFactory.getFactory().getClient(advice.getTarget(), advice.getBehavior().getName(),
            advice.getParameterArray(), advice.getReturnObj());
        String method = advice.getBehavior().getName();
        Object result = Reflect.on(target).call(method, advice.getParameterArray()).get();
        return CutOffResult.cutoff(result);
    }
}
