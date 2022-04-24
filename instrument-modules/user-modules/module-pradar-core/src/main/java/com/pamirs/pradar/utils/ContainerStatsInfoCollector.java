/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.pradar.utils;

import com.sun.management.OperatingSystemMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.util.FileUtil;
import oshi.util.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jiangjibo
 * @date 2021/10/22 9:35 上午
 * @description: 容器水位信息采集
 */
public class ContainerStatsInfoCollector {

    private final static Logger logger = LoggerFactory.getLogger(ContainerStatsInfoCollector.class.getName());

    /**
     * 偶数次采集数据
     */
    private volatile StatsInfo previousRecord;

    /**
     * 网口
     */
    private volatile String eth;

    /**
     * 核心数量
     */
    private volatile int coresNum;

    /**
     * 用户时钟
     */
    private volatile int userHz;

    /**
     * 网卡速度, 默认1000Mbs, 会在控制台配置覆盖这个值
     */
    private volatile int networkSpeed = 1000;

    private static long lastErrorTime;

    public ContainerStatsInfoCollector() {
        this.previousRecord = collectContainerStatsInfo();
    }

    public ContainerStatsInfo getContainerStatsInfo() {
        if (this.previousRecord == null) {
            this.previousRecord = collectContainerStatsInfo();
            Util.sleep(450);
        }
        if (previousRecord == null) {
            return null;
        }
        StatsInfo previous = previousRecord;
        StatsInfo latest = this.collectContainerStatsInfo();

        // 采集时间差
        long timeDiff = latest.collectingTime - previous.collectingTime;

        ContainerStatsInfo statsInfo = new ContainerStatsInfo();
        statsInfo.coresNum = this.coresNum;

        DecimalFormat format = new DecimalFormat("0.00");

        // 100% * 容器使用cpu时间 / 系统使用cpu时间
        //cpu使用率是整体的, 上限就是100，所以不乘核数
        BigDecimal containerCpuDelta = latest.containerCpuValue.subtract(previous.containerCpuValue);
        BigDecimal systemCpuDelta = new BigDecimal(latest.systemCpuValue - previous.systemCpuValue).multiply(new BigDecimal(1000 * 1000 * 1000 / userHz));

        statsInfo.cpuUsagePercent = containerCpuDelta.multiply(new BigDecimal(100)).divide(systemCpuDelta, 2, BigDecimal.ROUND_HALF_UP).doubleValue();
        if (statsInfo.cpuUsagePercent  < 2){
            OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
            double systemCpuLoad = osBean.getSystemCpuLoad();
            if (systemCpuLoad != 0){
                statsInfo.cpuUsagePercent = new BigDecimal(systemCpuLoad).multiply(new BigDecimal(100),new MathContext(2, RoundingMode.HALF_UP)).doubleValue();
            }
        }

        statsInfo.cpuIoWaitUsagePercent = Double.parseDouble(format.format(100 * (latest.systemIoWaitCpuValue - previous.systemIoWaitCpuValue) / (latest.systemCpuValue - previous.systemCpuValue)));

        statsInfo.latest1MinLoadAvg = Double.parseDouble(format.format(latest.latest1MinLoadAvg));
        statsInfo.latest5MinLoadAvg = Double.parseDouble(format.format(latest.latest5MinLoadAvg));
        statsInfo.latest15MinLoadAvg = Double.parseDouble(format.format(latest.latest15MinLoadAvg));
        statsInfo.totalMemory = latest.totalMemoryValue;
        statsInfo.availableMemory = latest.memAvailableValue;
        statsInfo.memoryUsagePercent = Double.parseDouble(format.format(100.0 * latest.usedMemoryValue / latest.totalMemoryValue));
        statsInfo.totalDiskSpace = latest.totalDiskSpaceValue;
        statsInfo.usableDiskSpace = latest.availableDiskSpaceValue;
        statsInfo.diskReadBytes = latest.diskReadBytesValue;
        statsInfo.diskWriteBytes = latest.diskWriteBytesValue;
        statsInfo.diskReadRate = (latest.diskReadBytesValue - previous.diskReadBytesValue) * 1000 / timeDiff;
        statsInfo.diskWriteRate = (latest.diskWriteBytesValue - previous.diskWriteBytesValue) * 1000 / timeDiff;
        statsInfo.txBytes = latest.txBytesValue;
        statsInfo.txRate = (latest.txBytesValue - previous.txBytesValue) / timeDiff * 1000;
        statsInfo.rxBytes = latest.rxBytesValue;
        statsInfo.rxRate = (latest.rxBytesValue - previous.rxBytesValue) / timeDiff * 1000;
        statsInfo.networkSpeed = this.networkSpeed;
        statsInfo.networkUsage = Double.parseDouble(new DecimalFormat("0.0000000").format(100 * (statsInfo.rxRate + statsInfo.txRate) / (networkSpeed / 8.0 * 1024 * 1024)));

        this.previousRecord = latest;
        return statsInfo;
    }

