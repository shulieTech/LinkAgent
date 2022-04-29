/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.shadowjob.interceptor;

import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarCoreUtils;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.internal.config.ShadowJob;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.IJobHandler;

/**
 * @Author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @package: com.pamirs.attach.plugin.shadowjob.interceptor
 * @Date 2020-03-18 16:03
 */
public class ProxyInterceptor extends IJobHandler {

    private ShadowJob shadowJob;

    private IJobHandler jobHandler;

    public ProxyInterceptor(ShadowJob shadowJob, IJobHandler jobHandler) {
        this.shadowJob = shadowJob;
        this.jobHandler = jobHandler;
    }

    private void serverRecv() {
        Pradar.startServerInvoke(shadowJob.getClassName(), shadowJob.getJobType(), null);
        Pradar.requestSize(0);
        Pradar.remoteIp(PradarCoreUtils.getLocalAddress());
    }

    private void serverSend(Throwable throwable) {
        Pradar.responseSize(0);
        if (Pradar.isResponseOn()) {
            Pradar.response(throwable);
        }
        Pradar.remoteIp(PradarCoreUtils.getLocalAddress());

        Pradar.endServerInvoke(throwable == null ? ResultCode.INVOKE_RESULT_SUCCESS : ResultCode.INVOKE_RESULT_FAILED, MiddlewareType.TYPE_JOB);
    }

    @Override
    public ReturnT<String> execute(String param) throws Exception {
        ReturnT<String> value = null;
        Throwable throwable = null;
        try {
            serverRecv();
            Pradar.setClusterTest(true);
            value = jobHandler.execute(param);
        } catch (Exception e) {
            throwable = e;
            throw e;
        } finally {
            serverSend(throwable);
            Pradar.setClusterTest(false);
        }
        return value;
    }
}
