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
package com.pamirs.attach.plugin.netty.interceptor;

import com.pamirs.attach.plugin.netty.NettyConstants;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.SocketChannel;

import javax.annotation.Resource;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/12/15 11:44 上午
 */
public class ChannelPipelineWriteInterceptor extends AroundInterceptor {
    @Resource
    protected DynamicFieldManager manager;

    @Override
    public void doBefore(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (!validate(args)) {
            return;
        }
        Object request = args[0];
        if (isAsynchronousInvocation(request)) {
            manager.setDynamicField(request, NettyConstants.DYNAMIC_FIELD_ASYNC_CONTEXT, Pradar.getInvokeContextMap());
        }
    }

    private boolean isAsynchronousInvocation(final Object request) {
        if (!(request instanceof ChannelPromise) && !(request instanceof SocketChannel)) {
            return false;
        }

        return true;
    }

    private boolean validate(Object[] args) {
        if (args == null || args.length == 0) {
            return false;
        }

        return true;
    }
}
