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
import ch.qos.logback.core.rolling.RollingPolicyBase;
import ch.qos.logback.core.rolling.RolloverFailure;
import ch.qos.logback.core.rolling.TriggeringPolicy;
import ch.qos.logback.core.rolling.helper.*;
import ch.qos.logback.core.util.FileSize;
import com.shulie.instrument.simulator.agent.core.util.CustomerReflectUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Date;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static ch.qos.logback.core.CoreConstants.UNBOUNDED_TOTAL_SIZE_CAP;
import static ch.qos.logback.core.CoreConstants.UNBOUND_HISTORY;

/**
 * @author angju
 * @date 2021/8/20 10:40
 */
public class CustomerTimeBasedRollingPolicy<E> extends RollingPolicyBase implements TriggeringPolicy<E> {
    static final String FNP_NOT_SET = "The FileNamePattern option must be set before using TimeBasedRollingPolicy. ";
    // WCS: without compression suffix
    FileNamePattern fileNamePatternWithoutCompSuffix;

    private Compressor compressor;
    private RenameUtil renameUtil = new RenameUtil();
    Future<?> compressionFuture;
    Future<?> cleanUpFuture;

    private int maxHistory = UNBOUND_HISTORY;
    protected FileSize totalSizeCap = new FileSize(UNBOUNDED_TOTAL_SIZE_CAP);

    private ArchiveRemover archiveRemover;

    CustomerTimeBasedFileNamingAndTriggeringPolicy<E> timeBasedFileNamingAndTriggeringPolicy;

    boolean cleanHistoryOnStart = false;

    @Override
    public void start() {
        // set the LR for our utility object
        renameUtil.setContext(this.context);

        // find out period from the filename pattern
        if (fileNamePatternStr != null) {
//            fileNamePattern = new FileNamePattern(fileNamePatternStr, this.context);
            CustomerReflectUtils.setFileNamePattern(new FileNamePattern(fileNamePatternStr, this.context), "fileNamePattern", this);
            determineCompressionMode();
        } else {
            addWarn(FNP_NOT_SET);
            addWarn(CoreConstants.SEE_FNP_NOT_SET);
            throw new IllegalStateException(FNP_NOT_SET + CoreConstants.SEE_FNP_NOT_SET);
        }

        compressor = new Compressor(compressionMode);
        compressor.setContext(context);

        // wcs : without compression suffix
        fileNamePatternWithoutCompSuffix = new FileNamePattern(Compressor.computeFileNameStrWithoutCompSuffix(fileNamePatternStr, compressionMode), this.context);

        addInfo("Will use the pattern " + fileNamePatternWithoutCompSuffix + " for the active file");

        if (compressionMode == CompressionMode.ZIP) {
            String zipEntryFileNamePatternStr = transformFileNamePattern2ZipEntry(fileNamePatternStr);
//            zipEntryFileNamePattern = new FileNamePattern(zipEntryFileNamePatternStr, context);
            CustomerReflectUtils.setFileNamePattern(new FileNamePattern(zipEntryFileNamePatternStr, context), "zipEntryFileNamePattern", this);

        }

        if (timeBasedFileNamingAndTriggeringPolicy == null) {
            timeBasedFileNamingAndTriggeringPolicy = new CustomerDefaultTimeBasedFileNamingAndTriggeringPolicy<E>();
        }
        timeBasedFileNamingAndTriggeringPolicy.setContext(context);
        timeBasedFileNamingAndTriggeringPolicy.setTimeBasedRollingPolicy(this);
        timeBasedFileNamingAndTriggeringPolicy.start();

        if (!timeBasedFileNamingAndTriggeringPolicy.isStarted()) {
            addWarn("Subcomponent did not start. TimeBasedRollingPolicy will not start.");
            return;
        }

        // the maxHistory property is given to TimeBasedRollingPolicy instead of to
        // the TimeBasedFileNamingAndTriggeringPolicy. This makes it more convenient
        // for the user at the cost of inconsistency here.
        if (maxHistory != UNBOUND_HISTORY) {
            archiveRemover = timeBasedFileNamingAndTriggeringPolicy.getArchiveRemover();
            archiveRemover.setMaxHistory(maxHistory);
            archiveRemover.setTotalSizeCap(totalSizeCap.getSize());
            if (cleanHistoryOnStart) {
                addInfo("Cleaning on start up");
                Date now = new Date(timeBasedFileNamingAndTriggeringPolicy.getCurrentTime());
                cleanUpFuture = archiveRemover.cleanAsynchronously(now);
            }
        } else if (!isUnboundedTotalSizeCap()) {
            addWarn("'maxHistory' is not set, ignoring 'totalSizeCap' option with value ["+totalSizeCap+"]");
        }

        super.start();
        initCurrentSize(getParentsRawFileProperty());

    }

