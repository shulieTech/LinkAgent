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
package com.pamirs.attach.plugin.shadowjob.interceptor;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.StringUtils;

/**
 * @author angju
 * @date 2021/3/22 20:24
 */
public class MethodJobHandlerExecuteInterceptor extends ParametersWrapperInterceptorAdaptor {

    @Override
    public Object[] getParameter0(Advice advice) throws Throwable {
        Pradar.setClusterTest(false);
        Pradar.startTrace(null, advice.getTarget().getClass().getName(), advice.getBehavior().getName());
        Object[] args = advice.getParameterArray();
        String param = (String) args[0];
        if (StringUtils.isNotBlank(param) && param.contains("performance=true")){
            Pradar.setClusterTest(true);
            args[0] = param.replace("performance=true", "");
        }
        return args;
    }

    @Override
    public void doAfter(Advice advice) throws Throwable {
        Pradar.setClusterTest(false);
        Pradar.endTrace();
    }

}
