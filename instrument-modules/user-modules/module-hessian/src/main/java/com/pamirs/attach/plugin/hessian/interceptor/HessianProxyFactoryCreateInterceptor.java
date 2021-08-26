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

import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.io.HessianRemoteObject;
import com.pamirs.attach.plugin.hessian.common.HessianProxyWrapper;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;

import javax.annotation.Resource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URL;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/7/22 9:16 下午
 */
public class HessianProxyFactoryCreateInterceptor extends CutoffInterceptorAdaptor {
    @Resource
    protected DynamicFieldManager manager;

    @Override
    public CutOffResult cutoff0(Advice advice) {
        Object[] args = advice.getParameterArray();
        Object target = advice.getTarget();
        Class<?> api = (Class<?>) args[0];
        URL url = (URL) args[1];
        ClassLoader loader = (ClassLoader) args[2];
        if (api == null) {
            throw new NullPointerException("api must not be null for HessianProxyFactory.create()");
        }
        InvocationHandler handler = null;

        handler = new HessianProxyWrapper(url, (HessianProxyFactory) target, api, manager);

        return CutOffResult.cutoff(Proxy.newProxyInstance(loader,
                new Class[]{api,
                        HessianRemoteObject.class},
                handler));
    }
}
