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


import com.shulie.instrument.simulator.agent.core.util.CustomerReflectUtils;

import java.io.File;
import java.util.Date;

/**
 * @author angju
 * @date 2021/8/20 11:00
 */
public class CustomerDefaultTimeBasedFileNamingAndTriggeringPolicy <E> extends CustomerTimeBasedFileNamingAndTriggeringPolicyBase<E> {

    @Override
    public void start() {
        super.start();
        if (!super.isErrorFree())
            return;
//        if(tbrp.fileNamePattern.hasIntegerTokenCOnverter()) {
        if(CustomerReflectUtils.getFileNamePattern(tbrp).hasIntegerTokenCOnverter()) {
//            addError("Filename pattern ["+tbrp.fileNamePattern+"] contains an integer token converter, i.e. %i, INCOMPATIBLE with this configuration. Remove it.");
            addError("Filename pattern ["+CustomerReflectUtils.getFileNamePattern(tbrp)+"] contains an integer token converter, i.e. %i, INCOMPATIBLE with this configuration. Remove it.");
            return;
        }

//        archiveRemover = new TimeBasedArchiveRemover(tbrp.fileNamePattern, rc);
        archiveRemover = new CustomerTimeBasedArchiveRemover(CustomerReflectUtils.getFileNamePattern(tbrp), rc);
        archiveRemover.setContext(context);
        started = true;
    }

    @Override
    public boolean isTriggeringEvent(File activeFile, final E event) {
        long time = getCurrentTime();
        if (time >= nextCheck) {
            Date dateOfElapsedPeriod = dateInCurrentPeriod;
            addInfo("Elapsed period: " + dateOfElapsedPeriod);
            elapsedPeriodsFileName = tbrp.fileNamePatternWithoutCompSuffix.convert(dateOfElapsedPeriod);
            setDateInCurrentPeriod(time);
            computeNextCheck();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "c.q.l.core.rolling.DefaultTimeBasedFileNamingAndTriggeringPolicy";
    }
}