    protected boolean isUnboundedTotalSizeCap() {
        return totalSizeCap.getSize() == UNBOUNDED_TOTAL_SIZE_CAP;
    }

    @Override
    public void stop() {
        if (!isStarted()){
            return;
        }
        waitForAsynchronousJobToStop(compressionFuture, "compression");
        waitForAsynchronousJobToStop(cleanUpFuture, "clean-up");
        super.stop();
    }

    private void waitForAsynchronousJobToStop(Future<?> aFuture, String jobDescription) {
        if (aFuture != null) {
            try {
                aFuture.get(CoreConstants.SECONDS_TO_WAIT_FOR_COMPRESSION_JOBS, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                addError("Timeout while waiting for " + jobDescription + " job to finish", e);
            } catch (Exception e) {
                addError("Unexpected exception while waiting for " + jobDescription + " job to finish", e);
            }
        }
    }

    private String transformFileNamePattern2ZipEntry(String fileNamePatternStr) {
        String slashified = FileFilterUtil.slashify(fileNamePatternStr);
        return FileFilterUtil.afterLastSlash(slashified);
    }

    public void setTimeBasedFileNamingAndTriggeringPolicy(CustomerDefaultTimeBasedFileNamingAndTriggeringPolicy<E> timeBasedTriggering) {
        this.timeBasedFileNamingAndTriggeringPolicy = timeBasedTriggering;
    }

    public CustomerTimeBasedFileNamingAndTriggeringPolicy<E> getTimeBasedFileNamingAndTriggeringPolicy() {
        return timeBasedFileNamingAndTriggeringPolicy;
    }

    @Override
    public void rollover() throws RolloverFailure {

        // when rollover is called the elapsed period's file has
        // been already closed. This is a working assumption of this method.

//        String elapsedPeriodsFileName = timeBasedFileNamingAndTriggeringPolicy.getElapsedPeriodsFileName();
        //去除文件名中的日志信息
        String parentsRawFile = getParentsRawFileProperty() + "." + lastFileSuffix;
        lastFileSuffix += (new File(parentsRawFile).length());

//        String elapsedPeriodStem = FileFilterUtil.afterLastSlash(elapsedPeriodsFileName);

//        if (compressionMode == CompressionMode.NONE) {
//            if (getParentsRawFileProperty() != null) {
//                renameUtil.rename(getParentsRawFileProperty(), parentsRawFile + "." + lastFileSuffix);
//            } // else { nothing to do if CompressionMode == NONE and parentsRawFileProperty == null }
//        }
        //不压缩
//        else {
//            if (getParentsRawFileProperty() == null) {
//                compressionFuture = compressor.asyncCompress(elapsedPeriodsFileName, elapsedPeriodsFileName, elapsedPeriodStem);
//            } else {
//                compressionFuture = renameRawAndAsyncCompress(elapsedPeriodsFileName, elapsedPeriodStem);
//            }
//        }

        if (archiveRemover != null) {
            Date now = new Date(timeBasedFileNamingAndTriggeringPolicy.getCurrentTime());
            this.cleanUpFuture = archiveRemover.cleanAsynchronously(now);
        }
    }
//    @Override
//    public void rollover() throws RolloverFailure {
//
//        // when rollover is called the elapsed period's file has
//        // been already closed. This is a working assumption of this method.
//
//        String elapsedPeriodsFileName = timeBasedFileNamingAndTriggeringPolicy.getElapsedPeriodsFileName();
//
//        String elapsedPeriodStem = FileFilterUtil.afterLastSlash(elapsedPeriodsFileName);
//
//        if (compressionMode == CompressionMode.NONE) {
//            if (getParentsRawFileProperty() != null) {
//                renameUtil.rename(getParentsRawFileProperty(), elapsedPeriodsFileName);
//            } // else { nothing to do if CompressionMode == NONE and parentsRawFileProperty == null }
//        } else {
//            if (getParentsRawFileProperty() == null) {
//                compressionFuture = compressor.asyncCompress(elapsedPeriodsFileName, elapsedPeriodsFileName, elapsedPeriodStem);
//            } else {
//                compressionFuture = renameRawAndAsyncCompress(elapsedPeriodsFileName, elapsedPeriodStem);
//            }
//        }
//
//        if (archiveRemover != null) {
//            Date now = new Date(timeBasedFileNamingAndTriggeringPolicy.getCurrentTime());
//            this.cleanUpFuture = archiveRemover.cleanAsynchronously(now);
//        }
//    }

    Future<?> renameRawAndAsyncCompress(String nameOfCompressedFile, String innerEntryName) throws RolloverFailure {
        String parentsRawFile = getParentsRawFileProperty();
        String tmpTarget = nameOfCompressedFile + System.nanoTime() + ".tmp";
        renameUtil.rename(parentsRawFile, tmpTarget);
        return compressor.asyncCompress(tmpTarget, nameOfCompressedFile, innerEntryName);
    }

    /**
     *
     * The active log file is determined by the value of the parent's filename
     * option. However, in case the file name is left blank, then, the active log
     * file equals the file name for the current period as computed by the
     * <b>FileNamePattern</b> option.
     *
     * <p>The RollingPolicy must know whether it is responsible for changing the
     * name of the active file or not. If the active file name is set by the user
     * via the configuration file, then the RollingPolicy must let it like it is.
     * If the user does not specify an active file name, then the RollingPolicy
     * generates one.
     *
     * <p> To be sure that the file name used by the parent class has been
     * generated by the RollingPolicy and not specified by the user, we keep track
     * of the last generated name object and compare its reference to the parent
     * file name. If they match, then the RollingPolicy knows it's responsible for
     * the change of the file name.
     *
     */
    @Override
    public String getActiveFileName() {
        String parentsRawFileProperty = getParentsRawFileProperty();
        parentsRawFileProperty = parentsRawFileProperty + "." + lastFileSuffix;
        if (parentsRawFileProperty != null) {
            return parentsRawFileProperty;
        } else {
            return timeBasedFileNamingAndTriggeringPolicy.getCurrentPeriodsFileNameWithoutCompressionSuffix();
        }
    }

    @Override
    public boolean isTriggeringEvent(File activeFile, final E event) {
        return timeBasedFileNamingAndTriggeringPolicy.isTriggeringEvent(activeFile, event);
    }

    /**
     * Get the number of archive files to keep.
     *
     * @return number of archive files to keep
     */
    public int getMaxHistory() {
        return maxHistory;
    }

    /**
     * Set the maximum number of archive files to keep.
     *
     * @param maxHistory
     *                number of archive files to keep
     */
    public void setMaxHistory(int maxHistory) {
        this.maxHistory = maxHistory;
    }

    public boolean isCleanHistoryOnStart() {
        return cleanHistoryOnStart;
    }

    /**
     * Should archive removal be attempted on application start up? Default is false.
     * @since 1.0.1
     * @param cleanHistoryOnStart
     */
    public void setCleanHistoryOnStart(boolean cleanHistoryOnStart) {
        this.cleanHistoryOnStart = cleanHistoryOnStart;
    }

    @Override
    public String toString() {
        return "c.q.l.core.rolling.TimeBasedRollingPolicy@"+this.hashCode();
    }

    public void setTotalSizeCap(FileSize totalSizeCap) {
        addInfo("setting totalSizeCap to "+totalSizeCap.toString());
        this.totalSizeCap = totalSizeCap;
    }

    private long lastFileSuffix = 0;


    private void initCurrentSize(String filePath) {
    File file = new File(filePath).getParentFile();
    final String fileName = new File(filePath).getName();
    final String fileNameNoLogSuffix = fileName.replace(".log", "");
    File[] files = file.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String suffix) {
            if (suffix.indexOf(".") == -1) {
                return false;
            }
            String lastSize = suffix.substring(suffix.lastIndexOf('.') + 1);
            return suffix.startsWith(fileNameNoLogSuffix) && NumberUtils.isDigits(lastSize);
        }
    });
    if (ArrayUtils.isEmpty(files)) {
        lastFileSuffix = 0;
        return;
    }
    long maxSize = 0;
    for (File f : files) {
        if (!f.isFile()) {
            continue;
        }
        String fName = f.getName();
        String suffix = fName.substring(fName.lastIndexOf('.') + 1);
        Long size = Long.valueOf(StringUtils.trim(suffix));
        if (size > maxSize) {
            maxSize = size;
        }
    }
    this.lastFileSuffix = maxSize;
}
}
