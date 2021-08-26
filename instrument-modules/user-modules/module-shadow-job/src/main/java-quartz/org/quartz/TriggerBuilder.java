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
package org.quartz;

import java.util.Date;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/7/23 9:08 下午
 */
public class TriggerBuilder<T extends Trigger> {

    private TriggerKey key;
    private String description;
    private Date startTime = new Date();
    private Date endTime;
    private int priority = Trigger.DEFAULT_PRIORITY;
    private String calendarName;
    private JobKey jobKey;
    private JobDataMap jobDataMap = new JobDataMap();

    private ScheduleBuilder<?> scheduleBuilder = null;

    private TriggerBuilder() {

    }

    /**
     * Create a new TriggerBuilder with which to define a
     * specification for a Trigger.
     *
     * @return the new TriggerBuilder
     */
    public static TriggerBuilder<Trigger> newTrigger() {
        return new TriggerBuilder<Trigger>();
    }

    /**
     * Produce the <code>Trigger</code>.
     *
     * @return a Trigger that meets the specifications of the builder.
     */
    @SuppressWarnings("unchecked")
    public T build() {
        return (T) null;
    }

    /**
     * Use a <code>TriggerKey</code> with the given name and default group to
     * identify the Trigger.
     *
     * <p>If none of the 'withIdentity' methods are set on the TriggerBuilder,
     * then a random, unique TriggerKey will be generated.</p>
     *
     * @param name the name element for the Trigger's TriggerKey
     * @return the updated TriggerBuilder
     * @see TriggerKey
     * @see Trigger#getKey()
     */
    public TriggerBuilder<T> withIdentity(String name) {
        key = new TriggerKey(name, null);
        return this;
    }

    /**
     * Use a TriggerKey with the given name and group to
     * identify the Trigger.
     *
     * <p>If none of the 'withIdentity' methods are set on the TriggerBuilder,
     * then a random, unique TriggerKey will be generated.</p>
     *
     * @param name  the name element for the Trigger's TriggerKey
     * @param group the group element for the Trigger's TriggerKey
     * @return the updated TriggerBuilder
     * @see TriggerKey
     * @see Trigger#getKey()
     */
    public TriggerBuilder<T> withIdentity(String name, String group) {
        key = new TriggerKey(name, group);
        return this;
    }

    /**
     * Use the given TriggerKey to identify the Trigger.
     *
     * <p>If none of the 'withIdentity' methods are set on the TriggerBuilder,
     * then a random, unique TriggerKey will be generated.</p>
     *
     * @param triggerKey the TriggerKey for the Trigger to be built
     * @return the updated TriggerBuilder
     * @see TriggerKey
     * @see Trigger#getKey()
     */
    public TriggerBuilder<T> withIdentity(TriggerKey triggerKey) {
        this.key = triggerKey;
        return this;
    }

    /**
     * Set the given (human-meaningful) description of the Trigger.
     *
     * @param triggerDescription the description for the Trigger
     * @return the updated TriggerBuilder
     * @see Trigger#getDescription()
     */
    public TriggerBuilder<T> withDescription(String triggerDescription) {
        this.description = triggerDescription;
        return this;
    }

    /**
     * Set the Trigger's priority.  When more than one Trigger have the same
     * fire time, the scheduler will fire the one with the highest priority
     * first.
     *
     * @param triggerPriority the priority for the Trigger
     * @return the updated TriggerBuilder
     * @see Trigger#DEFAULT_PRIORITY
     * @see Trigger#getPriority()
     */
    public TriggerBuilder<T> withPriority(int triggerPriority) {
        this.priority = triggerPriority;
        return this;
    }

    /**
     * Set the name of the {@link Calendar} that should be applied to this
     * Trigger's schedule.
     *
     * @param calName the name of the Calendar to reference.
     * @return the updated TriggerBuilder
     * @see Calendar
     * @see Trigger#getCalendarName()
     */
    public TriggerBuilder<T> modifiedByCalendar(String calName) {
        this.calendarName = calName;
        return this;
    }

    public TriggerBuilder<T> startAt(Date triggerStartTime) {
        this.startTime = triggerStartTime;
        return this;
    }

    /**
     * Set the time the Trigger should start at to the current moment -
     * the trigger may or may not fire at this time - depending upon the
     * schedule configured for the Trigger.
     *
     * @return the updated TriggerBuilder
     * @see Trigger#getStartTime()
     */
    public TriggerBuilder<T> startNow() {
        this.startTime = new Date();
        return this;
    }

