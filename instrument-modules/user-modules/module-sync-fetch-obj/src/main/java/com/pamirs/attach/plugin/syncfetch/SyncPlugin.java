package com.pamirs.attach.plugin.syncfetch;

import com.pamirs.attach.plugin.syncfetch.interceptor.SyncObjectAfterFetchInterceptor;
import com.pamirs.attach.plugin.syncfetch.interceptor.SyncObjectBeforeFetchInterceptor;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.listener.Listeners;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Licey
 * @date 2022/5/16
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "sync-fetch-obj", version = "1.0.0", author = "langyi", description = "sf-sync")
public class SyncPlugin extends ModuleLifecycleAdapter implements ExtensionModule {
    private static final Logger logger = LoggerFactory.getLogger(SyncPlugin.class);

    @Override
    public boolean onActive() throws Throwable {
        String values = System.getProperty("simulator.inner.module.syncfetch");
        logger.info("start to enhance target with : {}", values);
        if (values == null) {
            return true;
        }
        for (String s : values.split(",")) {
            String data = s.trim();
            if (data == null || data.isEmpty()) {
                continue;
            }
            if (!data.contains("#")) {
                enhanceTemplate.enhance(this, data, new EnhanceCallback() {
                    @Override
                    public void doEnhance(InstrumentClass target) {
                        target.getConstructors().addInterceptor(Listeners.of(SyncObjectAfterFetchInterceptor.class));
                    }
                });
                continue;
            }

            final String[] split = data.split("#");
            if (split.length == 2) {
                enhanceTemplate.enhance(this, split[0], new EnhanceCallback() {
                    @Override
                    public void doEnhance(InstrumentClass target) {
                        target.getDeclaredMethods(split[1]).addInterceptor(Listeners.of(SyncObjectAfterFetchInterceptor.class));
                    }
                });
                continue;
            }
            // 支持class#method#before, 这样在before里保存对象, 可以支持方法内部死循环情景，比如方法内部kafka循环消费
            if (split.length == 3) {
                String orderFlag = split[2];
                final Class clazz = "before".equals(orderFlag) ? SyncObjectBeforeFetchInterceptor.class : SyncObjectAfterFetchInterceptor.class;
                enhanceTemplate.enhance(this, split[0], new EnhanceCallback() {
                    @Override
                    public void doEnhance(InstrumentClass target) {
                        target.getDeclaredMethods(split[1]).addInterceptor(Listeners.of(clazz));
                    }
                });
                continue;
            }
            logger.error("can not sync fetch target:{}", data);

        }
        return true;
    }
}
