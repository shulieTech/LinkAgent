package com.pamirs.attach.plugin.activemqv2.consumer.config;

import com.pamirs.attach.plugin.activemqv2.consumer.server.ActiveMQShadowServer;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;
import org.apache.activemq.ActiveMQMessageConsumer;
import org.apache.activemq.ActiveMQSession;
import org.apache.activemq.command.ActiveMQDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author guann1n9
 * @date 2023/12/22 4:33 PM
 */
public class ActiveMQConsumerConfig  extends ConsumerConfig {

    private String key;

    private String server;

    private Object[] constructArgs;

    private ActiveMQMessageConsumer bizConsumer;

    private static final Logger logger = LoggerFactory.getLogger(ActiveMQShadowServer.class);


    public ActiveMQConsumerConfig(Object[] args, ActiveMQMessageConsumer bizConsumer) {
        this.constructArgs = args;
        this.bizConsumer = bizConsumer;
        for (Object arg : args) {
            if(arg instanceof ActiveMQDestination){
                String destination = ((ActiveMQDestination) arg).getQualifiedName();
                String selector;
                try {
                    selector = args[4] == null ? "empty" : args[4].toString().split("'")[1];
                } catch (Throwable e) {
                    logger.error("Apache-ActiveMQ: init shadow ActiveMQMessageConsumer err! get configKey error",e);
                    throw new RuntimeException(e);
                }
                this.key = destination + "#" + selector;
            }
            if(arg instanceof ActiveMQSession){
                this.server = ((ActiveMQSession) arg).getConnection().getBrokerInfo().getBrokerURL();
            }
        }
        if(key == null || server == null){
            throw new RuntimeException("Apache-ActiveMQ: init shadow ActiveMQMessageConsumer err! key  or server is null");
        }
    }


    /**
     * topic#selector  若无selector 则为empty
     * @return
     */
    @Override
    public String keyOfConfig() {
        return key;
    }

    @Override
    public String keyOfServer() {
        return server;
    }

    public void setConstructArgs(Object[] constructArgs) {
        this.constructArgs = constructArgs;
    }


    public Object[] getConstructArgs() {
        return constructArgs;
    }

    public void setBizConsumer(ActiveMQMessageConsumer bizConsumer) {
        this.bizConsumer = bizConsumer;
    }

    public ActiveMQMessageConsumer getBizConsumer() {
        return bizConsumer;
    }

}
