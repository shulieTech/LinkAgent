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
package com.pamirs.attach.plugin.sentinel.interceptor;

import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;

import com.pamirs.attach.plugin.sentinel.SentinelConstants;
import com.pamirs.pradar.InvokeContext;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/5/28 6:02 下午
 */
public class SphUEntryInterceptor extends AroundInterceptor {

    @Override
    public void doBefore(Advice advice) throws Throwable {
        InvokeContext invokeContext = Pradar.getInvokeContext();
        while (invokeContext != null && !isServer(invokeContext)) {
            invokeContext = invokeContext.getParentInvokeContext();
        }
        if (invokeContext == null) {
            return;
        }
        String resource = (String)advice.getParameterArray()[0];
        boolean flow = FlowRuleManager.hasConfig(resource);
        boolean degrade = DegradeRuleManager.hasConfig(resource);

        invokeContext.putLocalAttribute("sentinel.hasFlowRule", flow ? "1" : "0");
        invokeContext.putLocalAttribute("sentinel.hasDegradeRule", degrade ? "1" : "0");
    }

    @Override
    public void doException(Advice advice) throws Throwable {
        Throwable e = advice.getThrowable();
        String resource = (String)advice.getParameterArray()[0];
        if (e instanceof DegradeException) {
            record(resource, e, "02");
        }
        if (e instanceof FlowException) {
            record(resource, e, "01");
        }
    }

    private static void record(String resource, Throwable e, String number) {
        Pradar.startClientInvoke(resource, "entry");
        Pradar.middlewareName(SentinelConstants.PLUGIN_NAME);
        Pradar.response(e.getClass().getName());
        Pradar.endClientInvoke(number, SentinelConstants.PLUGIN_TYPE);
    }

    private static boolean isServer(InvokeContext invokeContext) {
        return invokeContext.getLogType() == Pradar.LOG_TYPE_INVOKE_SERVER
            || invokeContext.getLogType() == Pradar.LOG_TYPE_TRACE;
    }

}
