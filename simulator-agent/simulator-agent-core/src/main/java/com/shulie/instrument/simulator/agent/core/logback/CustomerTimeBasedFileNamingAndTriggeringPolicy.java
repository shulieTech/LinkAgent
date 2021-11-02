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

import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.rolling.TriggeringPolicy;
import ch.qos.logback.core.rolling.helper.ArchiveRemover;
import ch.qos.logback.core.spi.ContextAware;

/**
 * @author angju
 * @date 2021/8/20 10:52
 */
public interface CustomerTimeBasedFileNamingAndTriggeringPolicy<E> extends TriggeringPolicy<E>, ContextAware {

    /**
     * Set the host/parent {@link TimeBasedRollingPolicy}.
     *
     * @param tbrp
     *                parent TimeBasedRollingPolicy
     */
    void setTimeBasedRollingPolicy(CustomerTimeBasedRollingPolicy<E> tbrp);

    /**
     * Return the file name for the elapsed periods file name.
     *
     * @return
     */
    String getElapsedPeriodsFileName();

    /**
     * Return the current periods file name without the compression suffix. This
     * value is equivalent to the active file name.
     *
     * @return current period's file name (without compression suffix)
     */
    String getCurrentPeriodsFileNameWithoutCompressionSuffix();

    /**
     * Return the archive remover appropriate for this instance.
     */
    ArchiveRemover getArchiveRemover();

    /**
     * Return the current time which is usually the value returned by
     * System.currentMillis(). However, for <b>testing</b> purposed this value
     * may be different than the real time.
     *
     * @return current time value
     */
    long getCurrentTime();

    /**
     * Set the current time. Only unit tests should invoke this method.
     *
     * @param now
     */
    void setCurrentTime(long now);

    /**
     * Set some date in the current period. Only unit tests should invoke this
     * method.
     *
     * WARNING: method removed. A unit test should not set the
     * date in current period. It is the job of the FNATP to compute that.
     *
     * @param date
     */
    // void setDateInCurrentPeriod(Date date);
}
