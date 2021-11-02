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

import java.lang.reflect.Field;
import java.util.List;

import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.internal.MongoClientDelegate;
import com.mongodb.connection.ClusterSettings;
import com.pamirs.attach.plugin.mongodb.MongodbConstants;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.util.ReflectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoCollectionInternalTraceInterceptor extends TraceInterceptorAdaptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        MongoCollectionInternalTraceInterceptor.class.getName());

    @Override
    public String getPluginName() {
        return MongodbConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return MongodbConstants.PLUGIN_TYPE;
    }

    private MongoClientDelegate mongoClientDelegate = null;
    private Field executorField = null;

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        Object target = advice.getTarget();
        if(executorField == null){
            final Field tempField = ReflectionUtils.getDeclaredField(target, "executor");
            tempField.setAccessible(true);
            this.executorField = tempField;
        }

        Object executor = null;
        try {
            executor = executorField.get(target);
        } catch (IllegalAccessException e) {
            LOGGER.error("mongodb trace error",e);
        }
        if (mongoClientDelegate == null) {
            Field field = null;
            try {
                field = executor.getClass().getDeclaredField("this$0");
                field.setAccessible(true);
                mongoClientDelegate = (MongoClientDelegate)field.get(executor);
            } catch (Throwable e) {
                LOGGER.error(String.format("DelegateOperationExecutorInterceptor error,class:%s", executor.getClass().getName()) , e);
            } finally {
                if (field != null) {
                    field.setAccessible(false);
                }
            }
        }
        MongoCollection mongoCollection = (MongoCollection)target;
        SpanRecord record = new SpanRecord();
        ClusterSettings clusterSettings = (ClusterSettings)ReflectionUtils.getFieldValue(ReflectionUtils.getFieldValue(mongoClientDelegate,"cluster"),"settings");
        List<ServerAddress> serverAddresses = clusterSettings.getHosts();
        record.setService(mongoCollection.getNamespace().getFullName());
        record.setRequest(advice.getParameterArray());
        record.setMethod(advice.getBehaviorName());
        record.setRemoteIp(StringUtils.join(serverAddresses, ","));
        return record;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        SpanRecord record = new SpanRecord();
        record.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
        record.setRequest(advice.getParameterArray());
        record.setResponse(advice.getReturnObj());
        return record;

    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        SpanRecord record = new SpanRecord();
        record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        record.setRequest(advice.getParameterArray());
        record.setResponse(advice.getThrowable());
        return record;
    }

}
