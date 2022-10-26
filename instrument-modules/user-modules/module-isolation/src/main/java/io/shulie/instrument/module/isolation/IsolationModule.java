package io.shulie.instrument.module.isolation;

import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Licey
 * @date 2022/7/26
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "isolation", version = "1.0.0", author = "likan@shulie.io",
        description = "提供隔离路由的基础能力")
public class IsolationModule extends ModuleLifecycleAdapter implements ExtensionModule {

    private static final Logger logger = LoggerFactory.getLogger(IsolationModule.class);

    @Override
    public boolean onActive() throws Throwable {
        IsolationManager.init(moduleEventWatcher);
        return true;
    }

    @Override
    public void onUnload() throws Throwable {
        logger.info("[isolation] start unload");
        IsolationManager.release();
        logger.info("[isolation] end unload");
    }
}
