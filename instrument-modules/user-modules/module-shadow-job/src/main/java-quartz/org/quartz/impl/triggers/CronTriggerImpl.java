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

import org.quartz.Calendar;
import org.quartz.CronTrigger;
import org.quartz.ScheduleBuilder;

import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/7/23 9:05 下午
 */
public class CronTriggerImpl extends AbstractTrigger {
    private static final long serialVersionUID = -8644953146451592766L;

    public CronTriggerImpl() {
        super();
        setStartTime(new Date());
    }

    /**
     * <p>
     * Create a <code>CronTrigger</code> with the given name and default group.
     * </p>
     *
     * <p>
     * The start-time will also be set to the current time, and the time zone
     * will be set the the system's default time zone.
     * </p>
     *
     * @deprecated use a TriggerBuilder instead
     */
    @Deprecated
    public CronTriggerImpl(String name) {
        this(name, null);
    }

    @Deprecated
    public CronTriggerImpl(String name, String group) {
    }

    @Deprecated
    public CronTriggerImpl(String name, String group, String cronExpression)
            throws ParseException {

    }

    @Deprecated
    public CronTriggerImpl(String name, String group, String jobName,
                           String jobGroup) {
    }

    @Override
    public void triggered(Calendar calendar) {

    }

    @Override
    public Date computeFirstFireTime(Calendar calendar) {
        return null;
    }

    @Override
    public boolean mayFireAgain() {
        return false;
    }

    @Override
    public Date getStartTime() {
        return null;
    }

    @Override
    public void setStartTime(Date startTime) {

    }

    @Override
    public void setEndTime(Date endTime) {

    }

    @Override
    public Date getEndTime() {
        return null;
    }

    @Override
    public Date getNextFireTime() {
        return null;
    }

    @Override
    public Date getPreviousFireTime() {
        return null;
    }

    @Override
    public Date getFireTimeAfter(Date afterTime) {
        return null;
    }

    @Override
    public Date getFinalFireTime() {
        return null;
    }

    @Override
    protected boolean validateMisfireInstruction(int candidateMisfireInstruction) {
        return false;
    }

    @Override
    public void updateAfterMisfire(Calendar cal) {

    }

    @Override
    public void updateWithNewCalendar(Calendar cal, long misfireThreshold) {

    }

    @Override
    public void setNextFireTime(Date nextFireTime) {

    }

    @Override
    public void setPreviousFireTime(Date previousFireTime) {

    }

    @Override
    public ScheduleBuilder<CronTrigger> getScheduleBuilder() {
        return null;
    }

    @Deprecated
    public CronTriggerImpl(String name, String group, String jobName,
                           String jobGroup, String cronExpression) throws ParseException {
    }

    @Deprecated
    public CronTriggerImpl(String name, String group, String jobName,
                           String jobGroup, String cronExpression, TimeZone timeZone)
            throws ParseException {
    }

    @Deprecated
    public CronTriggerImpl(String name, String group, String jobName,
                           String jobGroup, Date startTime, Date endTime, String cronExpression)
            throws ParseException {
    }

    @Deprecated
    public CronTriggerImpl(String name, String group, String jobName,
                           String jobGroup, Date startTime, Date endTime,
                           String cronExpression, TimeZone timeZone) throws ParseException {
    }


    public String getCronExpression() {
        return null;
    }

}
