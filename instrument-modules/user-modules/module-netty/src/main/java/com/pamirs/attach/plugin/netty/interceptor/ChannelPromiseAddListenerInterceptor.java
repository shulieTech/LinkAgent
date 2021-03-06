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
 * @since 2020/12/15 7:05 下午
 */
public class ChannelPromiseAddListenerInterceptor extends AroundInterceptor {
    @Resource
    protected DynamicFieldManager manager;

    @Override
    public void doAfter(Advice advice) {
        Object[] args = advice.getParameterArray();
        Object result = advice.getReturnObj();
        Object target = advice.getTarget();
        if (isAsynchronousInvocation(target, args, result)) {
            manager.setDynamicField(result, NettyConstants.DYNAMIC_FIELD_ASYNC_CONTEXT, Pradar.getInvokeContextMap());
        }
    }

    private boolean isAsynchronousInvocation(final Object target, final Object[] args, Object result) {
        if (result == null) {
            return false;
        }

        if (!(result instanceof ChannelPromise) && !(result instanceof SocketChannel)) {
            return false;
        }

        return true;
    }
}
