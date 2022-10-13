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

package com.pamirs.attach.plugin.ehcache.interceptor;

import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/10/12 15:42
 */
public class BaseStoreCheckTypeInterceptor extends CutoffInterceptorAdaptor {

    @Override
    public CutOffResult cutoff0(Advice advice) throws Throwable {
        if (!PradarSwitcher.isClusterTestEnabled()) {
            return CutOffResult.passed();
        }
        if (!Pradar.isClusterTest()) {
            return CutOffResult.passed();
        } else {
            return CutOffResult.cutoff(null);
        }
    }
}
