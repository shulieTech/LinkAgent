package com.pamirs.attach.plugin.jms.interceptor;

import com.pamirs.attach.plugin.jms.JmsConstants;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.ReversedTraceInterceptorAdaptor;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

import javax.jms.ObjectMessage;
import javax.jms.TextMessage;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/05/14 11:37 上午
 */
public class QueueReceiverTraceInterceptor extends ReversedTraceInterceptorAdaptor {

    @Override
    protected boolean isClient(Advice advice) {
        return false;
    }

    @Override
    public String getPluginName() {
        return JmsConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return JmsConstants.PLUGIN_TYPE;
    }

    private static ThreadLocal<Object> RESPONSE = new ThreadLocal<Object>();
    private static ThreadLocal<Object> THROWABLE = new ThreadLocal<Object>();

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        final SpanRecord spanRecord = new SpanRecord();
        final Object returnObj = advice.getReturnObj();
        try{
            if(returnObj != null){
                if(returnObj instanceof ObjectMessage){
                    final Serializable object = ((ObjectMessage)returnObj).getObject();
                    if(object != null){
                        spanRecord.setResponse(object);
                    }
                }else if(returnObj instanceof TextMessage){
                    final String text = ((TextMessage)returnObj).getText();
                    if(text != null && !"".equals(text)){
                        spanRecord.setResponse(text);
                    }
                }
            }
        } catch (Exception e){
            LOGGER.error("jms set response error", e);
        }
        if(spanRecord.getResponse() == null){
            return null;
        }else {
            RESPONSE.set(spanRecord.getResponse());
        }

        if(advice.getThrowable() != null){
            THROWABLE.set(advice.getThrowable());
        }
        spanRecord.setService(LookupInterceptor.JNDI_NAME.get());
        LookupInterceptor.JNDI_NAME.remove();
        record(spanRecord, advice);
        return spanRecord;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        SpanRecord spanRecord = new SpanRecord();
        if(THROWABLE.get() == null) {
            spanRecord.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
            spanRecord.setResponse(RESPONSE.get());
        }else {
            spanRecord.setResponse(ResultCode.INVOKE_RESULT_FAILED);
            spanRecord.setResponse(THROWABLE.get());
        }
        THROWABLE.remove();
        RESPONSE.remove();
        return spanRecord;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        Throwable throwable = advice.getThrowable();
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setResponse(throwable);
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        return spanRecord;
    }

    private void record(SpanRecord spanRecord, Advice advice){
        final ObjectMessage returnObj = (ObjectMessage)advice.getReturnObj();
        try {
            final Map<String, String> map = new HashMap<String, String>();
            for (String invokeContextTransformKey : Pradar.getInvokeContextTransformKeys()) {
                final String stringProperty = returnObj.getStringProperty(
                    invokeContextTransformKey.replaceAll("-", "_"));
                map.put(invokeContextTransformKey, stringProperty);
            }
            spanRecord.setContext(map);
        } catch (Exception e){
            LOGGER.error("jms record error", e);
        }
    }

}
