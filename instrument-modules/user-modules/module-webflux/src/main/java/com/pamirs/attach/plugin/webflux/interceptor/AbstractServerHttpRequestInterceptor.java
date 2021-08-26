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
package com.pamirs.attach.plugin.webflux.interceptor;

import com.pamirs.attach.plugin.webflux.common.Cache;
import com.pamirs.attach.plugin.webflux.common.WebFluxConstants;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;

/**
 * @Auther: vernon
 * @Date: 2021/1/11 22:13
 * @Description:
 */
public class AbstractServerHttpRequestInterceptor extends TraceInterceptorAdaptor {



    @Override
    public void afterFirst(Advice advice) {

        if (advice.getTarget() instanceof AbstractServerHttpRequest) {
            Cache.RequestHolder.set(advice.getTarget());
        }

    }



    @Override
    public String getPluginName() {
        return WebFluxConstants.MODULE_NAME;
    }

    @Override
    public int getPluginType() {
        return MiddlewareType.TYPE_WEB_SERVER;
    }
}
