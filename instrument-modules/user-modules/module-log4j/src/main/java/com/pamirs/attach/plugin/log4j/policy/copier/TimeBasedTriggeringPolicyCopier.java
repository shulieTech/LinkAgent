package com.pamirs.attach.plugin.log4j.policy.copier;

import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/12/07 6:35 下午
 */
public class TimeBasedTriggeringPolicyCopier implements TriggeringPolicyCopier<TimeBasedTriggeringPolicy> {
    @Override
    public TimeBasedTriggeringPolicy copy(TimeBasedTriggeringPolicy source) {
        return TimeBasedTriggeringPolicy.createPolicy(source.getInterval() + "",
            Reflect.on(source).get("modulate").toString());
    }
}
