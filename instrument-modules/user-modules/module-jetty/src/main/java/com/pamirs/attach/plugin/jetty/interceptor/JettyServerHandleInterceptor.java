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
package com.pamirs.attach.plugin.jetty.interceptor;

import com.pamirs.attach.plugin.common.web.BufferedServletRequestWrapper;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author fabing.zhaofb
 */
public class JettyServerHandleInterceptor extends AbstractServerHandleInterceptor {
    @Override
    HttpServletRequest toHttpServletRequest(Object[] args) {
        if (args == null || args.length < 4) {
            return null;
        }

        if (args[2] instanceof HttpServletRequest) {
            try {
                return (HttpServletRequest) args[2];
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Override
    HttpServletResponse toHttpServletResponse(Object[] args) {
        if (args == null || args.length < 4) {
            return null;
        }

        if (args[3] instanceof HttpServletResponse) {
            try {
                return (HttpServletResponse) args[3];
            } catch (Throwable ignored) {
            }
        }
        return null;

    }

    @Override
    void doWrapRequest(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (!(args[2] instanceof HttpServletRequest)) {
            return;
        }
        advice.changeParameter(2, new BufferedServletRequestWrapper((HttpServletRequest) args[2]));
    }
}
