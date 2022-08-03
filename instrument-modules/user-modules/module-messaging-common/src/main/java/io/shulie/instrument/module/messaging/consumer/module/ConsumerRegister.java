package io.shulie.instrument.module.messaging.consumer.module;

import io.shulie.instrument.module.messaging.annoation.NotNull;
import io.shulie.instrument.module.messaging.common.ResourceInit;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowConsumerExecute;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;

/**
 * @author Licey
 * @date 2022/7/27
 */
public class ConsumerRegister {
    @NotNull
    private ResourceInit<ShadowConsumerExecute> consumerExecuteResourceInit;

    public static ConsumerRegister init(){
        return new ConsumerRegister();
    }

    public ConsumerRegister consumerExecute(ResourceInit<ShadowConsumerExecute> consumerExecuteResourceInit){
        this.consumerExecuteResourceInit = consumerExecuteResourceInit;
        return this;
    }

    public ResourceInit<ShadowConsumerExecute> getConsumerExecuteResourceInit() {
        return consumerExecuteResourceInit;
    }
}
