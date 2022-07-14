package com.pamirs.attach.plugin.apache.kafka.interceptor;

import com.pamirs.attach.plugin.apache.kafka.destroy.KafkaDestroy;
import com.pamirs.attach.plugin.apache.kafka.origin.ConsumerHolder;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.kafka.clients.consumer.Consumer;

@Destroyable(KafkaDestroy.class)
public class CreateRawConsumerInterceptor extends AroundInterceptor {

    @Override
    public void doAfter(Advice advice) throws Throwable {
        Object returnObj = advice.getReturnObj();
        ConsumerHolder.addWorkWithSpring((Consumer<?, ?>)returnObj);
    }
}
