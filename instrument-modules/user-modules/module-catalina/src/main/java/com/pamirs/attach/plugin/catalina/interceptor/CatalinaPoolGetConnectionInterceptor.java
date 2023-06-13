package com.pamirs.attach.plugin.catalina.interceptor;

import com.pamirs.attach.plugin.catalina.CatalinaConstans;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

public class CatalinaPoolGetConnectionInterceptor extends TraceInterceptorAdaptor {

    @Override
    public String getPluginName() {
        return CatalinaConstans.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return MiddlewareType.TYPE_WEB_SERVER;
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        if (Pradar.getInvokeContext() == null) {
            return null;
        }
        SpanRecord record = new SpanRecord();
        record.setMethod("TomcatConnectionPool#getConnection");
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
