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
import java.lang.reflect.Method;
import java.util.List;

import com.mongodb.client.MongoCollection;
import com.pamirs.attach.plugin.mongodb.MongodbConstants;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.util.ReflectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoCollectionTraceInterceptor extends TraceInterceptorAdaptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoCollectionTraceInterceptor.class.getName());

    @Override
    public String getPluginName() {
        return MongodbConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return MongodbConstants.PLUGIN_TYPE;
    }

    private Field executor;
    private Method getAllAddressMethod;
    private Field mongo$2;

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        Object target = advice.getTarget();
        if (executor == null) {
            final Field executor = ReflectionUtils.getDeclaredField(target, "executor");
            executor.setAccessible(true);
            this.executor = executor;
        }
        MongoCollection mongoCollection = (MongoCollection)target;
        SpanRecord record = new SpanRecord();
        record.setRequest(advice.getParameterArray());
        record.setService(mongoCollection.getNamespace().getFullName());
        record.setMethod(advice.getBehaviorName());
        try {
            Object mongo = executor.get(target);
            if (getAllAddressMethod == null) {
                getAllAddressMethod = ReflectionUtils.getDeclaredMethod(mongo,"getAllAddress",null);
            }
            if(getAllAddressMethod == null){
                if(mongo$2 == null) {
                    final Field this$0 = mongo.getClass().getDeclaredField("this$0");
                    this$0.setAccessible(true);
                    mongo$2 = this$0;
                }
                mongo = mongo$2.get(mongo);
                getAllAddressMethod = ReflectionUtils.getDeclaredMethod(mongo,"getAllAddress",null);
            }
            if("com.mongodb.Mongo$2".equals(mongo.getClass().getName())){
                mongo = mongo$2.get(mongo);
            }
            try {
                record.setRemoteIp(StringUtils.join((List)getAllAddressMethod.invoke(mongo), ","));
            } catch (Throwable e) {
                LOGGER.error("mongodb trace 获取数据库地址失败",e);
            }
        }catch (Throwable e){
            LOGGER.error("mongodb trace error",e);
        }
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
