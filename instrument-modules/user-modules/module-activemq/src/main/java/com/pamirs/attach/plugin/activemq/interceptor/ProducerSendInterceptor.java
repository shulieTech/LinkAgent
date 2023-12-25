package com.pamirs.attach.plugin.activemq.interceptor;

import com.pamirs.attach.plugin.activemq.ActiveMQConstants;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.ContextTransfer;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.activemq.ActiveMQSession;
import org.apache.activemq.command.ActiveMQDestination;

import javax.jms.JMSException;
import javax.jms.Message;

/**
 * @author guann1n9
 * @date 2023/12/21 1:44 PM
 */
public class ProducerSendInterceptor extends TraceInterceptorAdaptor {

    @Override
    public String getPluginName() {
        return ActiveMQConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return ActiveMQConstants.PLUGIN_TYPE;
    }



    @Override
    protected ContextTransfer getContextTransfer(Advice advice) {
        Object[] args = advice.getParameterArray();
        final Message message = (Message)args[2];
        return new ContextTransfer() {
            @Override
            public void transfer(String key, String value) {
                try {
                    message.setStringProperty(key, value);
                } catch (JMSException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }


    @Override
    public SpanRecord beforeTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        ActiveMQSession target = (ActiveMQSession) advice.getTarget();
        String brokerUrl = target.getConnection().getBrokerInfo().getBrokerURL();
        ActiveMQDestination destination = (ActiveMQDestination) args[1];
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setService(destination.getQualifiedName());
        spanRecord.setMethod(advice.getBehaviorName());
        spanRecord.setRemoteIp(brokerUrl);
        return spanRecord;
    }


    @Override
    public SpanRecord afterTrace(Advice advice) {
        Object[] args = advice.getParameterArray();

        final Message message = (Message)args[2];
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setRequest(message);
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
        return spanRecord;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        Throwable throwable = advice.getThrowable();
        final Message message = (Message)args[2];
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setRequest(message);
        spanRecord.setResponse(throwable);
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        return spanRecord;
    }


}
