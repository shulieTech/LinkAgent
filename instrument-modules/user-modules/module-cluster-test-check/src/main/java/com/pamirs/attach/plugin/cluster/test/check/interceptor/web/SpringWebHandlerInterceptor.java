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

package com.pamirs.attach.plugin.cluster.test.check.interceptor.web;

import com.pamirs.attach.plugin.cluster.test.check.interceptor.AbstractCheckInterceptor;
import com.pamirs.attach.plugin.cluster.test.check.utils.ClassUtil;
import com.shulie.instrument.simulator.api.annotation.Interrupted;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.springframework.web.server.ServerWebExchange;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/22 18:02
 */
@Interrupted
public class SpringWebHandlerInterceptor extends AbstractCheckInterceptor {

    @Override
    public Object getParam(Advice advice, String key) {
        if (ClassUtil.isInstance(advice.getParameterArray()[0], "org.springframework.web.server.ServerWebExchange")) {
            ServerWebExchange serverWebExchange = (ServerWebExchange) advice.getParameterArray()[0];
            return serverWebExchange.getRequest().getHeaders().getFirst(key);
        }
        return null;
    }
}