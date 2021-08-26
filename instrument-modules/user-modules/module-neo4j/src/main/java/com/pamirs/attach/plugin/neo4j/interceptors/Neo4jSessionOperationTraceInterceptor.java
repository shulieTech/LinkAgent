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
package com.pamirs.attach.plugin.neo4j.interceptors;

import com.pamirs.attach.plugin.neo4j.Neo4JConstants;
import com.pamirs.attach.plugin.neo4j.destroy.Neo4jDestroy;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @ClassName: GraphRepositoryImplOperationTraceInterceptor
 * @author: wangjian
 * @Date: 2020/7/29 21:41
 * @Description:
 */
@Destroyable(Neo4jDestroy.class)
public class Neo4jSessionOperationTraceInterceptor extends TraceInterceptorAdaptor {

    @Override
    public String getPluginName() {
        return Neo4JConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return Neo4JConstants.PLUGIN_TYPE;
    }

    private String getMethodName(String methodName, Object[] args) {
        if (args != null && args.length != 0) {
            return methodName + '-' + args[0];
        }
        return methodName;
    }

    // trace
    @Override
    public SpanRecord beforeTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        String methodName = advice.getBehavior().getName();
        Object target = advice.getTarget();
        SpanRecord record = new SpanRecord();
        record.setService(target.getClass().getName());
        record.setMethod(getMethodName(methodName, args));
        record.setRequest(args);
        return record;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        String methodName = advice.getBehavior().getName();
        Object target = advice.getTarget();
        Object result = advice.getReturnObj();
        SpanRecord record = new SpanRecord();
        record.setService(target.getClass().getName());
        record.setMethod(getMethodName(methodName, args));
        record.setRequest(args);
        record.setResponse(result);
        record.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
        return record;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        String methodName = advice.getBehavior().getName();
        Object target = advice.getTarget();
        SpanRecord record = new SpanRecord();
        record.setService(target.getClass().getName());
        record.setMethod(getMethodName(methodName, args));
        record.setRequest(args);
        record.setResponse(advice.getThrowable());
        record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        return record;
    }

}
