/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shulie.instrument.simulator.agent.core.logback;

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.LiteralConverter;
import ch.qos.logback.core.rolling.helper.*;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.util.FileSize;
import com.shulie.instrument.simulator.agent.core.util.CustomerReflectUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static ch.qos.logback.core.CoreConstants.UNBOUNDED_TOTAL_SIZE_CAP;

/**
 * @author angju
 * @date 2021/8/20 11:36
 */
public class CustomerTimeBasedArchiveRemover extends ContextAwareBase implements ArchiveRemover {

    static protected final long UNINITIALIZED = -1;
    // aim for 32 days, except in case of hourly rollover
    static protected final long INACTIVITY_TOLERANCE_IN_MILLIS = 32L * (long) CoreConstants.MILLIS_IN_ONE_DAY;
    static final int MAX_VALUE_FOR_INACTIVITY_PERIODS = 14 * 24; // 14 days in case of hourly rollover

    final FileNamePattern fileNamePattern;
    final RollingCalendar rc;
    private int maxHistory = CoreConstants.UNBOUND_HISTORY;
    private long totalSizeCap = CoreConstants.UNBOUNDED_TOTAL_SIZE_CAP;
    final boolean parentClean;
    long lastHeartBeat = UNINITIALIZED;

    public CustomerTimeBasedArchiveRemover(FileNamePattern fileNamePattern, RollingCalendar rc) {
        this.fileNamePattern = fileNamePattern;
        this.rc = rc;
        this.parentClean = computeParentCleaningFlag(fileNamePattern);
    }

    int callCount = 0;
    @Override
    public void clean(Date now) {

        long nowInMillis = now.getTime();
        // for a live appender periodsElapsed is expected to be 1
        int periodsElapsed = computeElapsedPeriodsSinceLastClean(nowInMillis);
        lastHeartBeat = nowInMillis;
        if (periodsElapsed > 1) {
            addInfo("Multiple periods, i.e. " + periodsElapsed + " periods, seem to have elapsed. This is expected at application start.");
        }
        for (int i = 0; i < periodsElapsed; i++) {
            int offset = getPeriodOffsetForDeletionTarget() - i;
            Date dateOfPeriodToClean = rc.getEndOfNextNthPeriod(now, offset);
            cleanPeriod(dateOfPeriodToClean);
        }
    }

    protected File[] getFilesInPeriod(Date dateOfPeriodToClean) {
        String filenameToDelete = fileNamePattern.convert(dateOfPeriodToClean);
        File file2Delete = new File(filenameToDelete);

        if (fileExistsAndIsFile(file2Delete)) {
            return new File[] { file2Delete };
        } else {
            return new File[0];
        }
    }

    private boolean fileExistsAndIsFile(File file2Delete) {
        return file2Delete.exists() && file2Delete.isFile();
    }

    public void cleanPeriod(Date dateOfPeriodToClean) {
        File[] matchingFileArray = getFilesInPeriod(dateOfPeriodToClean);

        for (File f : matchingFileArray) {
            addInfo("deleting " + f);
            f.delete();
        }

        if (parentClean && matchingFileArray.length > 0) {
            File parentDir = getParentDir(matchingFileArray[0]);
            removeFolderIfEmpty(parentDir);
        }
    }

    void capTotalSize(Date now) {
//        long totalSize = 0;
//        long totalRemoved = 0;
        Date date = rc.getEndOfNextNthPeriod(now, 0);
        File[] matchingFileArray = getAllFiles(date);
        descendingSortByLastModified(matchingFileArray);
        int length = matchingFileArray.length;
        int deleteIndex = 0;
        while (length >= maxHistory){
            addInfo("Deleting [" + matchingFileArray[0] + "]" + " of size " + new FileSize(matchingFileArray[0].length()));
            matchingFileArray[deleteIndex].delete();
            deleteIndex++;
            length--;
        }

//        for (int offset = 0; offset < maxHistory; offset++) {
//            Date date = rc.getEndOfNextNthPeriod(now, -offset);
////            File[] matchingFileArray = getFilesInPeriod(date);
//            File[] matchingFileArray = getAllFiles(date);
//            descendingSortByLastModified(matchingFileArray);
//            for (File f : matchingFileArray) {
//                long size = f.length();
//                if (totalSize + size > totalSizeCap) {
//                    addInfo("Deleting [" + f + "]" + " of size " + new FileSize(size));
//                    totalRemoved += size;
//                    f.delete();
//                }
//                totalSize += size;
//            }
//        }
//        addInfo("Removed  " + new FileSize(totalRemoved) + " of files");
    }

