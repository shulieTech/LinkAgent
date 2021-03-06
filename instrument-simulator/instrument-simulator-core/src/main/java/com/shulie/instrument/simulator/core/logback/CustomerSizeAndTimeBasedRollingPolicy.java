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

import ch.qos.logback.core.util.FileSize;

/**
 * @author angju
 * @date 2021/8/20 11:07
 */
public class CustomerSizeAndTimeBasedRollingPolicy <E> extends CustomerTimeBasedRollingPolicy<E> {

    FileSize maxFileSize;

    @Override
    public void start() {
        CustomerSizeAndTimeBasedFNATP<E> sizeAndTimeBasedFNATP = new CustomerSizeAndTimeBasedFNATP<E>(CustomerSizeAndTimeBasedFNATP.Usage.EMBEDDED);
        if(maxFileSize == null) {
            addError("maxFileSize property is mandatory.");
            return;
        } else {
            addInfo("Archive files will be limited to ["+maxFileSize+"] each.");
        }

        sizeAndTimeBasedFNATP.setMaxFileSize(maxFileSize);
        timeBasedFileNamingAndTriggeringPolicy = sizeAndTimeBasedFNATP;

        if(!isUnboundedTotalSizeCap() && totalSizeCap.getSize() < maxFileSize.getSize()) {
            addError("totalSizeCap of ["+totalSizeCap+"] is smaller than maxFileSize ["+maxFileSize+"] which is non-sensical");
            return;
        }

        // most work is done by the parent
        super.start();
    }


    public void setMaxFileSize(FileSize aMaxFileSize) {
        this.maxFileSize = aMaxFileSize;
    }

    @Override
    public String toString() {
        return "c.q.l.core.rolling.SizeAndTimeBasedRollingPolicy@"+this.hashCode();
    }
}
