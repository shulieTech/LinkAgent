package com.shulie.instrument.simulator.agent.core.logback.spi;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.LevelFilter;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.util.FileSize;
import com.shulie.instrument.simulator.agent.core.logback.CustomerRollingFileAppender;
import com.shulie.instrument.simulator.agent.core.logback.CustomerSizeAndTimeBasedRollingPolicy;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.Charset;

public class SimulatorAgentLogbackConfigurator extends ContextAwareBase implements ch.qos.logback.classic.spi.Configurator {

    private String SIMULATOR_LOG_PATH = System.getProperty("SIMULATOR_LOG_PATH");
    private String SIMULATOR_LOG_LEVEL = System.getProperty("SIMULATOR_LOG_LEVEL");

    @Override
    public void configure(LoggerContext lc) {
        try {
            Field rootField = LoggerContext.class.getDeclaredField("root");
            rootField.setAccessible(true);
            Logger root = (Logger) rootField.get(lc);
            root.setLevel(Level.valueOf(SIMULATOR_LOG_LEVEL));
        } catch (Exception e) {
        }
        addSimulatorAgentAppender(lc);
        addSimulatorAgentErrorAppender(lc);
    }

    private void addSimulatorAgentAppender(LoggerContext lc) {
        RollingFileAppender appender = new RollingFileAppender();
        appender.setName("SIMULATOR-AGENT-FILE-APPENDER");

        LevelFilter filter = new LevelFilter();
        filter.setLevel(Level.ERROR);
        filter.setOnMatch(FilterReply.DENY);
        filter.setOnMismatch(FilterReply.ACCEPT);
        filter.start();
        appender.addFilter(filter);

        appender.setFile(SIMULATOR_LOG_PATH + File.separator + "simulator-agent.log");

        SizeAndTimeBasedRollingPolicy policy = new SizeAndTimeBasedRollingPolicy();
        policy.setContext(lc);
        policy.setFileNamePattern(SIMULATOR_LOG_PATH + File.separator + "simulator-agent.%d{yyyy-MM-dd}.%i.log.zip");
        policy.setMaxHistory(3);
        policy.setMaxFileSize(FileSize.valueOf("100MB"));
        policy.setTotalSizeCap(FileSize.valueOf("300MB"));
        policy.setParent(appender);
        policy.start();
        appender.setRollingPolicy(policy);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(lc);
        encoder.setCharset(Charset.forName("UTF-8"));
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss} %-5level %msg%n");
        encoder.start();
        appender.setEncoder(encoder);

        appender.setContext(lc);
        appender.start();

        Logger rootLogger = lc.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setAdditive(false);
        rootLogger.addAppender(appender);
    }

    private void addSimulatorAgentErrorAppender(LoggerContext lc) {
        CustomerRollingFileAppender appender = new CustomerRollingFileAppender();
        appender.setName("SIMULATOR-AGENT-FILE-APPENDER-ERROR");

        ThresholdFilter filter = new ThresholdFilter();
        filter.setLevel("ERROR");
        filter.start();
        appender.addFilter(filter);

        appender.setFile(SIMULATOR_LOG_PATH + File.separator + "simulator-agent-error.log");

        CustomerSizeAndTimeBasedRollingPolicy policy = new CustomerSizeAndTimeBasedRollingPolicy();
        policy.setContext(lc);
        policy.setFileNamePattern(SIMULATOR_LOG_PATH + File.separator + "simulator-agent-error.%d{yyyy-MM-dd}.%i.log");
        policy.setMaxHistory(3);
        policy.setMaxFileSize(FileSize.valueOf("100MB"));
        policy.setTotalSizeCap(FileSize.valueOf("300MB"));
        policy.setParent(appender);
        policy.start();
        appender.setRollingPolicy(policy);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setCharset(Charset.forName("UTF-8"));
        encoder.setPattern("%replace(%msg){'[\r\n]+', 'nextline'}%nopex%n");
        encoder.setContext(lc);
        encoder.start();
        appender.setEncoder(encoder);

        appender.setContext(lc);
        appender.start();

        Logger rootLogger = lc.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setAdditive(false);
        rootLogger.addAppender(appender);

    }
}
