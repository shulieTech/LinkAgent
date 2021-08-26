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
package com.pamirs.attach.plugin.pulsar.pub;

import com.pamirs.attach.plugin.pulsar.common.MQTraceBean;
import com.pamirs.attach.plugin.pulsar.common.MQTraceContext;
import com.pamirs.attach.plugin.pulsar.common.PradarLogUtils;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.shulie.instrument.simulator.api.util.CollectionUtils;

/**
 * 消息发送轨迹埋点
 *
 * @author: wangxian<tangdahu @ pamirs.top>
 * @date:2016/09/29
 */
public class MQSendMessageTraceLog {

    public static void sendMessageBefore(MQTraceContext ctx) {
        if (ctx == null || ctx.getTraceBeans() == null || ctx.getTraceBeans().size() == 0
                || ctx.getTraceBeans().get(0) == null) {
            return;
        }

        String serviceName = PradarLogUtils.getService(ctx);
        String methodName = PradarLogUtils.getMethod(ctx);

        Pradar.startClientInvoke(serviceName, methodName);

        MQTraceBean traceBean = ctx.getTraceBeans().get(0);
        traceBean.setContext(Pradar.getInvokeContextMap());
        if (CollectionUtils.isNotEmpty(ctx.getTraceBeans())) {
            Pradar.remoteIp(ctx.getTraceBeans().get(0).getStoreHost());
            Pradar.remotePort(ctx.getTraceBeans().get(0).getPort());
        }

        Pradar.requestSize(traceBean.getBodyLength());
        Pradar.middlewareName("pulsar");

        // 如果采用异步的方式提交消息，需要将Pradar的context在不同线程中进行传递（默认Pradar的context是存放在
        // ThreadLocal 中）
        if (ctx.isAsync()) {
            ctx.setRpcContextInner(Pradar.getInvokeContextMap());
            Pradar.popInvokeContext();
        }
    }

    public static void sendMessageAfter(MQTraceContext ctx) {
        if (ctx == null || ctx.getTraceBeans() == null || ctx.getTraceBeans().size() == 0
                || ctx.getTraceBeans().get(0) == null) {
            return;
        }

        // 批量消息全部处理完毕的平均耗时
        long costTime = (System.currentTimeMillis() - ctx.getStartTime()) / ctx.getTraceBeans().size();
        ctx.setCostTime(costTime);

        // 如果采用异步的方式提交消息，需要将 Pradar 的 context 在不同线程中进行传递
        // （默认 Pradar 的 context 是存放在 ThreadLocal 中）
        if (ctx.isAsync()) {
            Pradar.setInvokeContext(ctx.getRpcContextInner());
        }

        if (!ctx.isSuccess()) {
            Pradar.endClientInvoke(ResultCode.INVOKE_RESULT_FAILED, MiddlewareType.TYPE_MQ);
            return;
        }

        // 消息发送成功后追加MsgId
        if (Pradar.isResponseOn()) {
            Pradar.response(ctx.getTraceBeans().get(0).getMsgId());
        }
        Pradar.endClientInvoke(ResultCode.INVOKE_RESULT_SUCCESS, MiddlewareType.TYPE_MQ);
    }
}
