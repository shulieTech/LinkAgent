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
import org.quartz.ScheduleBuilder;
import org.quartz.SimpleTrigger;

import java.util.Date;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/7/23 9:09 下午
 */
public class SimpleTriggerImpl extends AbstractTrigger<SimpleTrigger> {

    private static final long serialVersionUID = -3735980074222850397L;

    public SimpleTriggerImpl() {
        super();
    }

    @Deprecated
    public SimpleTriggerImpl(String name) {
        this(name, (String) null);
    }

    @Deprecated
    public SimpleTriggerImpl(String name, String group) {
        this(name, group, new Date(), null, 0, 0);
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
    public ScheduleBuilder<SimpleTrigger> getScheduleBuilder() {
        return null;
    }

    @Deprecated
    public SimpleTriggerImpl(String name, int repeatCount, long repeatInterval) {
        this(name, null, repeatCount, repeatInterval);
    }

    @Deprecated
    public SimpleTriggerImpl(String name, String group, int repeatCount,
                             long repeatInterval) {
        this(name, group, new Date(), null, repeatCount, repeatInterval);
    }

    @Deprecated
    public SimpleTriggerImpl(String name, Date startTime) {
        this(name, null, startTime);
    }

    @Deprecated
    public SimpleTriggerImpl(String name, String group, Date startTime) {
        this(name, group, startTime, null, 0, 0);
    }

    @Deprecated
    public SimpleTriggerImpl(String name, Date startTime,
                             Date endTime, int repeatCount, long repeatInterval) {
        this(name, null, startTime, endTime, repeatCount, repeatInterval);
    }

    @Deprecated
    public SimpleTriggerImpl(String name, String group, Date startTime,
                             Date endTime, int repeatCount, long repeatInterval) {
    }

    @Deprecated
    public SimpleTriggerImpl(String name, String group, String jobName,
                             String jobGroup, Date startTime, Date endTime, int repeatCount,
                             long repeatInterval) {
    }

    public int getRepeatCount() {
        return -1;
    }

    public int getRepeatInterval() {
        return -1;
    }
}
