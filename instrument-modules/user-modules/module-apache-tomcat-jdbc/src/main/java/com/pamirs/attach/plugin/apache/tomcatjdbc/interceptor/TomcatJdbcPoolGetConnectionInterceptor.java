package com.pamirs.attach.plugin.apache.tomcatjdbc.interceptor;

import com.pamirs.attach.plugin.apache.tomcatjdbc.ApacheTomcatJdbcConstants;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.tomcat.jdbc.pool.ConnectionPool;

public class TomcatJdbcPoolGetConnectionInterceptor extends TraceInterceptorAdaptor {

    @Override
    public String getPluginName() {
        return ApacheTomcatJdbcConstants.MODULE_NAME;
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
        ConnectionPool target = (ConnectionPool) advice.getTarget();
        String url = target.getPoolProperties().getUrl();
        record.setService(url);
        record.setMethod("ConnectionPool#" + advice.getBehaviorName());
        record.setRequest(advice.getParameterArray());
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
