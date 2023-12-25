package com.pamirs.attach.plugin.activemqv2.consumer.server;

import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;
import org.apache.activemq.ActiveMQMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author guann1n9
 * @date 2023/12/22 4:32 PM
 */
public class ActiveMQShadowServer implements ShadowServer {


    private static final Logger logger = LoggerFactory.getLogger(ActiveMQShadowServer.class);

    private final ActiveMQMessageConsumer shadowConsumer;

    private final AtomicBoolean started = new AtomicBoolean(false);

    public ActiveMQShadowServer(ActiveMQMessageConsumer shadowConsumer) {
        this.shadowConsumer = shadowConsumer;
    }

    @Override
    public Object getShadowTarget() {
        return this.shadowConsumer;
    }

    @Override
    public void start() {
        try {
            shadowConsumer.start();
            started.set(true);
        } catch (Throwable e) {
            ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.MQ)
                    .setErrorCode("MQ-0001")
                    .setMessage("Apache-ActiveMQ消费端启动失败！")
                    .setDetail("consumer name:" + shadowConsumer.getConsumerName() + "||"
                            + e.getMessage())
                    .report();
            logger.error("Apache-ActiveMQ: start shadow ActiveMQMessageConsumer err! consumer name:{}",
                    shadowConsumer.getConsumerName(), e);
        }
    }

    @Override
    public boolean isRunning() {
        return started.get();
    }

    @Override
    public void stop() {
        try {
            shadowConsumer.close();
            started.set(false);
        } catch (Throwable e) {
            ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.MQ)
                    .setErrorCode("MQ-9999")
                    .setMessage("Apache-ActiveMQ消费端关闭失败！")
                    .setDetail("consumer name:" + shadowConsumer.getConsumerName() + "||"
                            + e.getMessage())
                    .report();
            logger.error("Apache-ActiveMQ: shutdown shadow ActiveMQMessageConsumer err! consumer name:{}",
                    shadowConsumer.getConsumerName(), e);
        }
    }

}
