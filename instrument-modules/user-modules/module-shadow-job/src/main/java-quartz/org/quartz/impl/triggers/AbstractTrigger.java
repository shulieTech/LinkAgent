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
package org.quartz.impl.triggers;

import org.quartz.*;
import org.quartz.spi.OperableTrigger;

import java.util.Date;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/7/23 9:06 下午
 */
public abstract class AbstractTrigger<T extends Trigger> implements OperableTrigger {

    private static final long serialVersionUID = -3904243490805975570L;

    public AbstractTrigger() {
    }

    /**
     * <p>
     * Create a <code>Trigger</code> with the given name, and default group.
     * </p>
     *
     * <p>
     * Note that the {@link #setJobName(String)}and
     * {@link #setJobGroup(String)}methods must be called before the <code>Trigger</code>
     * can be placed into a {@link Scheduler}.
     * </p>
     *
     * @throws IllegalArgumentException if name is null or empty, or the group is an empty string.
     */
    public AbstractTrigger(String name) {
    }

    /**
     * <p>
     * Create a <code>Trigger</code> with the given name, and group.
     * </p>
     *
     * <p>
     * Note that the {@link #setJobName(String)}and
     * {@link #setJobGroup(String)}methods must be called before the <code>Trigger</code>
     * can be placed into a {@link Scheduler}.
     * </p>
     *
     * @param group if <code>null</code>, Scheduler.DEFAULT_GROUP will be used.
     * @throws IllegalArgumentException if name is null or empty, or the group is an empty string.
     */
    public AbstractTrigger(String name, String group) {
    }

