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
package com.pamirs.attach.plugin.log4j.interceptor.v1;

import com.pamirs.attach.plugin.log4j.destroy.Log4jDestroy;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;

/**
 * @Auther: vernon
 * @Date: 2020/12/9 12:11
 * @Description:
 */
@Destroyable(Log4jDestroy.class)
public class AppenderV1RouterInterceptor extends CutoffInterceptorAdaptor {
    protected boolean isBusinessLogOpen;
    protected String bizShadowLogPath;

    public AppenderV1RouterInterceptor(boolean isBusinessLogOpen, String bizShadowLogPath) {
        this.isBusinessLogOpen = isBusinessLogOpen;
        this.bizShadowLogPath = bizShadowLogPath;
    }

    @Override
    public CutOffResult cutoff0(Advice advice) {
        if (!isBusinessLogOpen) {
            return CutOffResult.passed();
        }

        /*   ClusterTestUtils.validateClusterTest();*/
        Object target = advice.getTarget();
        if (!(target instanceof Appender)) {
            return CutOffResult.passed();
        }
        if (!(target instanceof FileAppender)) {
            return CutOffResult.passed();
        }
        FileAppender appender = (FileAppender) target;
        String name = appender.getName();
        //压测流量放行压测的appender
        if (Pradar.isClusterTest()) {
            if (name.startsWith(Pradar.CLUSTER_TEST_PREFIX)) {
                return CutOffResult.passed();
            }
            return CutOffResult.cutoff(null);
        }
        //业务流量放行业务的appender
        if (name.startsWith(Pradar.CLUSTER_TEST_PREFIX)) {
            return CutOffResult.cutoff(null);
        }
        return CutOffResult.passed();
    }
}
