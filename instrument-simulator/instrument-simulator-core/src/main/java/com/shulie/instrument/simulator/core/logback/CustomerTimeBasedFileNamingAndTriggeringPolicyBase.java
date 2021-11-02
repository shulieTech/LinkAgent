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
package com.shulie.instrument.simulator.core.logback;

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.rolling.helper.ArchiveRemover;
import ch.qos.logback.core.rolling.helper.DateTokenConverter;
import ch.qos.logback.core.rolling.helper.RollingCalendar;
import ch.qos.logback.core.spi.ContextAwareBase;
import com.shulie.instrument.simulator.core.util.CustomerReflectUtils;

import java.io.File;
import java.util.Date;
import java.util.Locale;

import static ch.qos.logback.core.CoreConstants.CODES_URL;

/**
 * @author angju
 * @date 2021/8/20 10:54
 */
public abstract class CustomerTimeBasedFileNamingAndTriggeringPolicyBase <E> extends ContextAwareBase implements CustomerTimeBasedFileNamingAndTriggeringPolicy<E> {

    static private String COLLIDING_DATE_FORMAT_URL = CODES_URL + "#rfa_collision_in_dateFormat";

    protected CustomerTimeBasedRollingPolicy<E> tbrp;

    protected ArchiveRemover archiveRemover = null;
    protected String elapsedPeriodsFileName;
    protected RollingCalendar rc;

    protected long artificialCurrentTime = -1;
    protected Date dateInCurrentPeriod = null;

    protected long nextCheck;
    protected boolean started = false;
    protected boolean errorFree = true;

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public void start() {
//        DateTokenConverter<Object> dtc = tbrp.fileNamePattern.getPrimaryDateTokenConverter();
        DateTokenConverter<Object> dtc = CustomerReflectUtils.getFileNamePattern(tbrp).getPrimaryDateTokenConverter();
        if (dtc == null) {
//            throw new IllegalStateException("FileNamePattern [" + tbrp.fileNamePattern.getPattern() + "] does not contain a valid DateToken");
            throw new IllegalStateException("FileNamePattern [" + CustomerReflectUtils.getFileNamePattern(tbrp).getPattern() + "] does not contain a valid DateToken");
        }

        if (dtc.getTimeZone() != null) {
            rc = new RollingCalendar(dtc.getDatePattern(), dtc.getTimeZone(), Locale.getDefault());
        } else {
            rc = new RollingCalendar(dtc.getDatePattern());
        }
//        addInfo("The date pattern is '" + dtc.getDatePattern() + "' from file name pattern '" + tbrp.fileNamePattern.getPattern() + "'.");
        addInfo("The date pattern is '" + dtc.getDatePattern() + "' from file name pattern '" + CustomerReflectUtils.getFileNamePattern(tbrp).getPattern() + "'.");
        rc.printPeriodicity(this);

        if (!rc.isCollisionFree()) {
            addError("The date format in FileNamePattern will result in collisions in the names of archived log files.");
            addError(CoreConstants.MORE_INFO_PREFIX + COLLIDING_DATE_FORMAT_URL);
            withErrors();
            return;
        }

        setDateInCurrentPeriod(new Date(getCurrentTime()));
        if (tbrp.getParentsRawFileProperty() != null) {
            File currentFile = new File(tbrp.getParentsRawFileProperty());
            if (currentFile.exists() && currentFile.canRead()) {
                setDateInCurrentPeriod(new Date(currentFile.lastModified()));
            }
        }
        addInfo("Setting initial period to " + dateInCurrentPeriod);
        computeNextCheck();
    }

    @Override
    public void stop() {
        started = false;
    }

    protected void computeNextCheck() {
        nextCheck = rc.getNextTriggeringDate(dateInCurrentPeriod).getTime();
    }

    protected void setDateInCurrentPeriod(long now) {
        dateInCurrentPeriod.setTime(now);
    }

    // allow Test classes to act on the dateInCurrentPeriod field to simulate old
    // log files needing rollover
    public void setDateInCurrentPeriod(Date _dateInCurrentPeriod) {
        this.dateInCurrentPeriod = _dateInCurrentPeriod;
    }

    @Override
    public String getElapsedPeriodsFileName() {
        return elapsedPeriodsFileName;
    }

    @Override
    public String getCurrentPeriodsFileNameWithoutCompressionSuffix() {
        return tbrp.fileNamePatternWithoutCompSuffix.convert(dateInCurrentPeriod);
    }

    @Override
    public void setCurrentTime(long timeInMillis) {
        artificialCurrentTime = timeInMillis;
    }

    @Override
    public long getCurrentTime() {
        // if time is forced return the time set by user
        if (artificialCurrentTime >= 0) {
            return artificialCurrentTime;
        } else {
            return System.currentTimeMillis();
        }
    }

    @Override
    public void setTimeBasedRollingPolicy(CustomerTimeBasedRollingPolicy<E> _tbrp) {
        this.tbrp = _tbrp;

    }

    @Override
    public ArchiveRemover getArchiveRemover() {
        return archiveRemover;
    }

    protected void withErrors() {
        errorFree = false;
    }

    protected boolean isErrorFree() {
        return errorFree;
    }

}
