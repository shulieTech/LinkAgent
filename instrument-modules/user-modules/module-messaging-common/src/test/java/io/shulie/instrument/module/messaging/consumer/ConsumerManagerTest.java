package io.shulie.instrument.module.messaging.consumer;

import com.pamirs.pradar.SyncObjectService;
import com.pamirs.pradar.bean.SyncObject;
import com.pamirs.pradar.bean.SyncObjectData;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowConsumerExecute;
import io.shulie.instrument.module.messaging.consumer.execute.ShadowServer;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerRegister;
import io.shulie.instrument.module.messaging.consumer.module.ShadowConsumer;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Licey
 * @date 2022/7/27
 */
public class ConsumerManagerTest extends TestCase {

    public void setUp() throws Exception {
        SyncObject syncObject = new SyncObject();
        syncObject.addData(new SyncObjectData(new Object(), null, null));
        SyncObjectService.saveSyncObject("class#method", syncObject);

        HashSet<String> mqWhiteList = new HashSet<String>();
        mqWhiteList.add("aaa");
        GlobalConfig.getInstance().setMqWhiteList(mqWhiteList);

    }

    public void testRegister() throws InterruptedException {
        ConsumerRegister register = ConsumerRegister.init().consumerExecute(new ShadowConsumerExecute() {
            @Override
            public ConsumerConfig prepareConfig(SyncObjectData syncObjectData) {
                return new ConsumerConfig(){

                    @Override
                    public String keyOfConfig() {
                        return "aaa";
                    }
                };
            }

            @Override
            public ShadowServer fetchShadowServer(ConsumerConfig config, String shadowConfig) {
                return new ShadowServer() {
                    @Override
                    public void start() {
                        System.out.println("start");
                    }

                    @Override
                    public boolean isRunning() {
                        return true;
                    }

                    @Override
                    public void stop() {
                        System.out.println("stop");
                    }
                };
            }
        });

        ConsumerManager.register(register,"class#method");
        Thread.sleep(10 * 1000);
        List<ShadowConsumer> list = ConsumerManager.runningShadowConsumer();
        Assert.assertFalse(list.isEmpty());
    }

    public void testRegisterNotNull() {
        ConsumerRegister register = new ConsumerRegister();
        ConsumerManager.register(register);
    }
}