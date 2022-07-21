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
package com.pamirs.attach.plugin.redisson.interceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.pamirs.attach.plugin.redisson.RedissonConstants;
import com.pamirs.attach.plugin.redisson.destroy.RedissonDestroy;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.redisson.RedissonObject;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/9/8 2:05 下午
 */
@Destroyable(RedissonDestroy.class)
public class RedissonTraceMethodInterceptor extends BaseRedissonTimeSeriesMethodInterceptor {

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        String methodName = advice.getBehaviorName();
        Object target = advice.getTarget();
        SpanRecord record = new SpanRecord();
        record.setRemoteIp(getHost(target, methodName, args));
        record.setPort(getPort(target, methodName, args));
        record.setMiddlewareName(RedissonConstants.MIDDLEWARE_NAME);
        record.setService(String.valueOf(getDatabase(target, methodName, args)));
        record.setMethod(target.getClass().getName() + "." + methodName);
        if (target instanceof RedissonObject) {
            String name = ((RedissonObject)target).getName();
            List<Object> list = new ArrayList<Object>();
            list.add(name);
            list.addAll(Arrays.asList(args));
            record.setRequest(list.toArray());
        } else {
            record.setRequest(toArgs(args));
        }
        return record;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        Object result = advice.getReturnObj();
        SpanRecord record = new SpanRecord();
        record.setMiddlewareName(RedissonConstants.MIDDLEWARE_NAME);
        record.setResponse(result);
        record.setCallbackMsg(getPluginName());
        attachment(advice.getTarget(), advice.getBehaviorName(), advice.getParameterArray());
        return record;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        SpanRecord record = new SpanRecord();
        record.setMiddlewareName(RedissonConstants.MIDDLEWARE_NAME);
        record.setResponse(advice.getThrowable());
        record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        record.setCallbackMsg(getPluginName());
        attachment(advice.getTarget(), advice.getBehaviorName(), advice.getParameterArray());
        return record;
    }
}
