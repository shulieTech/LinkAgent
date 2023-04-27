package com.pamirs.pradar.degrade.resources;

import com.sun.management.OperatingSystemMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.TimeUnit;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/08/31 4:04 PM
 */
public class CpuUsageResourceDetector implements ResourceDetector {

    private volatile double maxUsage;

    volatile long processCpuTime = 0;
    volatile long processUpTime = 0;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public CpuUsageResourceDetector() {
        String value = System.getProperty(configName(), "0.7");
        this.maxUsage = Double.parseDouble(value);
    }

    @Override
    public boolean hasResource() {
        double cpuUsage = getCurrentCpuUsage();
        if(Double.isNaN(cpuUsage)){
            return true;
        }
        boolean result = cpuUsage < maxUsage;
        if (!result) {
            logger.info("{} detect result : {}", this.getClass(), cpuUsage);
        }
        return result;
    }

    @Override
    public String name() {
        return "cpu usage";
    }

    @Override
    public double threshold() {
        return maxUsage;
    }

    @Override
    public String configName() {
        return "degrade.cpu.usage.detect.ratio";
    }

    @Override
    public void refreshThreshold() {
        String value = System.getProperty(configName(), "0.7");
        this.maxUsage = Double.parseDouble(value);
    }

    private double getCurrentCpuUsage() {
        /*
         * Java Doc copied from {@link OperatingSystemMXBean#getSystemCpuLoad()}:</br>
         * Returns the "recent cpu usage" for the whole system. This value is a double in the [0.0,1.0] interval.
         * A value of 0.0 means that all CPUs were idle during the recent period of time observed, while a value
         * of 1.0 means that all CPUs were actively running 100% of the time during the recent period being
         * observed. All values between 0.0 and 1.0 are possible depending of the activities going on in the
         * system. If the system recent cpu usage is not available, the method returns a negative value.
         */
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        double systemCpuUsage = osBean.getSystemCpuLoad();

        // calculate process cpu usage to support application running in container environment
        RuntimeMXBean runtimeBean = ManagementFactory.getPlatformMXBean(RuntimeMXBean.class);
        long newProcessCpuTime = osBean.getProcessCpuTime();
        long newProcessUpTime = runtimeBean.getUptime();
        int cpuCores = osBean.getAvailableProcessors();
        long processCpuTimeDiffInMs = TimeUnit.NANOSECONDS.toMillis(newProcessCpuTime - processCpuTime);
        long processUpTimeDiffInMs = newProcessUpTime - processUpTime;
        double processCpuUsage = (double)processCpuTimeDiffInMs / processUpTimeDiffInMs / cpuCores;
        processCpuTime = newProcessCpuTime;
        processUpTime = newProcessUpTime;

        return Math.max(processCpuUsage, systemCpuUsage);
    }

}