    /**
     * <p>
     * Create a <code>Trigger</code> with the given name, and group.
     * </p>
     *
     * @param group if <code>null</code>, Scheduler.DEFAULT_GROUP will be used.
     * @throws IllegalArgumentException if name is null or empty, or the group is an empty string.
     */
    public AbstractTrigger(String name, String group, String jobName, String jobGroup) {
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Interface.
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Get the name of this <code>Trigger</code>.
     * </p>
     */
    public String getName() {
        return null;
    }

    /**
     * <p>
     * Set the name of this <code>Trigger</code>.
     * </p>
     *
     * @throws IllegalArgumentException if name is null or empty.
     */
    public void setName(String name) {
    }

    /**
     * <p>
     * Get the group of this <code>Trigger</code>.
     * </p>
     */
    public String getGroup() {
        return null;
    }

    /**
     * <p>
     * Set the name of this <code>Trigger</code>.
     * </p>
     *
     * @param group if <code>null</code>, Scheduler.DEFAULT_GROUP will be used.
     * @throws IllegalArgumentException if group is an empty string.
     */
    public void setGroup(String group) {
    }

    @Override
    public void setKey(TriggerKey key) {
    }

    /**
     * <p>
     * Get the name of the associated <code>{@link org.quartz.JobDetail}</code>.
     * </p>
     */
    public String getJobName() {
        return null;
    }

    /**
     * <p>
     * Set the name of the associated <code>{@link org.quartz.JobDetail}</code>.
     * </p>
     *
     * @throws IllegalArgumentException if jobName is null or empty.
     */
    public void setJobName(String jobName) {
    }

    /**
     * <p>
     * Get the name of the associated <code>{@link org.quartz.JobDetail}</code>'s
     * group.
     * </p>
     */
    public String getJobGroup() {
        return null;
    }

    /**
     * <p>
     * Set the name of the associated <code>{@link org.quartz.JobDetail}</code>'s
     * group.
     * </p>
     *
     * @param jobGroup if <code>null</code>, Scheduler.DEFAULT_GROUP will be used.
     * @throws IllegalArgumentException if group is an empty string.
     */
    public void setJobGroup(String jobGroup) {
    }

    @Override
    public void setJobKey(JobKey key) {
    }


    /**
     * <p>
     * Returns the 'full name' of the <code>Trigger</code> in the format
     * "group.name".
     * </p>
     */
    public String getFullName() {
        return null;
    }

    @Override
    public TriggerKey getKey() {
        return null;
    }

    @Override
    public JobKey getJobKey() {
        return null;
    }

    /**
     * <p>
     * Returns the 'full name' of the <code>Job</code> that the <code>Trigger</code>
     * points to, in the format "group.name".
     * </p>
     */
    public String getFullJobName() {
        return null;
    }

    /**
     * <p>
     * Return the description given to the <code>Trigger</code> instance by
     * its creator (if any).
     * </p>
     *
     * @return null if no description was set.
     */
    @Override
    public String getDescription() {
        return null;
    }

    /**
     * <p>
     * Set a description for the <code>Trigger</code> instance - may be
     * useful for remembering/displaying the purpose of the trigger, though the
     * description has no meaning to Quartz.
     * </p>
     */
    @Override
    public void setDescription(String description) {
    }

    /**
     * <p>
     * Associate the <code>{@link Calendar}</code> with the given name with
     * this Trigger.
     * </p>
     *
     * @param calendarName use <code>null</code> to dis-associate a Calendar.
     */
    @Override
    public void setCalendarName(String calendarName) {
    }

    /**
     * <p>
     * Get the name of the <code>{@link Calendar}</code> associated with this
     * Trigger.
     * </p>
     *
     * @return <code>null</code> if there is no associated Calendar.
     */
    @Override
    public String getCalendarName() {
        return null;
    }

    @Override
    public JobDataMap getJobDataMap() {
        return null;
    }

    @Override
    public void setJobDataMap(JobDataMap jobDataMap) {
    }

    @Override
    public int getPriority() {
        return -1;
    }

    @Override
    public void setPriority(int priority) {
    }

    @Override
    public abstract void triggered(Calendar calendar);

    @Override
    public abstract Date computeFirstFireTime(Calendar calendar);

    @Override
    public CompletedExecutionInstruction executionComplete(JobExecutionContext context,
                                                           JobExecutionException result) {
        return CompletedExecutionInstruction.NOOP;
    }

    @Override
    public abstract boolean mayFireAgain();

    @Override
    public abstract Date getStartTime();

    @Override
    public abstract void setStartTime(Date startTime);

    @Override
    public abstract void setEndTime(Date endTime);

    @Override
    public abstract Date getEndTime();

    @Override
    public abstract Date getNextFireTime();

    /**
     * <p>
     * Returns the previous time at which the <code>Trigger</code> fired.
     * If the trigger has not yet fired, <code>null</code> will be returned.
     */
    @Override
    public abstract Date getPreviousFireTime();

    /**
     * <p>
     * Returns the next time at which the <code>Trigger</code> will fire,
     * after the given time. If the trigger will not fire after the given time,
     * <code>null</code> will be returned.
     * </p>
     */
    @Override
    public abstract Date getFireTimeAfter(Date afterTime);

    /**
     * <p>
     * Returns the last time at which the <code>Trigger</code> will fire, if
     * the Trigger will repeat indefinitely, null will be returned.
     * </p>
     *
     * <p>
     * Note that the return time *may* be in the past.
     * </p>
     */
    @Override
    public abstract Date getFinalFireTime();

    /**
     * <p>
     * Set the instruction the <code>Scheduler</code> should be given for
     * handling misfire situations for this <code>Trigger</code>- the
     * concrete <code>Trigger</code> type that you are using will have
     * defined a set of additional <code>MISFIRE_INSTRUCTION_XXX</code>
     * constants that may be passed to this method.
     * </p>
     *
     * <p>
     * If not explicitly set, the default value is <code>MISFIRE_INSTRUCTION_SMART_POLICY</code>.
     * </p>
     *
     * @see #MISFIRE_INSTRUCTION_SMART_POLICY
     * @see #updateAfterMisfire(Calendar)
     * @see SimpleTrigger
     * @see CronTrigger
     */
    @Override
    public void setMisfireInstruction(int misfireInstruction) {
    }

    protected abstract boolean validateMisfireInstruction(int candidateMisfireInstruction);

    /**
     * <p>
     * Get the instruction the <code>Scheduler</code> should be given for
     * handling misfire situations for this <code>Trigger</code>- the
     * concrete <code>Trigger</code> type that you are using will have
     * defined a set of additional <code>MISFIRE_INSTRUCTION_XXX</code>
     * constants that may be passed to this method.
     * </p>
     *
     * <p>
     * If not explicitly set, the default value is <code>MISFIRE_INSTRUCTION_SMART_POLICY</code>.
     * </p>
     *
     * @see #MISFIRE_INSTRUCTION_SMART_POLICY
     * @see #updateAfterMisfire(Calendar)
     * @see SimpleTrigger
     * @see CronTrigger
     */
    @Override
    public int getMisfireInstruction() {
        return -1;
    }

    /**
     * <p>
     * This method should not be used by the Quartz client.
     * </p>
     *
     * <p>
     * To be implemented by the concrete classes that extend this class.
     * </p>
     *
     * <p>
     * The implementation should update the <code>Trigger</code>'s state
     * based on the MISFIRE_INSTRUCTION_XXX that was selected when the <code>Trigger</code>
     * was created.
     * </p>
     */
    @Override
    public abstract void updateAfterMisfire(Calendar cal);

    /**
     * <p>
     * This method should not be used by the Quartz client.
     * </p>
     *
     * <p>
     * To be implemented by the concrete class.
     * </p>
     *
     * <p>
     * The implementation should update the <code>Trigger</code>'s state
     * based on the given new version of the associated <code>Calendar</code>
     * (the state should be updated so that it's next fire time is appropriate
     * given the Calendar's new settings).
     * </p>
     *
     * @param cal the modifying calendar
     */
    @Override
    public abstract void updateWithNewCalendar(Calendar cal, long misfireThreshold);

    /**
     * <p>
     * Validates whether the properties of the <code>JobDetail</code> are
     * valid for submission into a <code>Scheduler</code>.
     *
     * @throws IllegalStateException if a required property (such as Name, Group, Class) is not
     *                               set.
     */
    @Override
    public void validate() throws SchedulerException {
    }

    /**
     * <p>
     * This method should not be used by the Quartz client.
     * </p>
     *
     * <p>
     * Usable by <code>{@link org.quartz.spi.JobStore}</code>
     * implementations, in order to facilitate 'recognizing' instances of fired
     * <code>Trigger</code> s as their jobs complete execution.
     * </p>
     */
    @Override
    public void setFireInstanceId(String id) {
    }

    /**
     * <p>
     * This method should not be used by the Quartz client.
     * </p>
     */
    @Override
    public String getFireInstanceId() {
        return null;
    }

    /**
     * <p>
     * Return a simple string representation of this object.
     * </p>
     */
    @Override
    public String toString() {
        return "Trigger '" + getFullName() + "':  triggerClass: '"
                + getClass().getName() + " calendar: '" + getCalendarName()
                + "' misfireInstruction: " + getMisfireInstruction()
                + " nextFireTime: " + getNextFireTime();
    }

    /**
     * <p>
     * Compare the next fire time of this <code>Trigger</code> to that of
     * another by comparing their keys, or in other words, sorts them
     * according to the natural (i.e. alphabetical) order of their keys.
     * </p>
     */
    @Override
    public int compareTo(Trigger other) {
        return 0;
    }

    /**
     * Trigger equality is based upon the equality of the TriggerKey.
     *
     * @return true if the key of this Trigger equals that of the given Trigger.
     */
    @Override
    public boolean equals(Object o) {
        return true;
    }


    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public Object clone() {
        return null;
    }

    @Override
    public TriggerBuilder<T> getTriggerBuilder() {
        return null;
    }

    @Override
    public abstract ScheduleBuilder<T> getScheduleBuilder();
}
