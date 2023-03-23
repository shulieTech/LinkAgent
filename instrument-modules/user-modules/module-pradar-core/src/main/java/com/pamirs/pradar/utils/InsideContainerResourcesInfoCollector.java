package com.pamirs.pradar.utils;

import com.pamirs.pradar.AppNameUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarCoreUtils;
import com.pamirs.pradar.PradarSwitcher;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jiangjibo
 * @date 2022/2/11 10:33 上午
 * @description: 规避探针增强MonitorCollector后第二个new Runnable()偶尔会变成Unavailable Anonymous Inner Class!的问题
 */
public class InsideContainerResourcesInfoCollector implements Runnable {

    private final static Logger logger = LoggerFactory.getLogger(InsideContainerResourcesInfoCollector.class.getName());
    private int printLogCount = 2;
    private ContainerStatsInfoCollector collector;

    @Override
    public void run() {
        if (collector == null) {
            collector = new ContainerStatsInfoCollector();
        }
        try {
            long timeStamp = System.currentTimeMillis() / 1000;
            String appName = AppNameUtils.appName();
            StringBuilder stringBuilder = new StringBuilder();
            ContainerStatsInfoCollector.ContainerStatsInfo statsInfo = collector.getContainerStatsInfo();

            stringBuilder.append(appName).append("|").append(timeStamp).append("|");
            if (StringUtils.isNotBlank(Pradar.PRADAR_ENV_CODE)) {
                stringBuilder.append(
                                StringUtils.isBlank(Pradar.PRADAR_TENANT_KEY) ? "" : Pradar.PRADAR_TENANT_KEY).append(
                                "|")
                        .append(StringUtils.isBlank(Pradar.PRADAR_ENV_CODE) ? "" : Pradar.PRADAR_ENV_CODE).append(
                                "|")
                        .append(StringUtils.isBlank(Pradar.PRADAR_USER_ID) ? "" : Pradar.PRADAR_USER_ID).append(
                                "|");
            }
            stringBuilder.append(Pradar.AGENT_ID_NOT_CONTAIN_USER_INFO).append("|")
                    .append(statsInfo.getCpuUsagePercent()).append("|")
                    .append(statsInfo.getLatest1MinLoadAvg()).append("|")
                    .append(statsInfo.getLatest5MinLoadAvg()).append("|")
                    .append(statsInfo.getLatest15MinLoadAvg()).append("|")
                    .append(statsInfo.getMemoryUsagePercent()).append("|")
                    .append(statsInfo.getTotalMemory()).append("|")
                    .append(statsInfo.getAvailableMemory()).append("|")
                    .append(statsInfo.getCpuIoWaitUsagePercent()).append("|")
                    .append(statsInfo.getNetworkUsage()).append('|')
                    .append(statsInfo.getNetworkSpeed()).append('|')
                    .append(statsInfo.getCoresNum()).append("|")
                    .append(statsInfo.getTotalDiskSpace()).append('|')
                    .append(statsInfo.getUsableDiskSpace()).append('|')
                    .append(statsInfo.getDiskReadBytes()).append('|')
                    .append(statsInfo.getDiskWriteBytes()).append('|');
            if (StringUtils.isNotBlank(Pradar.PRADAR_ENV_CODE)) {
                stringBuilder.append(1).append('|');
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
}
