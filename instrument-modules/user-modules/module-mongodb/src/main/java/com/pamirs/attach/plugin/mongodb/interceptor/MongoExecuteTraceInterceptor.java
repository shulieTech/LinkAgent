package com.pamirs.attach.plugin.mongodb.interceptor;

import com.mongodb.ServerAddress;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author angju
 * @date 2021/9/17 11:26
 */
public class MongoExecuteTraceInterceptor extends TraceInterceptorAdaptor{
    @Override
    public String getPluginName() {
        return "mongodb";
    }

    @Override
    public int getPluginType() {
        return 4;
    }

    private volatile Method getClusterMethod = null;

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setMiddlewareName("mongodb");
        Class operationClass = args[0].getClass();
        spanRecord.setMethod(operationClass.getSimpleName());
        if (getClusterMethod == null){
            synchronized (MongoExecuteTraceInterceptor.class){
                if (getClusterMethod == null){
                    try {
                        getClusterMethod = advice.getTarget().getClass().getSuperclass().getDeclaredMethod("getServerAddressList");
                        getClusterMethod.setAccessible(true);
                    } catch (Exception e) {
                        LOGGER.error("beforeTrace error ", e);
                    }
                }

            }
        }
        try {
            List<ServerAddress> list = (List<ServerAddress>)getClusterMethod.invoke(advice.getTarget());
            StringBuilder stringBuilder = new StringBuilder();
            for (ServerAddress serverAddress : list){
                stringBuilder.append(serverAddress.toString()).append(";");
            }
            spanRecord.setService(stringBuilder.toString());
        } catch (Exception e) {
            LOGGER.error("beforeTrace error ", e);
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
