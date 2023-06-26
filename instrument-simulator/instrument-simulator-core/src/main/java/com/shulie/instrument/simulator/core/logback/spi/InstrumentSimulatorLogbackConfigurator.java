package com.shulie.instrument.simulator.core.logback.spi;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.LevelFilter;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.util.FileSize;
import com.shulie.instrument.simulator.core.logback.CustomerRollingFileAppender;
import com.shulie.instrument.simulator.core.logback.CustomerSizeAndTimeBasedRollingPolicy;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.Charset;

public class InstrumentSimulatorLogbackConfigurator extends ContextAwareBase implements ch.qos.logback.classic.spi.Configurator {

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
        configAsyncSimulatorLogger(lc);
        configAsyncSimulatorErrorLogger(lc);
        configMockLogger(lc);
        configTimeConsumingLogger(lc);
    }

    private void configAsyncSimulatorLogger(LoggerContext lc) {
        RollingFileAppender appender = new RollingFileAppender();
        appender.setContext(lc);
        appender.setName("SIMULATOR-FILE-APPENDER");
        appender.setFile(SIMULATOR_LOG_PATH + File.separator + "simulator.log");

        FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
        rollingPolicy.setParent(appender);
        rollingPolicy.setContext(lc);
        rollingPolicy.setFileNamePattern(SIMULATOR_LOG_PATH + File.separator + "simulator.%i.log");
        rollingPolicy.setMaxIndex(1);
        rollingPolicy.setMaxIndex(3);
        rollingPolicy.start();
        appender.setRollingPolicy(rollingPolicy);

        SizeBasedTriggeringPolicy triggeringPolicy = new SizeBasedTriggeringPolicy();
        triggeringPolicy.setMaxFileSize(FileSize.valueOf("100MB"));
        triggeringPolicy.setContext(lc);
        triggeringPolicy.start();
        appender.setTriggeringPolicy(triggeringPolicy);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(lc);
        encoder.setCharset(Charset.forName("UTF-8"));
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss} %-5level %msg%n");
        encoder.start();
        appender.setEncoder(encoder);

        appender.start();

        AsyncAppender asyncAppender = new AsyncAppender();
        asyncAppender.setContext(lc);
        asyncAppender.setName("ASYNC-SIMULATOR-FILE-APPENDER");
        asyncAppender.addAppender(appender);
        asyncAppender.start();

        Logger rootLogger = lc.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setAdditive(false);
        rootLogger.addAppender(asyncAppender);
    }

    private void configAsyncSimulatorErrorLogger(LoggerContext lc) {
        CustomerRollingFileAppender appender = new CustomerRollingFileAppender();
        appender.setContext(lc);
        appender.setName("ERROR-APPENDER");
        appender.setFile(SIMULATOR_LOG_PATH + File.separator + "simulator-error.log");

        ThresholdFilter filter = new ThresholdFilter();
        filter.setLevel("ERROR");
        filter.start();
        appender.addFilter(filter);

        CustomerSizeAndTimeBasedRollingPolicy rollingPolicy = new CustomerSizeAndTimeBasedRollingPolicy();
        rollingPolicy.setParent(appender);
        rollingPolicy.setContext(lc);
        rollingPolicy.setFileNamePattern(SIMULATOR_LOG_PATH + File.separator + "simulator-error.%d{yyyy-MM-dd}.%i.log");
        rollingPolicy.setMaxFileSize(FileSize.valueOf("50MB"));
        rollingPolicy.setMaxHistory(3);
        rollingPolicy.setTotalSizeCap(FileSize.valueOf("300MB"));
        rollingPolicy.start();
        appender.setRollingPolicy(rollingPolicy);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(lc);
        encoder.setCharset(Charset.forName("UTF-8"));
        encoder.setPattern("%replace(%msg){'[\r\n]+', 'nextline'}%nopex%n");
        encoder.start();
        appender.setEncoder(encoder);

        appender.start();

        AsyncAppender asyncAppender = new AsyncAppender();
        asyncAppender.setContext(lc);
        asyncAppender.setName("ASYNC-ERROR-APPENDER");
        asyncAppender.addAppender(appender);
        asyncAppender.start();

        Logger rootLogger = lc.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setAdditive(false);
        rootLogger.addAppender(asyncAppender);
    }

    private void configMockLogger(LoggerContext lc) {
        RollingFileAppender appender = new RollingFileAppender();
        appender.setContext(lc);
        appender.setName("MOCK-APPENDER");
        appender.setFile(SIMULATOR_LOG_PATH + File.separator + "mock.log");

        LevelFilter levelFilter = new LevelFilter();
        levelFilter.setLevel(Level.ERROR);
        levelFilter.setOnMatch(FilterReply.DENY);
        levelFilter.setOnMismatch(FilterReply.ACCEPT);
        levelFilter.start();
        appender.addFilter(levelFilter);

        SizeAndTimeBasedRollingPolicy rollingPolicy = new SizeAndTimeBasedRollingPolicy();
        rollingPolicy.setParent(appender);
        rollingPolicy.setContext(lc);
        rollingPolicy.setFileNamePattern(SIMULATOR_LOG_PATH + File.separator + "mock.%d{yyyy-MM-dd}.%i.log.zip");
        rollingPolicy.setMaxFileSize(FileSize.valueOf("100MB"));
        rollingPolicy.setMaxHistory(3);
        rollingPolicy.setTotalSizeCap(FileSize.valueOf("300MB"));
        rollingPolicy.start();
        appender.setRollingPolicy(rollingPolicy);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(lc);
        encoder.setCharset(Charset.forName("UTF-8"));
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss} %-5level %msg%n");
        encoder.start();
        appender.setEncoder(encoder);

        appender.start();

        Logger logger = lc.getLogger("MOCK-LOGGER");
        logger.setAdditive(false);
        logger.addAppender(appender);

    }

    private void configTimeConsumingLogger(LoggerContext lc) {
        RollingFileAppender appender = new RollingFileAppender();
        appender.setContext(lc);
        appender.setName("TIME-CONSUMING-APPENDER");
        appender.setFile(SIMULATOR_LOG_PATH + File.separator + "time-consuming.log");

        SizeAndTimeBasedRollingPolicy rollingPolicy = new SizeAndTimeBasedRollingPolicy();
        rollingPolicy.setFileNamePattern(SIMULATOR_LOG_PATH + File.separator + "time-consuming.%d{yyyy-MM-dd}.%i.log.zip");
        rollingPolicy.setMaxFileSize(FileSize.valueOf("100MB"));
        rollingPolicy.setParent(appender);
        rollingPolicy.setContext(lc);
        rollingPolicy.setMaxHistory(3);
        rollingPolicy.setTotalSizeCap(FileSize.valueOf("1GB"));
        rollingPolicy.start();
        appender.setRollingPolicy(rollingPolicy);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(lc);
        encoder.setCharset(Charset.forName("UTF-8"));
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss} %-5level %msg%n");
        encoder.start();
        appender.setEncoder(encoder);

        appender.start();

        AsyncAppender asyncAppender = new AsyncAppender();
        asyncAppender.setContext(lc);
        asyncAppender.setName("ASYNC-TIME-CONSUMING-APPENDER");
        asyncAppender.addAppender(appender);
        asyncAppender.start();

        Logger logger = lc.getLogger("TIME-CONSUMING-LOGGER");
        logger.setAdditive(false);
        logger.addAppender(asyncAppender);
    }

}
