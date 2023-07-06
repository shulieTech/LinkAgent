package com.pamirs.attach.plugin.alibaba.druid.interceptor;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.druid.util.JdbcUtils;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

public class DruidGetConnectionDirectInterceptor extends TraceInterceptorAdaptor {

    private static ThreadLocal<String> dbType = new ThreadLocal<String>();

    @Override
    public String getPluginName() {
        return dbType.get();
    }

    @Override
    public int getPluginType() {
        return MiddlewareType.TYPE_DB;
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        if (Pradar.getInvokeContext() == null) {
            return null;
        }
        SpanRecord record = new SpanRecord();
        record.setService(ReflectionUtils.<String>get(advice.getTarget(), "jdbcUrl"));
        record.setMethod("DruidDataSource#" + advice.getBehaviorName());
        record.setRequest(advice.getParameterArray());
        dbType.set(JdbcUtils.getDbType(record.getService(), null));
        return record;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        if (Pradar.getInvokeContext() == null) {
            return null;
        }
        SpanRecord record = new SpanRecord();
        record.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
        return record;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        if (Pradar.getInvokeContext() == null) {
            return null;
        }
        SpanRecord record = new SpanRecord();
        record.setResponse(advice.getThrowable());
        return record;
    }
}