    /**
     * 采集容器水位信息
     */
    private StatsInfo collectContainerStatsInfo() {
        try {
            return doCollectContainerStatsInfo();
        } catch (IOException e) {
            if (System.currentTimeMillis() - lastErrorTime > 600000) {
                logger.warn("collect container stats info error! ", e);
                lastErrorTime = System.currentTimeMillis();
            }
        }
        return null;
    }

    private StatsInfo doCollectContainerStatsInfo() throws IOException {
        StatsInfo statsInfo = new StatsInfo();
        statsInfo.collectingTime = System.currentTimeMillis();
        // CPU
        List<String> contents = FileUtil.readFile("/proc/stat");
        String[] splits = contents.get(0).split("\\s+");
        statsInfo.systemIoWaitCpuValue = Long.parseLong(splits[5]);
        statsInfo.systemCpuValue = Long.parseLong(splits[1]) + Long.parseLong(splits[2]) + Long.parseLong(splits[3]) + Long.parseLong(splits[4]) + Long.parseLong(splits[5]) + Long.parseLong(splits[6]) + Long.parseLong(splits[7]) + Long.parseLong(splits[8]);

        statsInfo.containerCpuValue = new BigDecimal(FileUtil.readFile("/sys/fs/cgroup/cpuacct/cpuacct.usage").get(0));
        // 网口
        if (this.eth == null) {
            List<String> eths = execCommand("ip -o -4 route show to default | awk '{print $5}'");
            if (!eths.isEmpty()) {
                this.eth = eths.get(0);
            }else if(new File("/sys/class/net/eth0/speed").exists()){
                // 看看是否存在eth0网口,存在则用这个
                this.eth = "eth0";
            }
        }

        // 用户赫兹
        if (this.userHz == 0) {
            this.userHz = Integer.parseInt(execCommand("getconf CLK_TCK").get(0));
        }
        // 网络
        if (eth != null) {
            statsInfo.rxBytesValue = Long.parseLong(FileUtil.readFile(String.format("/sys/class/net/%s/statistics/rx_bytes", eth)).get(0));
            statsInfo.txBytesValue = Long.parseLong(FileUtil.readFile(String.format("/sys/class/net/%s/statistics/tx_bytes", eth)).get(0));
        }
        // CPU核心
        if (coresNum == 0) {
            String coreNumLine = FileUtil.readFile("/sys/fs/cgroup/cpuset/cpuset.cpus").get(0);
            int coreNum = 0;
            for (String split : coreNumLine.split(",")) {
                if (split == null || split.trim().length() == 0) {
                    continue;
                }
                if (!split.contains("-")) {
                    coreNum++;
                }
                String[] coreSplit = split.split("-", 2);
                coreNum += Integer.parseInt(coreSplit[1]) - Integer.parseInt(coreSplit[0]) + 1;
            }
            this.coresNum = coreNum;
        }
        // 负载
        String loadavgLine = FileUtil.readFile("/proc/loadavg").get(0);
        String[] loadavgs = loadavgLine.split(" ");
        statsInfo.latest1MinLoadAvg = Float.parseFloat(loadavgs[0].trim());
        statsInfo.latest5MinLoadAvg = Float.parseFloat(loadavgs[1].trim());
        statsInfo.latest15MinLoadAvg = Float.parseFloat(loadavgs[2].trim());
        // 内存
        List<String> meminfoLines = FileUtil.readFile("/sys/fs/cgroup/memory/memory.stat");
        long limit = Long.MAX_VALUE;
        long rss = 0, mappedFile = 0;
        for (String line : meminfoLines) {
            line = line.trim();
            if (line.startsWith("hierarchical_memory_limit ")) {
                limit = Long.parseLong(line.split(" ", 2)[1]);
                continue;
            }
            if (line.startsWith("rss ")) {
                rss = Long.parseLong(line.split(" ", 2)[1]);
                continue;
            }
            if (line.startsWith("mapped_file ")) {
                mappedFile = Long.parseLong(line.split(" ", 2)[1]);
            }
        }

        List<String> memInfo = FileUtil.readFile("/proc/meminfo");
        boolean found = false;
        long memAvailable = 0, memFree = 0, activeFile = 0, inactiveFile = 0, sReclaimable = 0, memTotal = 0;

        for (String line : memInfo) {
            String[] memorySplit = line.split("\\s+");
            if (memorySplit.length > 1) {
                if ("MemTotal:".equals(memorySplit[0])) {
                    memTotal = this.parseMeminfo(memorySplit);
                } else if ("MemFree:".equals(memorySplit[0])) {
                    memFree = this.parseMeminfo(memorySplit);
                } else if ("MemAvailable:".equals(memorySplit[0])) {
                    memAvailable = this.parseMeminfo(memorySplit);
                    found = true;
                } else if ("Active(file):".equals(memorySplit[0])) {
                    activeFile = this.parseMeminfo(memorySplit);
                } else if ("Inactive(file):".equals(memorySplit[0])) {
                    inactiveFile = this.parseMeminfo(memorySplit);
                } else if ("SReclaimable:".equals(memorySplit[0])) {
                    sReclaimable = this.parseMeminfo(memorySplit);
                }
            }
        }
        if (!found) {
            memAvailable = memFree + activeFile + inactiveFile + sReclaimable;
        }
        statsInfo.totalMemoryValue = Math.min(limit, memTotal);
        statsInfo.usedMemoryValue = rss + mappedFile;
        statsInfo.memAvailableValue = memAvailable;

        // IO
        List<String> ioInfos = FileUtil.readFile("/sys/fs/cgroup/blkio/blkio.throttle.io_service_bytes");
        long read = 0, write = 0;
        for (String line : ioInfos) {
            if (line.contains("Read")) {
                read += Long.parseLong(line.trim().substring(line.lastIndexOf(" ")).trim());
            }
            if (line.contains("Write")) {
                write += Long.parseLong(line.trim().substring(line.lastIndexOf(" ")).trim());
            }
        }
        statsInfo.diskWriteBytesValue = write;
        statsInfo.diskReadBytesValue = read;

        List<String> cmdResult = execCommand("df");
        for (String line : cmdResult) {
            if (line.trim().endsWith("/")) {
                String[] split = line.split("\\s+");
                statsInfo.totalDiskSpaceValue = Long.parseLong(split[1]) * 1024;
                statsInfo.usedDiskSpaceValue = Long.parseLong(split[2]) * 1024;
                statsInfo.availableDiskSpaceValue = Long.parseLong(split[3]) * 1024;
                break;
            }
        }
        return statsInfo;
    }

