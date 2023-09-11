package com.pamirs.pradar.degrade.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.List;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/09/09 11:43 AM
 */
public class MemoryResourceDetector implements ResourceDetector {

    private final MemoryPoolMXBean oldMemoryPoolMXBean;

    private volatile double threshold;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public MemoryResourceDetector() {
        String value = System.getProperty(configName(), "0.6");
        this.threshold = Double.parseDouble(value);
        MemoryPoolMXBean memoryPoolMXBeanTmp = null;
        List<MemoryPoolMXBean> memoryPoolMXBeanList = java.lang.management.ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeanList) {
            if (MemoryType.HEAP == memoryPoolMXBean.getType() && memoryPoolMXBean.getName().contains("Old Gen")) {
                memoryPoolMXBeanTmp = memoryPoolMXBean;
                break;
            }
        }
        this.oldMemoryPoolMXBean = memoryPoolMXBeanTmp;
    }

    @Override
    public boolean hasResource() {
        if (oldMemoryPoolMXBean == null) {
            return true;
        }
        MemoryUsage memoryUsage = oldMemoryPoolMXBean.getUsage();
        long maxSize = memoryUsage.getMax();
        long usedSize = memoryUsage.getUsed();
        double usage = (double)usedSize / (double)maxSize;
        boolean result = usage < threshold();
        if (!result) {
            logger.info("{} detect result : {}", this.getClass(), usage);
        }
        return result;
    }

    @Override
    public String name() {
        return "old gen";
    }

    @Override
    public double threshold() {
        return threshold;
    }

    @Override
    public String configName() {
        return "degrade.oldgen.detect.ratio";
    }

    @Override
    public void refreshThreshold() {
        String value = System.getProperty(configName(), "0.6");
        this.threshold = Double.parseDouble(value);
    }

}
