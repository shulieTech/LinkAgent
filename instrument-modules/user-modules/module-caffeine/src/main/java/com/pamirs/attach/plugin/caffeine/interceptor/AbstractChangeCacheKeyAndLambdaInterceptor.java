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
package com.pamirs.attach.plugin.caffeine.interceptor;

import com.pamirs.attach.plugin.caffeine.utils.WrapLambda;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/03/30 8:07 下午
 */
public abstract class AbstractChangeCacheKeyAndLambdaInterceptor extends AbstractChangeCacheKeyTraceInterceptor {

    @Override
    public Object[] doGetParameter(Advice advice) {
        Object[] args = super.doGetParameter(advice);
        int lambdaIndex = getLambdaIndex(args);
        Object lambda = advice.getParameterArray()[lambdaIndex];
        if (lambda instanceof WrapLambda) {
            return args;
        }
        args[lambdaIndex] = wrapLambda(lambda);
        return args;
    }

    protected abstract Object wrapLambda(Object lambda);

    protected abstract int getLambdaIndex(Object[] args);
}
