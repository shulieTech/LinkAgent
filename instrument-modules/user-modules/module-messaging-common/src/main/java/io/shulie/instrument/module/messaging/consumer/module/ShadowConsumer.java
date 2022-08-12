package io.shulie.instrument.module.messaging.consumer.module;

import io.shulie.instrument.module.messaging.consumer.execute.ShadowConsumerExecute;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Licey
 * @date 2022/7/27
 */
public class ShadowConsumer {
    private Set<ConsumerConfig> configSet;
    private ShadowConsumerExecute consumerExecute;
    private ShadowServer shadowServer;
    private Object bizTarget;
    private Set<ConsumerConfig> enableConfigSet;

    public ShadowConsumer(ShadowConsumerExecute consumerExecute, Object bizTarget) {
        this.configSet = new HashSet<>();
        this.enableConfigSet = new HashSet<>();
        this.consumerExecute = consumerExecute;
        this.bizTarget = bizTarget;
    }

    public Set<ConsumerConfig> getEnableConfigSet() {
        return enableConfigSet;
    }

    public void setEnableConfigSet(Set<ConsumerConfig> enableConfigSet) {
        this.enableConfigSet = enableConfigSet;
    }

    public ShadowConsumerExecute getConsumerExecute() {
        return consumerExecute;
    }

    public void setConsumerExecute(ShadowConsumerExecute consumerExecute) {
        this.consumerExecute = consumerExecute;
    }

    public Set<ConsumerConfig> getConfigSet() {
        return configSet;
    }

    public void setConfigSet(Set<ConsumerConfig> configSet) {
        this.configSet = configSet;
    }

    public Object getBizTarget() {
        return bizTarget;
    }

    public void setBizTarget(Object bizTarget) {
        this.bizTarget = bizTarget;
    }

    public ShadowServer getShadowServer() {
        return shadowServer;
    }

    public void setShadowServer(ShadowServer shadowServer) {
        this.shadowServer = shadowServer;
    }

    public boolean isStarted() {
        return shadowServer == null || shadowServer.isRunning();
    }

}
