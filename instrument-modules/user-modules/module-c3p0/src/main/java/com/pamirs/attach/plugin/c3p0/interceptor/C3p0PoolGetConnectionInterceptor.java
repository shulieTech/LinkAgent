package com.pamirs.attach.plugin.c3p0.interceptor;

import com.pamirs.pradar.pressurement.datasource.util.JdbcUrlParser;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.SyncObjectService;
import com.pamirs.pradar.bean.SyncObject;
import com.pamirs.pradar.bean.SyncObjectData;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.druid.util.JdbcUtils;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

import java.util.HashMap;
import java.util.Map;

public class C3p0PoolGetConnectionInterceptor extends TraceInterceptorAdaptor {

    private static Map<Integer, String> jdbcUrlCache = new HashMap<Integer, String>();

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
        record.setService(getJdbcUrl(advice.getTarget()));
        record.setMethod("C3P0PooledConnectionPool#" + advice.getBehaviorName());
        record.setRequest(advice.getParameterArray());
        Map.Entry<String, String> hostIp = JdbcUrlParser.extractUrl(record.getService());
        record.setRemoteIp(hostIp.getKey());
        record.setPort(hostIp.getValue());
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

    private String getJdbcUrl(Object pool) {
        int hashCode = System.identityHashCode(pool);
        if (jdbcUrlCache.containsKey(hashCode)) {
            return jdbcUrlCache.get(hashCode);
        }
        SyncObject syncObject = SyncObjectService.getSyncObject("com.mchange.v2.c3p0.impl.C3P0PooledConnectionPool");
        if (syncObject == null) {
            return null;
        }
        SyncObjectData objectData = null;
        for (SyncObjectData data : syncObject.getDatas()) {
            if (pool.equals(data.getTarget())) {
                objectData = data;
                break;
            }
        }
        if (objectData == null) {
            return null;
        }
        Object dataSource = objectData.getArgs()[0];
        String jdbcUrl = ReflectionUtils.getFieldValues(dataSource, "nestedDataSource", "jdbcUrl");
        jdbcUrlCache.put(hashCode, jdbcUrl);
        return jdbcUrl;
    }
}
