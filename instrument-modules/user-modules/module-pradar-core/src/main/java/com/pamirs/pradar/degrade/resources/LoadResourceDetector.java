package com.pamirs.pradar.degrade.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/08/31 4:04 PM
 */
public class LoadResourceDetector implements ResourceDetector {

    public SystemInfo si = new SystemInfo();

    private volatile double maxLoad;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final int coreNum = Runtime.getRuntime().availableProcessors();

    public LoadResourceDetector() {
        String value = System.getProperty(configName(), "1.5");
        this.maxLoad = coreNum * Double.parseDouble(value);
    }

    @Override
    public boolean hasResource() {
        double[] loadAverage = si.getHardware().getProcessor().getSystemLoadAverage(1);
        boolean result = loadAverage[0] < maxLoad;
        if (!result) {
            logger.info("{} detect result : {}", this.getClass(), loadAverage[0]);
        }
        return result;
    }

    @Override
    public String name() {
        return "load";
    }

    @Override
    public double threshold() {
        return maxLoad;
    }

    @Override
    public String configName() {
        return "degrade.load.detect.ratio";
    }

    @Override
    public void refreshThreshold() {
        String value = System.getProperty(configName(), "1.5");
        this.maxLoad = coreNum * Double.parseDouble(value);
    }
}
