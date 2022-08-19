package io.shulie.instrument.module.messaging.consumer.module;

import io.shulie.instrument.module.messaging.consumer.module.isolation.ConsumerClass;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Licey
 * @date 2022/8/8
 */
public class ConsumerIsolationRegister {
    private List<ConsumerClass> consumerClassList;

    public ConsumerIsolationRegister addConsumerClass(ConsumerClass consumerClass) {
        if (consumerClassList == null) {
            consumerClassList = new ArrayList<>();
        }
        consumerClassList.add(consumerClass);
        return this;
    }

    public List<ConsumerClass> getConsumerClassList() {
        return consumerClassList;
    }

    public void setConsumerClassList(List<ConsumerClass> consumerClassList) {
        this.consumerClassList = consumerClassList;
    }
}
