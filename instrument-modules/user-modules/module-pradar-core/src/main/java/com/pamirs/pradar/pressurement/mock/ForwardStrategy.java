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
package com.pamirs.pradar.pressurement.mock;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.internal.adapter.ExecutionForwardCall;
import com.pamirs.pradar.internal.adapter.ExecutionStrategy;
import com.pamirs.pradar.internal.config.ExecutionCall;
import com.pamirs.pradar.internal.config.MatchConfig;
import com.shulie.instrument.simulator.api.ProcessControlException;
import com.shulie.instrument.simulator.api.reflect.Reflect;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @Author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @package: com.pamirs.pradar.pressurement.mock
 * @Date 2021/6/7 7:12 下午
 */
public class ForwardStrategy implements ExecutionStrategy {

    @Override
    public Object processBlock(Class returnType, ClassLoader classLoader, Object params) {
        if (Pradar.isClusterTest()) {
            MatchConfig config = (MatchConfig) params;
            Object request = config.getArgs().get("request");
            String method = (String) config.getArgs().get("method");
            if (null != config.getForwarding() && null != request) {
                try {
                    URL url = new URL(config.getForwarding());
                    Reflect.on(request).set(method, url);
                } catch (Exception e) {
                    try {
                        URI uri = new URI(config.getForwarding());
                        Reflect.on(request).set(method, uri);
                    } catch (URISyntaxException uriSyntaxException) {
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Object processBlock(Class returnType, ClassLoader classLoader, Object params, ExecutionCall call) throws ProcessControlException {
        if (call instanceof ExecutionForwardCall) {
            ExecutionForwardCall forwardCall = (ExecutionForwardCall) call;
            Object block = processBlock(returnType, classLoader, params);
            return forwardCall.forward(block);
        }
        return processBlock(returnType, classLoader, params);
    }

    @Override
    public Object processNonBlock(Class returnType, ClassLoader classLoader, Object params, ExecutionCall call) throws ProcessControlException {
        if (call instanceof ExecutionForwardCall) {
            ExecutionForwardCall forwardCall = (ExecutionForwardCall) call;
            Object block = processBlock(returnType, classLoader, params);
            return forwardCall.forward(block);
        }
        return processBlock(returnType, classLoader, params);
    }
}
