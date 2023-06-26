package com.shulie.instrument.simulator.agent.core.logback.spi;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.util.EnvUtil;
import ch.qos.logback.core.LogbackException;
import ch.qos.logback.core.util.StatusListenerConfigHelper;
import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMDCAdapter;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.spi.MDCAdapter;

public class SimulatorAgentSlf4JServiceProvider implements org.slf4j.spi.SLF4JServiceProvider {

    private static boolean configByXml = "xml".equals(System.getProperty("pradar.log.config.type"));

    private LoggerContext loggerContext;
    private final IMarkerFactory markerFactory = new BasicMarkerFactory();
    private final MDCAdapter mdcAdapter = new BasicMDCAdapter();

    public SimulatorAgentSlf4JServiceProvider() {
        loggerContext = new LoggerContext();
    }

    @Override
    public ILoggerFactory getLoggerFactory() {
        return loggerContext;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return markerFactory;
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return mdcAdapter;
    }

    @Override
    public String getRequestedApiVersion() {
        return "2.0.7";
    }

    @Override
    public void initialize() {
        if (!configByXml) {
            StatusListenerConfigHelper.installIfAsked(loggerContext);
            Configurator c = EnvUtil.loadFromServiceLoader(Configurator.class);
            if (c == null) {
                throw new LogbackException("[slf4j-spi] can`t find logback spi implementation with class: 'ch.qos.logback.classic.spi.Configurator' to config logback !!!");
            }
            try {
                c.setContext(loggerContext);
                c.configure(loggerContext);
            } catch (Exception e) {
                throw new LogbackException(String.format("[slf4j-spi] Failed to initialize Configurator: %s using ServiceLoader", c != null ? c.getClass().getCanonicalName() : "null"), e);
            }
        }
    }

}
