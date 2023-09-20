package com.pamirs.pradar.degrade;

import com.pamirs.pradar.degrade.action.DegradeAction;
import com.pamirs.pradar.degrade.resources.ResourceDetector;
import com.pamirs.pradar.pressurement.agent.shared.service.SimulatorDynamicConfig;
import com.shulie.instrument.simulator.api.resource.SimulatorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/10/18 11:02 AM
 */
public class CombineResourceLimitDegradeDetect implements DegradeDetector {

    private final ScheduledExecutorService scheduledThreadPoolExecutor = Executors.newScheduledThreadPool(2);

    private int successCount = 0;

    private int failCount = 0;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final int period;

    private final int consecutiveConcurrences;

    private final List<ResourceDetector> resourceDetects = new ArrayList<ResourceDetector>();

    public CombineResourceLimitDegradeDetect(int duration, int period) {
        this.period = period;
        this.consecutiveConcurrences = duration / period;
    }

    @Override
    public void startDetect(final DegradeAction degradeAction) {
        scheduledThreadPoolExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (!Boolean.parseBoolean(System.getProperty("degrade.detect.enable", "true"))) {
                    SimulatorDynamicConfig.resetDegradeStatus();
                    return;
                }
                boolean hasResource = true;
                ResourceDetector resource = null;
                for (ResourceDetector resourceDetect : resourceDetects) {
                    if (resourceDetect.threshold() < 0) {
                        continue;
                    }
                    if (!resourceDetect.hasResource()) {
                        hasResource = false;
                        resource = resourceDetect;
                        break;
                    }
                }
                if (hasResource) {
                    successCount++;
                    failCount = 0;
                    if (!degradeAction.isDegraded()) {
                        return;
                    }
                    if (successCount >= consecutiveConcurrences) {
                        logger.warn("{} trigger unDegrade trigger using {}",
                                CombineResourceLimitDegradeDetect.this.getClass(),
                                degradeAction.getClass().getName());
                        degradeAction.unDegrade();
                        successCount = 0;
                    }
                } else {
                    failCount++;
                    successCount = 0;
                    if (!degradeAction.canDegrade()) {
                        return;
                    }
                    if (failCount >= consecutiveConcurrences) {
                        logger.warn("trigger degrade trigger using : {} because resource : {} exhausts max limit : {}",
                                degradeAction.getClass().getName(), resource.name(), resource.threshold());
                        degradeAction.degrade(
                                String.format("resouce %s over limit %s", resource.name(), resource.threshold()));
                        failCount = 0;
                    }
                }
            }
        }, 60, period, TimeUnit.SECONDS);

        scheduledThreadPoolExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                for (ResourceDetector detector : resourceDetects) {
                    if (detector.threshold() > 0 && detector.hasResource()) {
                        detector.refreshThreshold();
                    }
                }
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    @Override
    public void stopDetect() {
        scheduledThreadPoolExecutor.shutdown();
    }

    public void addResourceDetect(ResourceDetector resourceDetect) {
        resourceDetects.add(resourceDetect);
    }

    public List<ResourceDetector> getResourceDetects() {
        return resourceDetects;
    }
}
