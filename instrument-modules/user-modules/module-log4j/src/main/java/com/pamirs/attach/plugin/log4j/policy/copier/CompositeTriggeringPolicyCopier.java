package com.pamirs.attach.plugin.log4j.policy.copier;

import com.pamirs.attach.plugin.log4j.policy.TriggeringPolicies;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/12/07 5:40 下午
 */
public class CompositeTriggeringPolicyCopier implements TriggeringPolicyCopier<CompositeTriggeringPolicy> {

    @Override
    public CompositeTriggeringPolicy copy(CompositeTriggeringPolicy source) {
        TriggeringPolicy[] sources = source.getTriggeringPolicies();
        TriggeringPolicy[] result = new TriggeringPolicy[sources.length];
        for (int i = 0; i < sources.length; i++) {
            result[i] = TriggeringPolicies.copy(sources[i]);
        }
        return CompositeTriggeringPolicy.createPolicy(result);
    }
}