    /**
     * 获取所有文件，并按照后缀从小到大排序
     * @param dateOfPeriodToClean
     * @return
     */
    private File[] getAllFiles(Date dateOfPeriodToClean){
        File archive0 = new File(fileNamePattern.convertMultipleArguments(dateOfPeriodToClean, 0));
        File parentDir = getParentDir(archive0);
        String stemRegex = archive0.getName().split("\\.")[0] + ".log.(\\d+)$";
        File[] matchingFileArray = FileFilterUtil.filesInFolderMatchingStemRegex(parentDir, stemRegex);

        return matchingFileArray;
    }

    private void descendingSortByLastModified(File[] matchingFileArray) {
        Arrays.sort(matchingFileArray, new Comparator<File>() {
            @Override
            public int compare(final File f1, final File f2) {
                long l1 = f1.lastModified();
                long l2 = f2.lastModified();
                if (l1 == l2)
                    return 0;
                // descending sort, i.e. newest files first
                if (l2 < l1)
                    return 1;
                else
                    return -1;
            }
        });
    }

    File getParentDir(File file) {
        File absolute = file.getAbsoluteFile();
        File parentDir = absolute.getParentFile();
        return parentDir;
    }

    int computeElapsedPeriodsSinceLastClean(long nowInMillis) {
        long periodsElapsed = 0;
        if (lastHeartBeat == UNINITIALIZED) {
            addInfo("first clean up after appender initialization");
            periodsElapsed = rc.periodBarriersCrossed(nowInMillis, nowInMillis + INACTIVITY_TOLERANCE_IN_MILLIS);
            periodsElapsed = Math.min(periodsElapsed, MAX_VALUE_FOR_INACTIVITY_PERIODS);
        } else {
            periodsElapsed = rc.periodBarriersCrossed(lastHeartBeat, nowInMillis);
            // periodsElapsed of zero is possible for size and time based policies
        }
        return (int) periodsElapsed;
    }

    boolean computeParentCleaningFlag(FileNamePattern fileNamePattern) {
        DateTokenConverter<Object> dtc = fileNamePattern.getPrimaryDateTokenConverter();
        // if the date pattern has a /, then we need parent cleaning
        if (dtc.getDatePattern().indexOf('/') != -1) {
            return true;
        }
        // if the literal string subsequent to the dtc contains a /, we also
        // need parent cleaning

//        Converter<Object> p = fileNamePattern.headTokenConverter;
        Converter<Object> p = CustomerReflectUtils.getHeadTokenConverter(fileNamePattern);

        // find the date converter
        while (p != null) {
            if (p instanceof DateTokenConverter) {
                break;
            }
            p = p.getNext();
        }

        while (p != null) {
            if (p instanceof LiteralConverter) {
                String s = p.convert(null);
                if (s.indexOf('/') != -1) {
                    return true;
                }
            }
            p = p.getNext();
        }

        // no /, so we don't need parent cleaning
        return false;
    }

    void removeFolderIfEmpty(File dir) {
        removeFolderIfEmpty(dir, 0);
    }

    /**
     * Will remove the directory passed as parameter if empty. After that, if the
     * parent is also becomes empty, remove the parent dir as well but at most 3
     * times.
     *
     * @param dir
     * @param depth
     */
    private void removeFolderIfEmpty(File dir, int depth) {
        // we should never go more than 3 levels higher
        if (depth >= 3) {
            return;
        }
        if (dir.isDirectory() && FileFilterUtil.isEmptyDirectory(dir)) {
            addInfo("deleting folder [" + dir + "]");
            dir.delete();
            removeFolderIfEmpty(dir.getParentFile(), depth + 1);
        }
    }

    @Override
    public void setMaxHistory(int maxHistory) {
        this.maxHistory = maxHistory;
    }

    protected int getPeriodOffsetForDeletionTarget() {
        return -maxHistory - 1;
    }

    @Override
    public void setTotalSizeCap(long totalSizeCap) {
        this.totalSizeCap = totalSizeCap;
    }

    @Override
    public String toString() {
        return "c.q.l.core.rolling.helper.TimeBasedArchiveRemover";
    }

    @Override
    public Future<?> cleanAsynchronously(Date now) {
        ArhiveRemoverRunnable runnable = new ArhiveRemoverRunnable(now);
        ExecutorService executorService = context.getScheduledExecutorService();
        Future<?> future = executorService.submit(runnable);
        return future;
    }

    public class ArhiveRemoverRunnable implements Runnable {
        Date now;

        ArhiveRemoverRunnable(Date now) {
            this.now = now;
        }

        @Override
        public void run() {
            //不需要按照时间清楚
//            clean(now);
            if (totalSizeCap != UNBOUNDED_TOTAL_SIZE_CAP && totalSizeCap > 0) {
                capTotalSize(now);
            }
        }
    }

}
