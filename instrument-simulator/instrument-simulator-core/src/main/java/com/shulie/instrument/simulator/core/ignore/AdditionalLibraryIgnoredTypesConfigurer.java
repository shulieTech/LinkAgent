package com.shulie.instrument.simulator.core.ignore;

import com.shulie.instrument.simulator.api.ignore.IgnoredTypesBuilder;
import com.shulie.instrument.simulator.api.ignore.IgnoredTypesConfigurer;

public class AdditionalLibraryIgnoredTypesConfigurer implements IgnoredTypesConfigurer {

    // only used by tests (to bypass the ignores check)
    public void configure(IgnoredTypesBuilder builder) {
        builder
                .ignoreClass("com.beust.jcommander.")
                .ignoreClass("com.fasterxml.classmate.")
                .ignoreClass("com.github.mustachejava.")
                .ignoreClass("com.jayway.jsonpath.")
                .ignoreClass("com.lightbend.lagom.")
                .ignoreClass("javax.el.")
                .ignoreClass("org.apache.lucene.")
                .ignoreClass("org.apache.tartarus.")
                .ignoreClass("org.json.simple.")
                .ignoreClass("org.yaml.snakeyaml.");

        builder.ignoreClass("net.sf.cglib.").allowClass("net.sf.cglib.core.internal.LoadingCache$2");

        builder
                .ignoreClass("org.springframework.");

        // xml-apis, xerces, xalan, but not xml web-services
        builder
                .ignoreClass("javax.xml.")
                .allowClass("javax.xml.ws.")
                .ignoreClass("org.apache.bcel.")
                .ignoreClass("org.apache.html.")
                .ignoreClass("org.apache.regexp.")
                .ignoreClass("org.apache.wml.")
                .ignoreClass("org.apache.xalan.")
                .ignoreClass("org.apache.xerces.")
                .ignoreClass("org.apache.xml.")
                .ignoreClass("org.apache.xpath.")
                .ignoreClass("org.xml.");

        builder
                .ignoreClass("com.couchbase.client.deps.");

        builder
                .ignoreClass("com.google.cloud.")
                .ignoreClass("com.google.instrumentation.")
                .ignoreClass("com.google.j2objc.")
                .ignoreClass("com.google.gson.")
                .ignoreClass("com.google.logging.")
                .ignoreClass("com.google.longrunning.")
                .ignoreClass("com.google.protobuf.")
                .ignoreClass("com.google.rpc.")
                .ignoreClass("com.google.thirdparty.")
                .ignoreClass("com.google.type.");

        builder
                .ignoreClass("com.google.common.");

        builder
                .ignoreClass("com.google.inject.");

        builder.ignoreClass("com.google.api.");

        builder
                .ignoreClass("org.h2.");

        builder
                .ignoreClass("com.carrotsearch.hppc.");

        builder
                .ignoreClass("com.fasterxml.jackson.");

        // kotlin, note we do not ignore kotlinx because we instrument coroutines code
        builder.ignoreClass("kotlin.");
    }
}
