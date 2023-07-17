package com.shulie.instrument.simulator.core.ignore;

import com.shulie.instrument.simulator.api.ignore.IgnoredTypesBuilder;
import com.shulie.instrument.simulator.api.ignore.IgnoredTypesConfigurer;

/**
 * 探针框架相关的配置
 */
public class InstrumentSimulatorTypesConfigurer implements IgnoredTypesConfigurer {

    @Override
    public void configure(IgnoredTypesBuilder builder) {
        builder
                .ignoreClass("com.shulie.instrument.")
                .ignoreClass("com.pamirs.pradar.")
                .ignoreClass("com.pamirs.attach.")
                .ignoreClass("com.shulie.druid.")
                .ignoreClass("oshi.");

        builder
                .ignoreClassLoader("com.shulie.instrument.simulator.");
    }

}
