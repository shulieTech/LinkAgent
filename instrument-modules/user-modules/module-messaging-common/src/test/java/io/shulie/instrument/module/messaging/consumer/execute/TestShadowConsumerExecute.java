package io.shulie.instrument.module.messaging.consumer.execute;

import com.pamirs.pradar.bean.SyncObjectData;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerConfig;

import java.util.Arrays;
import java.util.List;

/**
 * @author Licey
 * @date 2022/8/2
 */
public class TestShadowConsumerExecute implements ShadowConsumerExecute {
    @Override
    public List<ConsumerConfig> prepareConfig(SyncObjectData syncObjectData) {
        ConsumerConfig c1 = new ConsumerConfig() {

            @Override
            public String keyOfConfig() {
                return "aaa";
            }

            @Override
            public String keyOfServer() {
                return null;
            }
        };
        ConsumerConfig c2 = new ConsumerConfig() {

            @Override
            public String keyOfConfig() {
                return "bbb";
            }

            @Override
            public String keyOfServer() {
                return null;
            }
        };
        return Arrays.asList(c1, c2);
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
}
