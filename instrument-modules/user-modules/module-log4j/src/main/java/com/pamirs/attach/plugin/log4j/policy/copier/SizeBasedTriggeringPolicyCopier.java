package com.pamirs.attach.plugin.log4j.policy.copier;

import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/12/07 5:57 下午
 */
public class SizeBasedTriggeringPolicyCopier implements TriggeringPolicyCopier<SizeBasedTriggeringPolicy> {
    @Override
    public SizeBasedTriggeringPolicy copy(SizeBasedTriggeringPolicy source) {
        return new SizeBasedTriggeringPolicy(source.getMaxFileSize()){};
    }
}
