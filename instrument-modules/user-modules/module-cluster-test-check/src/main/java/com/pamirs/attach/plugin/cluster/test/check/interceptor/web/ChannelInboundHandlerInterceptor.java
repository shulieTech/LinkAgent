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

import com.netflix.zuul.message.http.HttpRequestMessage;
import com.pamirs.attach.plugin.cluster.test.check.interceptor.AbstractCheckInterceptor;
import com.pamirs.attach.plugin.cluster.test.check.utils.ClassUtil;
import com.shulie.instrument.simulator.api.annotation.Interrupted;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import io.netty.handler.codec.http.HttpMessage;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/22 18:07
 */
@Interrupted
public class ChannelInboundHandlerInterceptor extends AbstractCheckInterceptor {

    @Override
    public Object getParam(Advice advice, String key) {
        Object request = advice.getParameterArray()[1];
        if (ClassUtil.isInstance(request, "io.netty.handler.codec.http.HttpMessage")) {
            HttpMessage httpMessage = (HttpMessage) request;
            return httpMessage.headers().get(key);
        }
        if (ClassUtil.isInstance(request, "com.netflix.zuul.message.http.HttpRequestMessage")) {
            HttpRequestMessage requestMessage = (HttpRequestMessage) request;
            return requestMessage.getHeaders().getFirst(key);
        }
        return null;
    }
}
