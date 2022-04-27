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
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;


/**
 * @Auther: vernon
 * @Date: 2021/1/11 17:45
 * @Description:
 */
public class HeaderMethodInterceptor extends CutoffInterceptorAdaptor {

    @Override
    public  CutOffResult cutoff0(Advice advice) {
        if (!PradarSwitcher.isClusterTestEnabled()) {
            return CutOffResult.cutoff(null);
        }
        HttpHeaders headers = (HttpHeaders) advice.getTarget();

        HttpHeaders writableHttpHeaders = HttpHeaders.writableHttpHeaders(headers);

        writableHttpHeaders.add(String.valueOf(advice.getParameterArray()[0]), String.valueOf(advice.getParameterArray()[1]));

        HttpHeaders readOnlyHttpHeaders = HttpHeaders.readOnlyHttpHeaders(writableHttpHeaders);
        Object object = Cache.RequestHolder.get();
        if (object instanceof AbstractServerHttpRequest) {
            AbstractServerHttpRequest httpRequest = (AbstractServerHttpRequest) object;
//            ((HeaderSetter) httpRequest)._$PRADAR$_setHeader(readOnlyHttpHeaders);
        }

        return CutOffResult.cutoff(null);
    }
}
