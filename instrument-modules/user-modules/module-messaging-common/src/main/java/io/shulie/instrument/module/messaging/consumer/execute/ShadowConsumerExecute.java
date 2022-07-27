package io.shulie.instrument.module.messaging.consumer.execute;

import com.pamirs.pradar.bean.SyncObjectData;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;

/**
 * @author Licey
 * @date 2022/7/27
 */
public interface ShadowConsumerExecute {

    ConsumerConfig prepareConfig(SyncObjectData syncObjectData);

    ShadowServer fetchShadowServer(ConsumerConfig config, String shadowConfig);
}
