package com.pamirs.pradar.degrade.resources;

import com.sun.management.OperatingSystemMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author angju
 * @date 2022/3/9 14:50
 */
public class ContainerCpuUsageResourceDetector implements ResourceDetector {

    private volatile double maxUsage;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final AtomicBoolean warnAlready = new AtomicBoolean(false);
    private final int cpuCores;
    private final double containerCores;
    private final int userHz;

    public ContainerCpuUsageResourceDetector() throws IOException {
        String value = System.getProperty(configName(), "0.7");
        this.maxUsage = Double.parseDouble(value);
        cpuCores = getTotalCpuCores();
        containerCores = getContainerCores();
        userHz = Integer.parseInt(execCommand("getconf CLK_TCK").get(0));
    }

    @Override
    public boolean hasResource() {
        try {
            double v = cpuInfo();
            boolean result = v < maxUsage;
            if (!result) {
                logger.info("{} detect result : {}", this.getClass(), v);
            }
            return result;
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public String name() {
        return "container cpu usage";
    }

    @Override
    public double threshold() {
        return maxUsage;
    }

    @Override
    public String configName() {
        return "degrade.container.cpu.usage.detect.ratio";
    }

    @Override
    public void refreshThreshold() {
        String value = System.getProperty(configName(), "0.7");
        this.maxUsage = Double.parseDouble(value);
    }


    public static List<String> readFile(String filename) throws IOException {
        return Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
    }

    public double cpuInfo() {
        try {
            List<String> prfContents = readFile("/proc/stat");
            String[] prfSplits = prfContents.get(0).split("\\s+");
            long prfSystemCpuValue = Long.parseLong(prfSplits[1]) + Long.parseLong(prfSplits[2]) + Long.parseLong(
                prfSplits[3]) + Long.parseLong(prfSplits[4]) + Long.parseLong(prfSplits[5]) + Long.parseLong(
                prfSplits[6]) + Long.parseLong(prfSplits[7]) + Long.parseLong(prfSplits[8]);
            BigDecimal prfContainerCpuValue = new BigDecimal(readFile("/sys/fs/cgroup/cpuacct/cpuacct.usage").get(0));

            Thread.sleep(1000);

            List<String> afterContents = readFile("/proc/stat");
            String[] afterSplits = afterContents.get(0).split("\\s+");
            long systemCpuValue = Long.parseLong(afterSplits[1]) + Long.parseLong(afterSplits[2]) + Long.parseLong(
                afterSplits[3]) + Long.parseLong(afterSplits[4]) + Long.parseLong(afterSplits[5]) + Long.parseLong(
                afterSplits[6]) + Long.parseLong(afterSplits[7]) + Long.parseLong(afterSplits[8]);
            BigDecimal containerCpuValue = new BigDecimal(readFile("/sys/fs/cgroup/cpuacct/cpuacct.usage").get(0));

            // 100% * 容器使用cpu时间 / 系统使用cpu时间
            //cpu使用率是整体的, 上限就是100，所以不乘核数
            BigDecimal containerCpuDelta = containerCpuValue.subtract(prfContainerCpuValue);
            BigDecimal systemCpuDelta = new BigDecimal(systemCpuValue - prfSystemCpuValue).multiply(
                new BigDecimal(1000 * 1000 * 1000 / userHz));

            double cpuUsagePercent = 0.0;
            if (containerCpuDelta.doubleValue() > 0 && systemCpuDelta.doubleValue() > 0) {
                cpuUsagePercent = containerCpuDelta.multiply(new BigDecimal(100)).divide(systemCpuDelta, 2,
                    BigDecimal.ROUND_HALF_UP).doubleValue();
                if (cpuUsagePercent < 2) {
                    OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
                    cpuUsagePercent = osBean.getSystemCpuLoad();
                }
            }
            cpuUsagePercent = cpuUsagePercent * cpuCores; //cpu占用率
            cpuUsagePercent = cpuUsagePercent / containerCores;
            cpuUsagePercent = cpuUsagePercent / 100.0;
            return cpuUsagePercent;
        } catch (Throwable e) {
            if (warnAlready.compareAndSet(false, true)) {
                logger.error("cpuInfo fail!", e);
            }
            return -1;
        }
    }

    private static double getContainerCores() throws IOException {
        int quota = Integer.parseInt(readFile("/sys/fs/cgroup/cpuacct/cpu.cfs_quota_us").get(0));
        int period = Integer.parseInt(readFile("/sys/fs/cgroup/cpuacct/cpu.cfs_period_us").get(0));
        return ((double)quota) / ((double)period);
    }

    private static int getTotalCpuCores() throws IOException {
        List<String> cores = readFile("/sys/fs/cgroup/cpuacct/cpuacct.usage_percpu");
        String[] tmp = cores.get(0).split("\\s");
        return tmp.length;
    }

    private static List<String> execCommand(String cmd) throws IOException {
        List<String> result = new ArrayList<String>();
        Process ps = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", cmd});
        BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
        String line;
        while ((line = br.readLine()) != null) {
            result.add(line);
        }
        return result;
    }

}