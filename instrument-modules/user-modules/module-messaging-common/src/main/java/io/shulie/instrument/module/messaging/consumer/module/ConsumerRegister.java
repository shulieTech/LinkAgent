package io.shulie.instrument.module.messaging.consumer.module;

import io.shulie.instrument.module.messaging.annoation.NotNull;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowConsumerExecute;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;

/**
 * @author Licey
 * @date 2022/7/27
 */
public class ConsumerRegister {
    @NotNull
    private ShadowConsumerExecute consumerExecute;

    public static ConsumerRegister init(){
        return new ConsumerRegister();
    }

    public ConsumerRegister consumerExecute(ShadowConsumerExecute consumerExecute){
        this.consumerExecute = consumerExecute;
        return this;
    }

    public ShadowConsumerExecute getConsumerExecute() {
        return consumerExecute;
    }

}
