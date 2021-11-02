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

import ch.qos.logback.core.rolling.helper.FileFilterUtil;
import ch.qos.logback.core.rolling.helper.FileNamePattern;
import ch.qos.logback.core.rolling.helper.RollingCalendar;

import java.io.File;
import java.util.Date;

/**
 * @author angju
 * @date 2021/8/20 11:44
 */
public class CustomerSizeAndTimeBasedArchiveRemover extends CustomerTimeBasedArchiveRemover {

    public CustomerSizeAndTimeBasedArchiveRemover(FileNamePattern fileNamePattern, RollingCalendar rc) {
        super(fileNamePattern, rc);
    }

    @Override
    protected File[] getFilesInPeriod(Date dateOfPeriodToClean) {
        return new File[]{};
//        File archive0 = new File(fileNamePattern.convertMultipleArguments(dateOfPeriodToClean, 0));
//        File parentDir = getParentDir(archive0);
//        String stemRegex = createStemRegex(dateOfPeriodToClean);
//        File[] matchingFileArray = FileFilterUtil.filesInFolderMatchingStemRegex(parentDir, stemRegex);
//
//
//        return matchingFileArray;
    }




    private String createStemRegex(final Date dateOfPeriodToClean) {
        String regex = fileNamePattern.toRegexForFixedDate(dateOfPeriodToClean);
        return FileFilterUtil.afterLastSlash(regex);
    }

}
