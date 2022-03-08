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
package com.shulie.instrument.simulator.agent.spi.config;

import java.util.concurrent.TimeUnit;

/**
 * 调度参数
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/5/22 12:29 下午
 */
public class SchedulerArgs {

    /**
     * 延迟调度时间,如果 delay 小于等于0则不需要延迟
     */
    private int delay = 0;

    /**
     * 延迟调度单位
     */
    private TimeUnit delayUnit = TimeUnit.SECONDS;

    /**
     * 调度间隔时间
     */
    private int interval = 10;

    /**
     * 调试间隔单位
     */
    private TimeUnit intervalUnit = TimeUnit.SECONDS;

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public TimeUnit getDelayUnit() {
        return delayUnit;
    }

    public void setDelayUnit(TimeUnit delayUnit) {
        this.delayUnit = delayUnit;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public TimeUnit getIntervalUnit() {
        return intervalUnit;
    }

    public void setIntervalUnit(TimeUnit intervalUnit) {
        this.intervalUnit = intervalUnit;
    }
}
