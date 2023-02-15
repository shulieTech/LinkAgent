package com.pamirs.attach.plugin.shadow.preparation;

import com.pamirs.attach.plugin.shadow.preparation.checker.ShadowDataSourceConfigChecker;
import com.pamirs.pradar.pressurement.base.util.PropertyUtil;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.executors.ExecutorServiceFactory;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "shadow-preparation", version = "1.0.0", author = "jiangjibo@shulie.io", description = "影子资源准备工作，包括创建，校验，生效")
public class ShadowPreparationModule extends ModuleLifecycleAdapter implements ExtensionModule {

    private final static Logger LOGGER = LoggerFactory.getLogger(ShadowPreparationModule.class);

    private ThreadPoolExecutor pool;

    @Override
    public boolean onActive() throws Throwable {
        if (!PropertyUtil.isShadowPreparationEnabled()) {
            return true;
        }

        pool = new ThreadPoolExecutor(1, 4, 120L, TimeUnit.SECONDS, new ArrayBlockingQueue(10),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName("[shadow-preparation]");
                        return thread;
                    }
                });

        startDatasourceScheduling();

        return true;
    }

    /**
     * 开始影子数据源校验定时任务
     */
    private void startDatasourceScheduling() {
        ShadowDataSourceConfigChecker.simulatorConfig = simulatorConfig;

        String intervalString = "90s";
        int interval = 90, delay = 60;
        TimeUnit timeUnit = TimeUnit.SECONDS;

        try {
            String property = simulatorConfig.getProperty("shadow.datasource.check.interval");
            if (property != null) {
                interval = Integer.parseInt(property.substring(0, property.length() - 1));
                String unit = property.substring(property.length() - 1).toLowerCase();
                if (unit.equals("m")) {
                    timeUnit = TimeUnit.MINUTES;
                    delay = 1;
                }
                intervalString = property;
            }
        } catch (Exception e) {
            LOGGER.error("[shadow-preparation] property 'shadow.datasource.check.interval' is not formatted! Use default schedule interval 90s");
        }
        ShadowDataSourceConfigChecker.scheduleInterval = intervalString;

        ExecutorServiceFactory.getFactory().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                scheduleCheckShadowDatabaseAvailable();
            }
        }, delay, interval, timeUnit);

    }

    private void scheduleCheckShadowDatabaseAvailable() {
        Future future = pool.submit(new Runnable() {
            @Override
            public void run() {
                ShadowDataSourceConfigChecker.checkShadowDataSourceAvailable();
            }
        });
        try {
            future.get(70, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.error("[shadow-preparation] check shadow datasource available occur exception, maybe called after 70s don`t has result returned!", e);
            future.cancel(true);
        }
    }


}
