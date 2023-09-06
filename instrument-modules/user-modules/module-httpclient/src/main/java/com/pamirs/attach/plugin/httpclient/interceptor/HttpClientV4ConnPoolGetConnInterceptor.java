package com.pamirs.attach.plugin.httpclient.interceptor;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.http.HttpHost;

public class HttpClientV4ConnPoolGetConnInterceptor extends TraceInterceptorAdaptor {

    @Override
    public String getPluginName() {
        return "httpclient";
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

        Object route = advice.getTarget();
        if (route.getClass().getName().equals("org.apache.http.conn.routing.HttpRoute")) {
            HttpHost targetHost = ReflectionUtils.get(route, "targetHost");
            record.setService(targetHost.getHostName() + ":" + targetHost.getPort());
        }
        record.setMethod("AbstractConnPool#" + advice.getBehaviorName());
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
