package com.pamirs.attach.plugin.activemqv2.consumer;

import com.pamirs.attach.plugin.activemqv2.consumer.config.ActiveMQConsumerConfig;
import com.pamirs.attach.plugin.activemqv2.consumer.server.ActiveMQShadowServer;
import com.pamirs.attach.plugin.activemqv2.util.ActiveMQDestinationUtil;
import com.pamirs.pradar.bean.SyncObjectData;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowConsumerExecute;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfigWithData;
import org.apache.activemq.ActiveMQMessageConsumer;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.ConsumerId;

import java.util.ArrayList;
import java.util.List;

/**
 * @author guann1n9
 * @date 2023/12/22 4:28 PM
 */
public class ActiveMQShadowConsumerExecute implements ShadowConsumerExecute {


    @Override
    public List<ConsumerConfig> prepareConfig(SyncObjectData syncObjectData) {
        Object[] args = syncObjectData.getArgs();
        ActiveMQMessageConsumer bizConsumer = (ActiveMQMessageConsumer) syncObjectData.getReturnObj();
        ActiveMQConsumerConfig consumerConfig = new ActiveMQConsumerConfig(args,bizConsumer);
        List<ConsumerConfig> configs = new ArrayList<ConsumerConfig>();
        configs.add(consumerConfig);
        return configs;
    }


    @Override
    public ShadowServer fetchShadowServer(List<ConsumerConfigWithData> configList) {

        ConsumerConfigWithData data = configList.get(0);
        ActiveMQConsumerConfig consumerConfig = (ActiveMQConsumerConfig) data.getConsumerConfig();
        Object[] constructArgs = consumerConfig.getConstructArgs();
        //替换入参为影子配置
        for (Object arg : constructArgs) {
            if(arg instanceof ConsumerId){
                ConsumerId origin = (ConsumerId) arg;
                ConsumerId shadow = new ConsumerId(origin);
                shadow.setValue(10000+origin.getValue());
                arg = shadow;
            }
            if(arg instanceof ActiveMQDestination){
                ActiveMQDestination origin = (ActiveMQDestination) arg;
                arg = ActiveMQDestinationUtil.mappingShadowDestination(origin);
            }
        }
        ActiveMQMessageConsumer shadowConsumer = Reflect.on(ActiveMQMessageConsumer.class).create(constructArgs).get();
        return new ActiveMQShadowServer(shadowConsumer);


    }
}
