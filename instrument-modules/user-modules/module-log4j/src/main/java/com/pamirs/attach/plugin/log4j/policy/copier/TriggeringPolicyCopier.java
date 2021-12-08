package com.pamirs.attach.plugin.log4j.policy.copier;

import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/12/07 5:47 下午
 */
public interface TriggeringPolicyCopier<T extends TriggeringPolicy> {

    T copy(T source);
}
