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
package com.pamirs.attach.plugin.aerospike.interceptor;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.StringUtils;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/8/14 11:16 下午
 */
public class ScanNodeParameterWrapInterceptor extends ParametersWrapperInterceptorAdaptor {
    @Override
    protected Object[] getParameter0(Advice advice) {
        Object[] args = advice.getParameterArray();
        String namespace = (String) args[2];
        /**
         * 如果业务不使用namespace，则使用setName来进行隔离
         */
        if (StringUtils.isBlank(namespace)) {
            String setName = (String) args[3];
            if (!Pradar.isClusterTestPrefix(setName)) {
                setName = Pradar.addClusterTestPrefix(setName);
                args[3] = setName;
            }
        } else {
            if (!Pradar.isClusterTestPrefix(namespace)) {
                namespace = Pradar.addClusterTestPrefix(namespace);
                args[2] = namespace;
            }
        }
        return args;
    }
}
