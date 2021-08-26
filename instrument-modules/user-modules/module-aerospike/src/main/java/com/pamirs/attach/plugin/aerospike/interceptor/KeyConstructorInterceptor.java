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

/**
 * 修改com.aerospike.client.Key构造函数, 在创建对象时添加压测标识
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/10/11 3:32 下午
 */
public class KeyConstructorInterceptor extends ParametersWrapperInterceptorAdaptor {
    @Override
    public Object[] getParameter0(Advice advice) {
        /**
         * 修改第二个参数即可
         */
        Object[] args = advice.getParameterArray();
        if (args.length < 2 || !(args[1] instanceof String)) {
            return args;
        }
        String setName = (String) args[1];
        if (!Pradar.isClusterTestPrefix(setName)) {
            setName = Pradar.addClusterTestPrefix(setName);
        }
        args[1] = setName;
        return args;
    }
}
