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
package com.pamirs.attach.plugin.mongodb.interceptor.mongo2_14_3;

import java.util.List;

import com.mongodb.DBCollection;
import com.mongodb.ServerAddress;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.StringUtils;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/05/21 6:33 下午
 */
public class DBCollectionTraceInterceptor extends TraceInterceptorAdaptor {

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
        DBCollection coll = (DBCollection)advice.getTarget();
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setMethod(advice.getBehaviorName());
        List<ServerAddress> serverAddresses = coll.getDB().getMongo().getAllAddress();
        spanRecord.setRemoteIp(StringUtils.join(serverAddresses, ","));
        spanRecord.setRequest(coll.getName());
        return spanRecord;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
        return spanRecord;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        return spanRecord;
    }
}
