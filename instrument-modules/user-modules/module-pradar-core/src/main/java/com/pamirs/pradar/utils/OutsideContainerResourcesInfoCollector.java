package com.pamirs.pradar.utils;

import com.pamirs.pradar.AppNameUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarCoreUtils;
import com.pamirs.pradar.PradarSwitcher;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.Util;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jiangjibo
 * @date 2022/2/11 1:48 下午
 * @description:
 */
public class OutsideContainerResourcesInfoCollector implements Runnable {

    public SystemInfo si = new SystemInfo();

    public static final String CPU_USAGE_KEY = "cpuUsage";
    public static final String IO_WAIT_KEY = "ioWait";
    public static final String NETWORK_BANDWIDTH_KEY = "networkBandwidth";
    private final static Logger logger = LoggerFactory.getLogger(OutsideContainerResourcesInfoCollector.class.getName());
    private static final ThreadLocal<DecimalFormat> decimalFormatThreadLocal = new ThreadLocal<DecimalFormat>() {
        @Override
        protected DecimalFormat initialValue() {
            return new DecimalFormat("0.00");
        }
    };

    private static long lastErrorTime;
    private int printLogCount = 2;

    private static GcSnapshot gcSnapshot;

    @Override
    public void run() {
        try {
            long timeStamp = System.currentTimeMillis() / 1000;
            String appName = AppNameUtils.appName();
            StringBuilder stringBuilder = new StringBuilder();
            HardwareAbstractionLayer hal = si.getHardware();
            CentralProcessor processor = si.getHardware().getProcessor();
            Map<String, String> cpuInfoResult = getCpuUsageAndIoWaitAndNetwork(hal);
            String[] cpuLoad = getCpuLoad(processor);
            String memoryUsage = getMemoryUsage(hal.getMemory());
            int cpuNum = getCpus(processor);
            long[] diskReadWrites = getDisk(hal);
            hal.getNetworkIFs()[0].getSpeed();
            long[] diskUses = getFileSystem();
            stringBuilder.append(appName).append("|").append(timeStamp).append("|");
            // 新版本兼容老版本的控制台和大数据
            if (StringUtils.isNotBlank(Pradar.PRADAR_ENV_CODE)) {
                stringBuilder.append(
                                StringUtils.isBlank(Pradar.PRADAR_TENANT_KEY) ? "" : Pradar.PRADAR_TENANT_KEY).append(
                                "|")
                        .append(Pradar.PRADAR_ENV_CODE).append("|")
                        .append(StringUtils.isBlank(Pradar.PRADAR_USER_ID) ? "" : Pradar.PRADAR_USER_ID).append(
                                "|");
            }

            stringBuilder.append(Pradar.AGENT_ID_NOT_CONTAIN_USER_INFO).append("|")
                    .append(cpuInfoResult.get(CPU_USAGE_KEY) == null ? "" : cpuInfoResult.get(CPU_USAGE_KEY))
                    .append("|")
                    .append(cpuLoad[0]).append("|")
                    .append(cpuLoad[1]).append("|")
                    .append(cpuLoad[2]).append("|")
                    .append(memoryUsage).append("|")
                    .append(hal.getMemory().getTotal()).append("|")
                    .append(hal.getMemory().getAvailable()).append("|")
                    //                            .append(cpuInfoResult.get(IO_WAIT_KEY) == null ? "" :
                    //                            cpuInfoResult.get(IO_WAIT_KEY)).append("|")
                    .append(0).append("|")
                    //                            .append(cpuInfoResult.get(NETWORK_BANDWIDTH_RATE_KEY) == null ?
                    //                            0 : cpuInfoResult.get(NETWORK_BANDWIDTH_RATE_KEY)).append('|')
                    .append(0).append("|")
                    .append(cpuInfoResult.get(NETWORK_BANDWIDTH_KEY) == null ? 0
                            : cpuInfoResult.get(NETWORK_BANDWIDTH_KEY)).append('|')
                    .append(cpuNum).append("|")
                    .append(diskUses != null ? diskUses[0] : "").append('|')
                    .append(diskUses != null ? diskUses[1] : "").append('|')
                    .append(diskReadWrites != null ? diskReadWrites[0] : "").append('|')
                    .append(diskReadWrites != null ? diskReadWrites[1] : "").append('|');

            // 14版本增加gc次数及时间
            if (Pradar.PRADAR_MONITOR_LOG_VERSION > 13) {
                long youngGcCount = 0, youngGcCosts = 0, oldGcCount = 0, oldGcCosts = 0;
                if (gcSnapshot == null) {
                    gcSnapshot = buildGcSnapshot();
                } else {
                    GcSnapshot latest = buildGcSnapshot();
                    youngGcCount = latest.youngGcCount - gcSnapshot.youngGcCount;
                    youngGcCosts = latest.youngGcCosts - gcSnapshot.youngGcCosts;
                    oldGcCount = latest.oldGcCount - gcSnapshot.oldGcCount;
                    oldGcCosts = latest.oldGcCosts - gcSnapshot.oldGcCosts;
                    gcSnapshot = latest;
                }
                stringBuilder.append(youngGcCount).append("|").append(youngGcCosts)
                        .append(oldGcCount).append("|").append(oldGcCosts);
            }

            if (StringUtils.isNotBlank(Pradar.PRADAR_ENV_CODE)) {
                stringBuilder.append(0).append('|');

            }
            stringBuilder.append(Pradar.PRADAR_MONITOR_LOG_VERSION)
                    .append(PradarCoreUtils.NEWLINE);
            Pradar.commitMonitorLog(stringBuilder.toString());
        } catch (Throwable e) {
            if (printLogCount > 0) {
                printLogCount--;
                logger.error("write server monitor error!", e);
            }
        }
    }

