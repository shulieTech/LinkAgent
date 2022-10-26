/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pamirs.attach.plugin.feign.interceptor;

import com.pamirs.pradar.flag.GuavaCacheSkipFlag;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/10/11 14:10
 */
public class GuavaCacheSkipInterceptor extends AroundInterceptor {

    @Override
    public void doBefore(Advice advice) throws Throwable {
        GuavaCacheSkipFlag.skip.set(true);
    }

    @Override
    public void doAfter(Advice advice) throws Throwable {
        GuavaCacheSkipFlag.skip.set(false);
    }

    @Override
    public void doException(Advice advice) throws Throwable {
        GuavaCacheSkipFlag.skip.set(false);
    }
}
