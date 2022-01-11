package com.pamirs.attach.plugin.jms.interceptor;

import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.concurrent.Callable;

public class QueueReceiverInterceptor extends CutoffInterceptorAdaptor {
    protected final static Logger LOGGER = LoggerFactory.getLogger(QueueReceiverInterceptor.class.getName());

    @Override
    public CutOffResult cutoff0(Advice advice) throws Throwable {
        QueueReceiver queueReceiver = (QueueReceiver)(advice.getTarget());
        final String jndiName = LookupInterceptor.JNDI_NAME.get();
        try {
            // 如果不在白名单内则放行
            if (!PradarSwitcher.whiteListSwitchOn() || !GlobalConfig.getInstance().getMqWhiteList().contains(
                jndiName + "#" + jndiName)) {
                // 如果是压测但是不在白名单就抛出异常
                if (Pradar.isClusterTestPrefix(jndiName)) {
                    LOGGER.error(String.format("jms queue:%s not in mqWhiteList:%s", jndiName,
                        GlobalConfig.getInstance().getMqWhiteList()));
                    throw new PressureMeasureError(
                        String.format("jms queue:%s not in mqWhiteList:%s", jndiName,
                            GlobalConfig.getInstance().getMqWhiteList()));
                }
                return CutOffResult.passed();
            }
            Serializable message = null;
            ObjectMessage returnObj = null;
            // 非压测流量先获取业务数据
            if (!Pradar.isClusterTest()) {
                returnObj = (ObjectMessage)queueReceiver.receiveNoWait();
                if (returnObj != null) {
                    message = returnObj.getObject();
                }
            }
            // 如果业务数据是空的就查询压测数据
            if (message == null || "".equals(message)) {
                final Callable<QueueConnection> queueConnectionCallable
                    = QueueConnectionFactoryInterceptor.callableOfGetQueueConnectionThreadLocal.get();
                final QueueConnection queueConnection = queueConnectionCallable.call();
                final QueueSession queueSession = queueConnection.createQueueSession(false, 1);
                final QueueReceiver receiver = queueSession.createReceiver(
                    LookupInterceptor.applyFunctionOfGetQueueThreadLocal.get().apply());
                queueConnection.start();
                final ObjectMessage ptObj = (ObjectMessage)receiver.receiveNoWait();
                receiver.close();
                queueSession.close();
                queueConnection.close();
                if (ptObj != null) {
                    return CutOffResult.cutoff(ptObj);
                }
            }
            // 如果压测数据也是空的。就继续查询业务数据。
            if (returnObj == null) {
                LookupInterceptor.JNDI_NAME.set(jndiName);
                returnObj = (ObjectMessage)queueReceiver.receive(3000L);
            }
            return CutOffResult.cutoff(returnObj == null ? emptyObjectMessage : returnObj);
        } finally {
            QueueConnectionFactoryInterceptor.callableOfGetQueueConnectionThreadLocal.remove();
            LookupInterceptor.applyFunctionOfGetQueueThreadLocal.remove();
        }
    }

    private static final ObjectMessage emptyObjectMessage = new ObjectMessage() {
        @Override
        public void setObject(Serializable object) throws JMSException {

        }

        @Override
        public Serializable getObject() throws JMSException {
            return null;
        }

        @Override
        public String getJMSMessageID() throws JMSException {
            return null;
        }

        @Override
        public void setJMSMessageID(String id) throws JMSException {

        }

        @Override
        public long getJMSTimestamp() throws JMSException {
            return 0;
        }

        @Override
        public void setJMSTimestamp(long timestamp) throws JMSException {

        }

        @Override
        public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
            return new byte[0];
        }

        @Override
        public void setJMSCorrelationIDAsBytes(byte[] correlationID) throws JMSException {

        }

        @Override
        public void setJMSCorrelationID(String correlationID) throws JMSException {

        }

        @Override
        public String getJMSCorrelationID() throws JMSException {
            return null;
        }

        @Override
        public Destination getJMSReplyTo() throws JMSException {
            return null;
        }

        @Override
        public void setJMSReplyTo(Destination replyTo) throws JMSException {

        }

        @Override
        public Destination getJMSDestination() throws JMSException {
            return null;
        }

        @Override
        public void setJMSDestination(Destination destination) throws JMSException {

        }

        @Override
        public int getJMSDeliveryMode() throws JMSException {
            return 0;
        }

        @Override
        public void setJMSDeliveryMode(int deliveryMode) throws JMSException {

        }

        @Override
        public boolean getJMSRedelivered() throws JMSException {
            return false;
        }

        @Override
        public void setJMSRedelivered(boolean redelivered) throws JMSException {

        }

        @Override
        public String getJMSType() throws JMSException {
            return null;
        }

        @Override
        public void setJMSType(String type) throws JMSException {

        }

        @Override
        public long getJMSExpiration() throws JMSException {
            return 0;
        }

        @Override
        public void setJMSExpiration(long expiration) throws JMSException {

        }

        @Override
        public int getJMSPriority() throws JMSException {
            return 0;
        }

        @Override
        public void setJMSPriority(int priority) throws JMSException {

        }

        @Override
        public void clearProperties() throws JMSException {

        }

        @Override
        public boolean propertyExists(String name) throws JMSException {
            return false;
        }

        @Override
        public boolean getBooleanProperty(String name) throws JMSException {
            return false;
        }

        @Override
        public byte getByteProperty(String name) throws JMSException {
            return 0;
        }

        @Override
        public short getShortProperty(String name) throws JMSException {
            return 0;
        }

        @Override
        public int getIntProperty(String name) throws JMSException {
            return 0;
        }

        @Override
        public long getLongProperty(String name) throws JMSException {
            return 0;
        }

        @Override
        public float getFloatProperty(String name) throws JMSException {
            return 0;
        }

        @Override
        public double getDoubleProperty(String name) throws JMSException {
            return 0;
        }

        @Override
        public String getStringProperty(String name) throws JMSException {
            return null;
        }

        @Override
        public Object getObjectProperty(String name) throws JMSException {
            return null;
        }

        @Override
        public Enumeration getPropertyNames() throws JMSException {
            return null;
        }

        @Override
        public void setBooleanProperty(String name, boolean value) throws JMSException {

        }

        @Override
        public void setByteProperty(String name, byte value) throws JMSException {

        }

        @Override
        public void setShortProperty(String name, short value) throws JMSException {

        }

        @Override
        public void setIntProperty(String name, int value) throws JMSException {

        }

        @Override
        public void setLongProperty(String name, long value) throws JMSException {

        }

        @Override
        public void setFloatProperty(String name, float value) throws JMSException {

        }

        @Override
        public void setDoubleProperty(String name, double value) throws JMSException {

        }

        @Override
        public void setStringProperty(String name, String value) throws JMSException {

        }

        @Override
        public void setObjectProperty(String name, Object value) throws JMSException {

        }

        @Override
        public void acknowledge() throws JMSException {

        }

        @Override
        public void clearBody() throws JMSException {

        }
    };
}