    public TriggerBuilder<T> endAt(Date triggerEndTime) {
        this.endTime = triggerEndTime;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <SBT extends T> TriggerBuilder<SBT> withSchedule(ScheduleBuilder<SBT> schedBuilder) {
        this.scheduleBuilder = schedBuilder;
        return (TriggerBuilder<SBT>) this;
    }

    /**
     * Set the identity of the Job which should be fired by the produced
     * Trigger.
     *
     * @param keyOfJobToFire the identity of the Job to fire.
     * @return the updated TriggerBuilder
     * @see Trigger#getJobKey()
     */
    public TriggerBuilder<T> forJob(JobKey keyOfJobToFire) {
        this.jobKey = keyOfJobToFire;
        return this;
    }

    /**
     * Set the identity of the Job which should be fired by the produced
     * Trigger - a <code>JobKey</code> will be produced with the given
     * name and default group.
     *
     * @param jobName the name of the job (in default group) to fire.
     * @return the updated TriggerBuilder
     * @see Trigger#getJobKey()
     */
    public TriggerBuilder<T> forJob(String jobName) {
        this.jobKey = new JobKey(jobName, null);
        return this;
    }

    /**
     * Set the identity of the Job which should be fired by the produced
     * Trigger - a <code>JobKey</code> will be produced with the given
     * name and group.
     *
     * @param jobName  the name of the job to fire.
     * @param jobGroup the group of the job to fire.
     * @return the updated TriggerBuilder
     * @see Trigger#getJobKey()
     */
    public TriggerBuilder<T> forJob(String jobName, String jobGroup) {
        this.jobKey = new JobKey(jobName, jobGroup);
        return this;
    }

    /**
     * Set the identity of the Job which should be fired by the produced
     * Trigger, by extracting the JobKey from the given job.
     *
     * @param jobDetail the Job to fire.
     * @return the updated TriggerBuilder
     * @see Trigger#getJobKey()
     */
    public TriggerBuilder<T> forJob(JobDetail jobDetail) {
        return this;
    }

    /**
     * Add the given key-value pair to the Trigger's {@link JobDataMap}.
     *
     * @return the updated TriggerBuilder
     * @see Trigger#getJobDataMap()
     */
    public TriggerBuilder<T> usingJobData(String dataKey, String value) {
        jobDataMap.put(dataKey, value);
        return this;
    }

    /**
     * Add the given key-value pair to the Trigger's {@link JobDataMap}.
     *
     * @return the updated TriggerBuilder
     * @see Trigger#getJobDataMap()
     */
    public TriggerBuilder<T> usingJobData(String dataKey, Integer value) {
        jobDataMap.put(dataKey, value);
        return this;
    }

    /**
     * Add the given key-value pair to the Trigger's {@link JobDataMap}.
     *
     * @return the updated TriggerBuilder
     * @see Trigger#getJobDataMap()
     */
    public TriggerBuilder<T> usingJobData(String dataKey, Long value) {
        jobDataMap.put(dataKey, value);
        return this;
    }

    /**
     * Add the given key-value pair to the Trigger's {@link JobDataMap}.
     *
     * @return the updated TriggerBuilder
     * @see Trigger#getJobDataMap()
     */
    public TriggerBuilder<T> usingJobData(String dataKey, Float value) {
        jobDataMap.put(dataKey, value);
        return this;
    }

    /**
     * Add the given key-value pair to the Trigger's {@link JobDataMap}.
     *
     * @return the updated TriggerBuilder
     * @see Trigger#getJobDataMap()
     */
    public TriggerBuilder<T> usingJobData(String dataKey, Double value) {
        jobDataMap.put(dataKey, value);
        return this;
    }

    /**
     * Add the given key-value pair to the Trigger's {@link JobDataMap}.
     *
     * @return the updated TriggerBuilder
     * @see Trigger#getJobDataMap()
     */
    public TriggerBuilder<T> usingJobData(String dataKey, Boolean value) {
        jobDataMap.put(dataKey, value);
        return this;
    }

    /**
     * Set the Trigger's {@link JobDataMap}, adding any values to it
     * that were already set on this TriggerBuilder using any of the
     * other 'usingJobData' methods.
     *
     * @return the updated TriggerBuilder
     * @see Trigger#getJobDataMap()
     */
    public TriggerBuilder<T> usingJobData(JobDataMap newJobDataMap) {
        return this;
    }

}
