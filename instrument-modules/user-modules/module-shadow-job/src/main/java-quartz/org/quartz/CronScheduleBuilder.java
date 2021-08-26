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

import org.quartz.spi.MutableTrigger;

import java.text.ParseException;
import java.util.TimeZone;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/7/23 9:10 下午
 */
public class CronScheduleBuilder extends ScheduleBuilder<CronTrigger> {

    private CronExpression cronExpression;
    private int misfireInstruction = CronTrigger.MISFIRE_INSTRUCTION_SMART_POLICY;

    protected CronScheduleBuilder(CronExpression cronExpression) {
        if (cronExpression == null) {
            throw new NullPointerException("cronExpression cannot be null");
        }
        this.cronExpression = cronExpression;
    }

    /**
     * Create a CronScheduleBuilder with the given cron-expression string -
     * which is presumed to b e valid cron expression (and hence only a
     * RuntimeException will be thrown if it is not).
     *
     * @param cronExpression the cron expression string to base the schedule on.
     * @return the new CronScheduleBuilder
     * @throws RuntimeException wrapping a ParseException if the expression is invalid
     * @see CronExpression
     */
    public static CronScheduleBuilder cronSchedule(String cronExpression) {
        try {
            return cronSchedule(new CronExpression(cronExpression));
        } catch (ParseException e) {
            // all methods of construction ensure the expression is valid by
            // this point...
            throw new RuntimeException("CronExpression '" + cronExpression
                    + "' is invalid.", e);
        }
    }

    /**
     * Create a CronScheduleBuilder with the given cron-expression string -
     * which may not be a valid cron expression (and hence a ParseException will
     * be thrown if it is not).
     *
     * @param cronExpression the cron expression string to base the schedule on.
     * @return the new CronScheduleBuilder
     * @throws ParseException if the expression is invalid
     * @see CronExpression
     */
    public static CronScheduleBuilder cronScheduleNonvalidatedExpression(
            String cronExpression) throws ParseException {
        return cronSchedule(new CronExpression(cronExpression));
    }

    private static CronScheduleBuilder cronScheduleNoParseException(
            String presumedValidCronExpression) {
        try {
            return cronSchedule(new CronExpression(presumedValidCronExpression));
        } catch (ParseException e) {
            // all methods of construction ensure the expression is valid by
            // this point...
            throw new RuntimeException(
                    "CronExpression '"
                            + presumedValidCronExpression
                            + "' is invalid, which should not be possible, please report bug to Quartz developers.",
                    e);
        }
    }

    /**
     * Create a CronScheduleBuilder with the given cron-expression.
     *
     * @param cronExpression the cron expression to base the schedule on.
     * @return the new CronScheduleBuilder
     * @see CronExpression
     */
    public static CronScheduleBuilder cronSchedule(CronExpression cronExpression) {
        return new CronScheduleBuilder(cronExpression);
    }

    /**
     * Create a CronScheduleBuilder with a cron-expression that sets the
     * schedule to fire every day at the given time (hour and minute).
     *
     * @param hour   the hour of day to fire
     * @param minute the minute of the given hour to fire
     * @return the new CronScheduleBuilder
     * @see CronExpression
     */
    public static CronScheduleBuilder dailyAtHourAndMinute(int hour, int minute) {
        return null;
    }

    /**
     * Create a CronScheduleBuilder with a cron-expression that sets the
     * schedule to fire at the given day at the given time (hour and minute) on
     * the given days of the week.
     *
     * @param daysOfWeek the dasy of the week to fire
     * @param hour       the hour of day to fire
     * @param minute     the minute of the given hour to fire
     * @return the new CronScheduleBuilder
     * @see CronExpression
     */

    public static CronScheduleBuilder atHourAndMinuteOnGivenDaysOfWeek(
            int hour, int minute, Integer... daysOfWeek) {
        return null;
    }

    /**
     * Create a CronScheduleBuilder with a cron-expression that sets the
     * schedule to fire one per week on the given day at the given time (hour
     * and minute).
     *
     * @param dayOfWeek the day of the week to fire
     * @param hour      the hour of day to fire
     * @param minute    the minute of the given hour to fire
     * @return the new CronScheduleBuilder
     * @see CronExpression
     */
    public static CronScheduleBuilder weeklyOnDayAndHourAndMinute(
            int dayOfWeek, int hour, int minute) {
        return null;
    }

    /**
     * Create a CronScheduleBuilder with a cron-expression that sets the
     * schedule to fire one per month on the given day of month at the given
     * time (hour and minute).
     *
     * @param dayOfMonth the day of the month to fire
     * @param hour       the hour of day to fire
     * @param minute     the minute of the given hour to fire
     * @return the new CronScheduleBuilder
     * @see CronExpression
     */
    public static CronScheduleBuilder monthlyOnDayAndHourAndMinute(
            int dayOfMonth, int hour, int minute) {
        return null;
    }

    /**
     * The <code>TimeZone</code> in which to base the schedule.
     *
     * @param timezone the time-zone for the schedule.
     * @return the updated CronScheduleBuilder
     * @see CronExpression#getTimeZone()
     */
    public CronScheduleBuilder inTimeZone(TimeZone timezone) {
        cronExpression.setTimeZone(timezone);
        return this;
    }

    /**
     * If the Trigger misfires, use the
     * {@link Trigger#MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY} instruction.
     *
     * @return the updated CronScheduleBuilder
     * @see Trigger#MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY
     */
    public CronScheduleBuilder withMisfireHandlingInstructionIgnoreMisfires() {
        misfireInstruction = Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY;
        return this;
    }

    /**
     * If the Trigger misfires, use the
     * {@link CronTrigger#MISFIRE_INSTRUCTION_DO_NOTHING} instruction.
     *
     * @return the updated CronScheduleBuilder
     * @see CronTrigger#MISFIRE_INSTRUCTION_DO_NOTHING
     */
    public CronScheduleBuilder withMisfireHandlingInstructionDoNothing() {
        misfireInstruction = CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING;
        return this;
    }

    /**
     * If the Trigger misfires, use the
     * {@link CronTrigger#MISFIRE_INSTRUCTION_FIRE_ONCE_NOW} instruction.
     *
     * @return the updated CronScheduleBuilder
     * @see CronTrigger#MISFIRE_INSTRUCTION_FIRE_ONCE_NOW
     */
    public CronScheduleBuilder withMisfireHandlingInstructionFireAndProceed() {
        misfireInstruction = CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
        return this;
    }

    @Override
    protected MutableTrigger build() {
        return null;
    }
}