    /**
     * 获取cpuLoad
     *
     * @return
     */
    public String[] getCpuLoad(CentralProcessor processor) {
        double[] loadAverage = processor.getSystemLoadAverage(3);
        return new String[]{decimalFormatThreadLocal.get().format(loadAverage[0]),
                decimalFormatThreadLocal.get().format(loadAverage[1]), decimalFormatThreadLocal.get().format(
                loadAverage[2])};
    }

    /**
     * 获取cpu利用率和iowait
     *
     * @return
     */
    public Map<String, String> getCpuUsageAndIoWaitAndNetwork(HardwareAbstractionLayer hal) {
        long[] prevTicks = hal.getProcessor().getSystemCpuLoadTicks();
        NetworkIF eth = null;
        long sleep = 450;
        Util.sleep(sleep);
        long[] ticks = hal.getProcessor().getSystemCpuLoadTicks();

        long user = ticks[CentralProcessor.TickType.USER.getIndex()]
                - prevTicks[CentralProcessor.TickType.USER.getIndex()];
        long nice = ticks[CentralProcessor.TickType.NICE.getIndex()]
                - prevTicks[CentralProcessor.TickType.NICE.getIndex()];
        long sys = ticks[CentralProcessor.TickType.SYSTEM.getIndex()]
                - prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
        long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()]
                - prevTicks[CentralProcessor.TickType.IDLE.getIndex()];
        long iowait = ticks[CentralProcessor.TickType.IOWAIT.getIndex()]
                - prevTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
        long irq = ticks[CentralProcessor.TickType.IRQ.getIndex()]
                - prevTicks[CentralProcessor.TickType.IRQ.getIndex()];
        long softirq = ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()]
                - prevTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
        //        long steal = ticks[CentralProcessor.TickType.STEAL.getIndex()] - prevTicks[CentralProcessor
        //        .TickType.STEAL.getIndex()];
        //        long totalCpu = user + nice + sys + idle + iowait + irq + softirq + steal;
        long totalCpu = user + nice + sys + idle + iowait + irq + softirq;
        double cpuUsage = (totalCpu - idle) * 100d / totalCpu;
        double ioWait = iowait * 100d / totalCpu;
        Map<String, String> result = new HashMap<String, String>();
        if (Double.isNaN(cpuUsage)) {
            result.put(CPU_USAGE_KEY, decimalFormatThreadLocal.get().format(0));
        } else {
            result.put(CPU_USAGE_KEY, decimalFormatThreadLocal.get().format(cpuUsage));
        }
        if (Double.isNaN(ioWait)) {
            result.put(IO_WAIT_KEY, decimalFormatThreadLocal.get().format(0));
        } else {
            result.put(IO_WAIT_KEY, decimalFormatThreadLocal.get().format(ioWait));
        }

        return result;
    }

    /**
     * 获取内存使用率
     *
     * @param memory
     * @return
     */
    public String getMemoryUsage(GlobalMemory memory) {
        return decimalFormatThreadLocal.get().format(
                (memory.getTotal() - memory.getAvailable()) * 100d / memory.getTotal());
    }

    /**
     * cpu 核数量
     */
    public int getCpus(CentralProcessor processor) {
        return processor.getPhysicalProcessorCount();
    }

    /**
     * 获取磁盘
     *
     * @return
     */
    public long[] getDisk(HardwareAbstractionLayer hal) {
        try {
            List<HWDiskStore> diskStores = Arrays.asList(hal.getDiskStores());
            long readBytes = 0L;
            long writeBytes = 0L;
            for (HWDiskStore hwDiskStore : diskStores) {
                readBytes += hwDiskStore.getReadBytes();
                writeBytes += hwDiskStore.getWriteBytes();
            }
            return new long[]{readBytes, writeBytes};
        } catch (Throwable e) {
            return null;
        }

    }

    public long[] getFileSystem() {
        try {
            FileSystem fileSystem = si.getOperatingSystem().getFileSystem();
            List<OSFileStore> fileStores = Arrays.asList(fileSystem.getFileStores());
            long totalSpace = 0L;
            long useSpace = 0L;
            for (OSFileStore store : fileStores) {
                totalSpace += store.getTotalSpace();
                useSpace += store.getUsableSpace();
            }
            return new long[]{totalSpace, useSpace};
        } catch (Throwable e) {
            if (System.currentTimeMillis() - lastErrorTime > 600000) {
                logger.warn("getFileSystem error! ", e);
                lastErrorTime = System.currentTimeMillis();
            }
            return null;
        }

    }

    private GcSnapshot buildGcSnapshot() {
        List<GarbageCollectorMXBean> garbageCollectorMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        GcSnapshot snapshot = new GcSnapshot();
        snapshot.youngGcCount = garbageCollectorMXBeans.get(0).getCollectionCount();
        snapshot.youngGcCosts = garbageCollectorMXBeans.get(0).getCollectionTime();
        snapshot.oldGcCount = garbageCollectorMXBeans.get(1).getCollectionCount();
        snapshot.oldGcCosts = garbageCollectorMXBeans.get(1).getCollectionTime();
        return snapshot;
    }

    private static class GcSnapshot {
        /**
         * gc次数和耗时
         */
        private long youngGcCount;
        private long youngGcCosts;
        private long oldGcCount;
        private long oldGcCosts;

    }

}