    private List<String> execCommand(String cmd) throws IOException {
        List<String> result = new ArrayList<String>();
        Process ps = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", cmd});
        BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
        String line;
        while ((line = br.readLine()) != null) {
            result.add(line);
        }
        return result;
    }

    private long parseMeminfo(String[] memorySplit) {
        if (memorySplit.length < 2) {
            return 0L;
        } else {
            long memory = Long.parseLong(memorySplit[1]);
            if (memorySplit.length > 2 && "kB".equals(memorySplit[2])) {
                memory *= 1024L;
            }

            return memory;
        }
    }

    private static class StatsInfo {

        /**
         * 采集时间
         */
        private long collectingTime;

        /**
         * 系统CPU处理iowait状态时间
         */
        private long systemIoWaitCpuValue;

        /**
         * 容器系统cpu
         */
        private long systemCpuValue;

        /**
         * 容器使用cpu
         */
        private BigDecimal containerCpuValue;

        /**
         * 容器内存上限
         */
        private long totalMemoryValue;

        /**
         * 容器使用内存
         */
        private long usedMemoryValue;

        /**
         * 可用内存
         */
        private long memAvailableValue;

        /**
         * 容器累计接受字节
         */
        private long rxBytesValue;

        /**
         * 容器累计发送字节
         */
        private long txBytesValue;

