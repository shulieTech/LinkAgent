package com.shulie.instrument.simulator.core.ignore;

import com.shulie.instrument.simulator.api.ignore.IgnoredTypesBuilder;
import com.shulie.instrument.simulator.api.ignore.IgnoredTypesConfigurer;

public class AdditionalLibraryIgnoredTypesConfigurer implements IgnoredTypesConfigurer {

    // only used by tests (to bypass the ignores check)
    public void configure(IgnoredTypesBuilder builder) {

        builder.ignoreClass("oracle.jdbc.")
                .ignoreClass("com.mysql.")
                .ignoreClass("org.influxdb")
                .ignoreClass("com.microsoft.sqlserver.");

        builder.ignoreClass("com.navercorp.pinpoint.")
                .ignoreClass("org.apache.skywalking.")
                .ignoreClass("com.clickhouse.")
                .ignoreClass("ru.yandex.clickhouse.");

        builder.ignoreClass("com.baomidou.")
                .ignoreClass("com.github.xiaoymin.")
                .ignoreClass("com.google.protobuf.")
                .ignoreClass("com.netflix.curator.")
                .ignoreClass("com.sun.jersey.")
                .ignoreClass("org.dom4j.")
                .ignoreClass("springfox.")
                .ignoreClass("io.swagger.")
                .ignoreClass("org.apache.poi.")
                .ignoreClass("org.aspectj.")
                .ignoreClass("aj.org.objectweb.")
                .ignoreClass("bsh.")
                .ignoreClass("jodd.")
                .ignoreClass("org.codehaus.jackson.")
                .ignoreClass("com.sun.xml.")
                .ignoreClass("org.hamcrest")
                .ignoreClass("org.jboss.")
                .ignoreClass("org.kohsuke.")
                .ignoreClass("org.apache.ibatis.")
                .ignoreClass("javassist.")
                .ignoreClass("lombok.")
                .ignoreClass("org.pf4j.")
                .ignoreClass("org.apache.ibatis.");


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
