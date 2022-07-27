package io.shulie.instrument.module.messaging.consumer.module;

import com.pamirs.pradar.bean.SyncObject;
import com.pamirs.pradar.bean.SyncObjectData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Licey
 * @date 2022/7/27
 */
public class ConsumerModule {
    private final Map<String, SyncObject> syncObjectMap = new ConcurrentHashMap<String, SyncObject>();
    private final Map<SyncObjectData, ShadowConsumer> shadowConsumerMap = new ConcurrentHashMap<SyncObjectData, ShadowConsumer>();
    private ConsumerRegister consumerRegister;

    public Map<String, SyncObject> getSyncObjectMap() {
        return syncObjectMap;
    }

    public Map<SyncObjectData, ShadowConsumer> getShadowConsumerMap() {
        return shadowConsumerMap;
    }

    public ConsumerModule(ConsumerRegister consumerRegister) {
        this.consumerRegister = consumerRegister;
    }

    public ConsumerRegister getConsumerRegister() {
        return consumerRegister;
    }

    public void setConsumerRegister(ConsumerRegister consumerRegister) {
        this.consumerRegister = consumerRegister;
    }
}
