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
package com.pamirs.attach.plugin.shadowjob.destory;

import com.pamirs.attach.plugin.shadowjob.common.LtsJobTrackerAppContext;
import com.pamirs.attach.plugin.shadowjob.common.api.RegistryCenterFactory;
import com.pamirs.attach.plugin.shadowjob.common.lts.JobRunnerShadowRegistry;
import com.pamirs.attach.plugin.shadowjob.interceptor.JobRunShellInitializeInterceptor_1;
import com.shulie.instrument.simulator.api.listener.Destroyed;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/06/16 3:20 下午
 */
public class JobDestroy implements Destroyed {
    @Override
    public void destroy() {
        LtsJobTrackerAppContext.release();
        JobRunnerShadowRegistry.release();
        RegistryCenterFactory.release();
    }
}
