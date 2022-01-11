package com.pamirs.attach.plugin.jms.interceptor;

import com.pamirs.attach.plugin.jms.JmsConstants;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.ContextTransfer;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.shulie.instrument.simulator.api.ProcessControlException;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

public class SendTraceInterceptor extends TraceInterceptorAdaptor {
    protected final static Logger LOGGER = LoggerFactory.getLogger(SendTraceInterceptor.class.getName());

    @Override
    public String getPluginName() {
        return JmsConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return MiddlewareType.TYPE_MQ;
    }

    @Override
    public void beforeFirst(Advice advice) throws Exception {
        ClusterTestUtils.validateClusterTest();
        if(!Pradar.isClusterTest() && Pradar.isClusterTestPrefix(LookupInterceptor.JNDI_NAME.get())){
            LOGGER.error("jms 流量状态异常0");
            throw new PressureMeasureError("流量状态异常0");
        }
        if(Pradar.isClusterTest() && !Pradar.isClusterTestPrefix(LookupInterceptor.JNDI_NAME.get())){
            LOGGER.error("流量状态异常1");
            throw new PressureMeasureError("流量状态异常1");
        }
        super.beforeFirst(advice);
    }

    @Override
    public void beforeLast(Advice advice) throws ProcessControlException {
        super.beforeLast(advice);
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setService(LookupInterceptor.JNDI_NAME.get());
        LookupInterceptor.JNDI_NAME.remove();
        spanRecord.setRemoteIp("jms");
        try {
            if (advice.getParameterArray()[0] instanceof ObjectMessage) {
                ObjectMessage message = (ObjectMessage)advice.getParameterArray()[0];
                spanRecord.setRequest(message.getObject());
            }
            if (advice.getParameterArray()[0] instanceof TextMessage) {
                TextMessage message = (TextMessage)advice.getParameterArray()[0];
                spanRecord.setRequest(message.getText());
            }
            } catch(Exception e){
                LOGGER.warn("jms trace setRequest fail", e);
            }
        return spanRecord;
    }

    @Override
    public void afterFirst(Advice advice) throws ProcessControlException {
        super.afterFirst(advice);
    }

    @Override
    public void afterLast(Advice advice) {
        super.afterLast(advice);
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setResponse(advice.getReturnObj());
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
        if (Pradar.getInvokeContext() != null) {
            Pradar.getInvokeContext().setClusterTest(Pradar.isClusterTest());
        }
        return spanRecord;
    }

    @Override
    public void exceptionFirst(Advice advice) {
        super.exceptionFirst(advice);
    }

    @Override
    public void exceptionLast(Advice advice) {
        super.exceptionLast(advice);
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        spanRecord.setResponse(advice.getThrowable().getMessage());
        return spanRecord;

    }

    @Override
    protected ContextTransfer getContextTransfer(Advice advice) {
        Object[] args = advice.getParameterArray();
        final ObjectMessage objectMessage = (ObjectMessage)args[0];
        return new ContextTransfer() {
            @Override
            public void transfer(String key, String value) {
                try {
                    objectMessage.setStringProperty(key.replaceAll("-", "_"), value);
                } catch (Exception e) {
                    LOGGER.error(String.format("jms getContextTransfer error,key:%s,value:%s", key, value), e);
                }
            }
        };
    }
}

