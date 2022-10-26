package com.pamirs.attach.plugin.syncfetch;

import com.pamirs.attach.plugin.syncfetch.interceptor.SyncObjectFetchInterceptor;
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
        if (values != null) {
            for (String s : values.split(",")) {
                String data = s.trim();
                if (!data.isEmpty()) {
                    if (data.contains("#")) {
                        final String[] split = data.split("#");
                        if (split.length == 2) {
                            enhanceTemplate.enhance(this, split[0], new EnhanceCallback() {
                                @Override
                                public void doEnhance(InstrumentClass target) {
                                    target.getDeclaredMethods(split[1]).addInterceptor(Listeners.of(SyncObjectFetchInterceptor.class));
                                }
                            });
                        } else {
                            logger.error("can not sync fetch target:{}", data);
                        }
                    } else {
                      enhanceTemplate.enhance(this, data, new EnhanceCallback() {
                          @Override
                          public void doEnhance(InstrumentClass target) {
                              target.getConstructors().addInterceptor(Listeners.of(SyncObjectFetchInterceptor.class));
                          }
                      });
                    }
                }
            }
        }
        return true;
    }
}