        /**
         * 容器磁盘写字节
         */
        private long diskWriteBytesValue;

        /**
         * 容器磁盘读字节
         */
        private long diskReadBytesValue;

        /**
         * 所有磁盘空间
         */
        private long totalDiskSpaceValue;

        /**
         * 已用磁盘空间
         */
        private long usedDiskSpaceValue;

        /**
         * 可用空间
         */
        private long availableDiskSpaceValue;

        /**
         * 负载
         */
        private float latest1MinLoadAvg;
        private float latest5MinLoadAvg;
        private float latest15MinLoadAvg;

    }

    public static class ContainerStatsInfo {
        private double cpuUsagePercent;
        private double cpuIoWaitUsagePercent;
        private double latest1MinLoadAvg;
        private double latest5MinLoadAvg;
        private double latest15MinLoadAvg;
        private long totalMemory;
        private long availableMemory;
        private double memoryUsagePercent;
        private int coresNum;
        private long totalDiskSpace;
        private long usableDiskSpace;
        private long diskReadBytes;
        private long diskReadRate;
        private long diskWriteBytes;
        private long diskWriteRate;
        private long rxBytes;
        private long rxRate;
        private long txBytes;
        private long txRate;
        private long networkSpeed;
        private double networkUsage;

        public double getCpuUsagePercent() {
            return cpuUsagePercent;
        }

        public double getCpuIoWaitUsagePercent() {
            return cpuIoWaitUsagePercent;
        }

        public double getLatest1MinLoadAvg() {
            return latest1MinLoadAvg;
        }

        public double getLatest5MinLoadAvg() {
            return latest5MinLoadAvg;
        }

        public double getLatest15MinLoadAvg() {
            return latest15MinLoadAvg;
        }

        public long getTotalMemory() {
            return totalMemory;
        }

        public long getAvailableMemory() {
            return availableMemory;
        }

        public double getMemoryUsagePercent() {
            return memoryUsagePercent;
        }

        public int getCoresNum() {
            return coresNum;
        }

        public long getTotalDiskSpace() {
            return totalDiskSpace;
        }

        public long getUsableDiskSpace() {
            return usableDiskSpace;
        }

        public long getDiskReadBytes() {
            return diskReadBytes;
        }

        public long getDiskReadRate() {
            return diskReadRate;
        }

        public long getDiskWriteBytes() {
            return diskWriteBytes;
        }

        public long getDiskWriteRate() {
            return diskWriteRate;
        }

        public long getRxBytes() {
            return rxBytes;
        }

        public long getRxRate() {
            return rxRate;
        }

        public long getTxBytes() {
            return txBytes;
        }

        public long getTxRate() {
            return txRate;
        }

        public long getNetworkSpeed() {
            return networkSpeed;
        }

        public double getNetworkUsage() {
            return networkUsage;
        }
    }

}
