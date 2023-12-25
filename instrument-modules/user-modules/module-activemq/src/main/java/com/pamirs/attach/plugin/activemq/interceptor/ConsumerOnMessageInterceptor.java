package com.pamirs.attach.plugin.activemq.interceptor;

import com.pamirs.attach.plugin.activemq.ActiveMQConstants;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.activemq.command.Message;
import org.apache.activemq.command.MessageDispatch;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author guann1n9
 * @date 2023/12/21 2:59 PM
 */
public class ConsumerOnMessageInterceptor extends TraceInterceptorAdaptor {


    List<String> TRACE_HEADERS = Pradar.getInvokeContextTransformKeys();


    @Override
    public String getPluginName() {
        return ActiveMQConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return ActiveMQConstants.PLUGIN_TYPE;
    }


    @Override
    protected boolean isClient(Advice advice) {
        return false;
    }

    /**
     * 开始消费前清空当前上下文
     * @param advice
     * @throws Exception
     */
    @Override
    public void beforeFirst(Advice advice) throws Exception {
        if(Pradar.getInvokeContext() != null){
            Pradar.endServerInvoke(ResultCode.INVOKE_RESULT_SUCCESS, ActiveMQConstants.PLUGIN_TYPE);
        }
        Pradar.clearInvokeContext();
    }


    @Override
    public SpanRecord beforeTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        MessageDispatch md = (MessageDispatch) args[0];
        if(md == null){
            return null;
        }
        Message message = md.getMessage();
        if(message == null){
            return null;
        }

        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setRequest(message);
        spanRecord.setMethod(advice.getBehaviorName());
        spanRecord.setService(message.getDestination().getQualifiedName());
        spanRecord.setRemoteIp(message.getConnection().getBrokerInfo().getBrokerURL());
        Map<String, String> ctx = getContextFromProperties(message);
        spanRecord.setContext(ctx);
        return spanRecord;
    }


    /**
     * 从消息中获取trace上下文
     * @param message
     * @return
     */
    private Map<String,String> getContextFromProperties(Message message) {
        Map<String, Object> properties = null;
        try {
            properties = message.getProperties();
        } catch (IOException e) {
           return null;
        }
        if(properties == null || properties.isEmpty()){
            return null;
        }
        Map<String, String> context = new HashMap<String, String>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (!TRACE_HEADERS.contains(entry.getKey())) {
                continue;
            }
            context.put(entry.getKey(), entry.getValue().toString());
        }
        return context;
    }


    @Override
    public SpanRecord afterTrace(Advice advice) {
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
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
}