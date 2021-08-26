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
package org.quartz.spi;

import org.quartz.*;

import java.util.Date;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/7/23 9:12 下午
 */
public interface MutableTrigger extends Trigger {

    public void setKey(TriggerKey key);

    public void setJobKey(JobKey key);

    /**
     * <p>
     * Set a description for the <code>Trigger</code> instance - may be
     * useful for remembering/displaying the purpose of the trigger, though the
     * description has no meaning to Quartz.
     * </p>
     */
    public void setDescription(String description);

    /**
     * <p>
     * Associate the <code>{@link Calendar}</code> with the given name with
     * this Trigger.
     * </p>
     *
     * @param calendarName use <code>null</code> to dis-associate a Calendar.
     */
    public void setCalendarName(String calendarName);

    /**
     * <p>
     * Set the <code>JobDataMap</code> to be associated with the
     * <code>Trigger</code>.
     * </p>
     */
    public void setJobDataMap(JobDataMap jobDataMap);

    /**
     * The priority of a <code>Trigger</code> acts as a tie breaker such that if
     * two <code>Trigger</code>s have the same scheduled fire time, then Quartz
     * will do its best to give the one with the higher priority first access
     * to a worker thread.
     *
     * <p>
     * If not explicitly set, the default value is <code>5</code>.
     * </p>
     *
     * @see #DEFAULT_PRIORITY
     */
    public void setPriority(int priority);

    /**
     * <p>
     * The time at which the trigger's scheduling should start.  May or may not
     * be the first actual fire time of the trigger, depending upon the type of
     * trigger and the settings of the other properties of the trigger.  However
     * the first actual first time will not be before this date.
     * </p>
     * <p>
     * Setting a value in the past may cause a new trigger to compute a first
     * fire time that is in the past, which may cause an immediate misfire
     * of the trigger.
     * </p>
     */
    public void setStartTime(Date startTime);

    public void setEndTime(Date endTime);

    public void setMisfireInstruction(int misfireInstruction);


    public Object clone();

}
