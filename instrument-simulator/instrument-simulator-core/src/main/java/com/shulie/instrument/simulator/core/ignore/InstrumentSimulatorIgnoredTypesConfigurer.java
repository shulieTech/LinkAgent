package com.shulie.instrument.simulator.core.ignore;

import com.shulie.instrument.simulator.api.ignore.IgnoredTypesBuilder;
import com.shulie.instrument.simulator.api.ignore.IgnoredTypesConfigurer;

/**
 * 探针框架相关的配置
 */
public class InstrumentSimulatorIgnoredTypesConfigurer implements IgnoredTypesConfigurer {

    @Override
    public void configure(IgnoredTypesBuilder builder) {
        builder
                .ignoreClass("com.shulie.instrument.")
                .ignoreClass("com.pamirs.pradar.")
                .ignoreClass("com.pamirs.attach.")
                .ignoreClass("com.shulie.druid.")
                .ignoreClass("io.shulie.instrument.")
                .ignoreClass("oshi.");

        builder
                .ignoreClass("com.alibaba.fastjson2.")
                .ignoreClass("com.alibaba.fastjson.")
                .ignoreClass("org.apache.commons.")
                .ignoreClass("org.apache.curator.")
                .ignoreClass("org.apache.zookeeper.")
                .ignoreClass("com.alicp.jetcache.")
                .ignoreClass("com.fasterxml.jackson.")
                .ignoreClass("com.google.gson.")
                .ignoreClass("com.netflix.governator.")
                .ignoreClass("okio.");

        builder
                .ignoreClassLoader("com.shulie.instrument.simulator.");
    }

}
