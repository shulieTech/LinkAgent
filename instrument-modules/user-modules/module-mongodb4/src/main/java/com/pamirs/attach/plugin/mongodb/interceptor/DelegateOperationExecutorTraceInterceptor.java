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
package com.pamirs.attach.plugin.mongodb.interceptor;

import com.mongodb.client.internal.MongoClientDelegate;
import com.pamirs.attach.plugin.mongodb.utils.Caches;
import com.pamirs.attach.plugin.mongodb.utils.OperationAccessor;
import com.pamirs.attach.plugin.mongodb.utils.OperationAccessorFactory;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.StringUtils;

/**
 * @author angju
 * @date 2021/3/31 21:19
 */
public class DelegateOperationExecutorTraceInterceptor extends TraceInterceptorAdaptor {

    @Override
    public String getPluginName() {
        return "mongodb";
    }

    @Override
    public int getPluginType() {
        return 4;
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setMiddlewareName("mongodb");
        Class operationClass = args[0].getClass();
        spanRecord.setMethod(operationClass.getSimpleName());
        spanRecord.setService("unknown");
        OperationAccessor operationAccessor = OperationAccessorFactory.getOperationAccessor(operationClass);
        if (operationAccessor != null) {
            try {
                spanRecord.setService((operationAccessor.getMongoNamespace(args[0]).getFullName()));
            } catch (Exception e) {
                LOGGER.error("not support operation class is {} ", args[0].getClass().getName());
            }
        }
        try {
            MongoClientDelegate mongoClientDelegate = Caches.getMongoClientDelegate(advice.getTarget());
            String addressesStr = StringUtils.join(mongoClientDelegate.getCluster().getSettings().getHosts(), ",");
            spanRecord.setRemoteIp(addressesStr);
        } catch (Exception e) {
            LOGGER.error("can not get mongoClientDelegate");
        }
        return spanRecord;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setResultCode("200");
        return spanRecord;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setResponse(advice.getThrowable());
        spanRecord.setResultCode("500");
        return spanRecord;
    }
}
