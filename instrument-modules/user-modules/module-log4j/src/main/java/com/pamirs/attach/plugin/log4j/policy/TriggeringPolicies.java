package com.pamirs.attach.plugin.log4j.policy;

import java.util.HashMap;
import java.util.Map;

import com.pamirs.attach.plugin.log4j.policy.copier.CompositeTriggeringPolicyCopier;
import com.pamirs.attach.plugin.log4j.policy.copier.SizeBasedTriggeringPolicyCopier;
import com.pamirs.attach.plugin.log4j.policy.copier.TimeBasedTriggeringPolicyCopier;
import com.pamirs.attach.plugin.log4j.policy.copier.TriggeringPolicyCopier;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/12/07 5:32 下午
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class TriggeringPolicies {

    private static final Map<Class<?>, TriggeringPolicyCopier<?>> map = new HashMap<>();

    public static <T extends TriggeringPolicy> T copy(T source) {
        TriggeringPolicyCopier<T> policyCopier = (TriggeringPolicyCopier<T>)map.get(source.getClass());
        if (policyCopier == null) {
            return null;
        }
        return policyCopier.copy(source);
    }

    public interface TriggeringPolicyCopierBuilder<T extends TriggeringPolicy> {
        TriggeringPolicyCopier<T> build();
    }

    public static <T extends TriggeringPolicy> void registerTriggeringPolicyCopier(
        Class<T> policyClass, TriggeringPolicyCopier<T> policyCopier) {
        map.put(policyClass, policyCopier);
    }

    static {
        for (TriggeringPolicyCopiers value : TriggeringPolicyCopiers.values()) {
            registerTriggeringPolicyCopier(value.matchClass, value.build());
        }
    }

    private enum TriggeringPolicyCopiers implements TriggeringPolicyCopierBuilder {

        CompositeTriggeringPolicy(CompositeTriggeringPolicy.class) {

            private final TriggeringPolicyCopier<CompositeTriggeringPolicy> instance
                = new CompositeTriggeringPolicyCopier();

            @Override
            public TriggeringPolicyCopier<CompositeTriggeringPolicy> build() {
                return instance;
            }
        },

        SizeBasedTriggeringPolicy(SizeBasedTriggeringPolicy.class) {

            private final TriggeringPolicyCopier<SizeBasedTriggeringPolicy> instance = new SizeBasedTriggeringPolicyCopier();

            @Override
            public TriggeringPolicyCopier<SizeBasedTriggeringPolicy> build() {
                return instance;
            }
        },

        TimeBasedTriggeringPolicy(TimeBasedTriggeringPolicy.class) {

            private final TriggeringPolicyCopier<TimeBasedTriggeringPolicy> instance = new TimeBasedTriggeringPolicyCopier();

            @Override
            public TriggeringPolicyCopier<TimeBasedTriggeringPolicy> build() {
                return instance;
            }
        },
        ;

        private final Class<? extends TriggeringPolicy> matchClass;

        TriggeringPolicyCopiers(Class<? extends TriggeringPolicy> matchClass) {this.matchClass = matchClass;}

    }
}
