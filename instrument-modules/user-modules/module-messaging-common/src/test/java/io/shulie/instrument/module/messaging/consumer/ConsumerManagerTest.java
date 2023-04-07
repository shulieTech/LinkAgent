package io.shulie.instrument.module.messaging.consumer;

import com.pamirs.pradar.SyncObjectService;
import com.pamirs.pradar.bean.SyncObject;
import com.pamirs.pradar.bean.SyncObjectData;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.filter.Filter;
import com.shulie.instrument.simulator.api.listener.ext.EventWatchCondition;
import com.shulie.instrument.simulator.api.listener.ext.Progress;
import com.shulie.instrument.simulator.api.listener.ext.WatchCallback;
import com.shulie.instrument.simulator.api.resource.DumpResult;
import com.shulie.instrument.simulator.api.resource.ModuleEventWatcher;
import io.shulie.instrument.module.isolation.IsolationManager;
import io.shulie.instrument.module.isolation.proxy.ShadowMethodProxyUtils;
import io.shulie.instrument.module.messaging.consumer.execute.TestShadowConsumerExecute;
import io.shulie.instrument.module.messaging.consumer.isolation.ConsumerIsolationCache;
import io.shulie.instrument.module.messaging.consumer.isolation.ConsumerIsolationLifecycle;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerIsolationRegister;
import io.shulie.instrument.module.messaging.consumer.module.ConsumerRegister;
import io.shulie.instrument.module.messaging.consumer.module.ShadowConsumer;
import io.shulie.instrument.module.messaging.consumer.module.isolation.ConsumerClass;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.HashSet;
import java.util.List;

/**
 * @author Licey
 * @date 2022/7/27
 */
public class ConsumerManagerTest extends TestCase {

    private Object bizTarget;

    public void setUp() throws Exception {
        SyncObject syncObject = new SyncObject();
        Object target = new Object();
        syncObject.addData(new SyncObjectData(target, null,null,null, null));
        syncObject.addData(new SyncObjectData(target, null,null,null, null));
        SyncObjectService.saveSyncObject("class#method", syncObject);

        SyncObject syncObject2 = new SyncObject();
        syncObject2.addData(new SyncObjectData(new Object(), null,null,null, null));
        SyncObjectService.saveSyncObject("class#method2", syncObject2);

        HashSet<String> mqWhiteList = new HashSet<String>();
        mqWhiteList.add("aaa");
        mqWhiteList.add("bbb");
        GlobalConfig.getInstance().setMqWhiteList(mqWhiteList);

        IsolationManager.init(new ModuleEventWatcher() {
            @Override
            public int watch(Filter filter, Progress progress) {
                //todo@langyi
                return 0;
            }

            @Override
            public DumpResult dump(Filter filter, Progress progress) {
                return null;
            }

            @Override
            public int watch(Filter filter) {
                return 0;
            }

            @Override
            public int watch(EventWatchCondition condition, Progress progress) {
                return 0;
            }

            @Override
            public void delete(int watcherId, Progress progress) {

            }

            @Override
            public void delete(int watcherId) {

            }

            @Override
            public void watching(Filter filter, Progress wProgress, WatchCallback watchCb, Progress dProgress) throws Throwable {

            }

            @Override
            public void watching(Filter filter, WatchCallback watchCb) throws Throwable {

            }

            @Override
            public void close() {

            }
        });

        bizTarget = syncObject.getDatas().iterator().next().getTarget();

    }

    public void testRegister() throws InterruptedException {
        ConsumerRegister register = ConsumerRegister.init("test-module").consumerExecute(TestShadowConsumerExecute::new);

        ConsumerManager.register(register, "class#method","class#method2");
        ConsumerManager.runTask();
        List<ShadowConsumer> list = ConsumerManager.runningShadowConsumer();
        Assert.assertFalse(list.isEmpty());
        Assert.assertEquals(list.size(), 2);
    }

    public void testIsolationRegister() throws InterruptedException {
        ConsumerRegister register = ConsumerRegister.init("test-module-iso").consumerExecute(TestShadowConsumerExecute::new);
        ConsumerIsolationRegister isolationRegister=new ConsumerIsolationRegister().addConsumerClass(new ConsumerClass("class").addEnhanceMethod("abc", ShadowMethodProxyUtils.defaultRoute()));

        ConsumerManager.register(register, isolationRegister, "class#method", "class#method2");
        ConsumerManager.runTask();
        List<ShadowConsumer> list = ConsumerManager.runningShadowConsumer();
        Assert.assertFalse(list.isEmpty());
        Assert.assertEquals(list.size(), 2);

        ConsumerIsolationLifecycle lifecycle = ConsumerIsolationCache.get(bizTarget);
        Assert.assertNotNull(lifecycle);
        Assert.assertNotNull(lifecycle.getShadowServer());
    }


}