package com.pamirs.attach.plugin.log4j.interceptor.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/12/06 5:46 下午
 */
public class AppenderTypes {

    private static final List<AppenderType> appenderTypeList = new ArrayList<>();

    static {
        registerAppenderType(
            new ClassFileAppenderType(
                "org.apache.logging.log4j.core.appender.FileAppender",
                "org.apache.logging.log4j.core.appender.RollingFileAppender",
                "org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender"
            ));
        registerAppenderType(
            new RemoteAppenderType(
                "org.apache.logging.log4j.core.appender.mom.kafka.KafkaAppender",
                "org.apache.logging.log4j.core.appender.HttpAppender",
                "org.apache.logging.log4j.core.appender.db.jdbc.JdbcAppender"
            ));
    }

    public static void registerAppenderType(AppenderType appenderType) {
        appenderTypeList.add(appenderType);
    }

    public static boolean isFileAppender(Object appender) {
        for (AppenderType appenderType : appenderTypeList) {
            if (appenderType.isFileAppender(appender)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isRemoteAppender(Object appender) {
        for (AppenderType appenderType : appenderTypeList) {
            if (appenderType.isRemoteAppender(appender)) {
                return true;
            }
        }
        return false;
    }

    public interface AppenderType {

        boolean isFileAppender(Object appender);

        boolean isRemoteAppender(Object appender);
    }

    public static class ClassAppenderType implements AppenderType {

        private final Set<String> clazzNames = new HashSet<>();

        public ClassAppenderType(Class<?>... clazz) {
            for (Class<?> aClass : clazz) {
                clazzNames.add(aClass.getName());
            }
        }

        public ClassAppenderType(String... clazz) {
            clazzNames.addAll(Arrays.asList(clazz));
        }

        @Override
        public boolean isFileAppender(Object appender) {
            return isAssignable(appender);
        }

        @Override
        public boolean isRemoteAppender(Object appender) {
            return isAssignable(appender);
        }

        private boolean isAssignable(Object appender) {
            Class<?> appenderClass = appender.getClass();
            ClassLoader classLoader = appender.getClass().getClassLoader();
            for (String className : clazzNames) {
                try {
                    Class<?> clazz = classLoader.loadClass(className);
                    if (clazz.getName().equals(appenderClass.getName()) || clazz.isAssignableFrom(appenderClass)) {
                        return true;
                    }
                } catch (ClassNotFoundException ignore) {
                }
            }
            return false;
        }

    }

    public static class ClassFileAppenderType extends ClassAppenderType implements AppenderType {

        public ClassFileAppenderType(Class<?>... clazz) {
            super(clazz);
        }

        public ClassFileAppenderType(String... clazz) {
            super(clazz);
        }

        @Override
        public boolean isRemoteAppender(Object appender) {
            return false;
        }
    }

    public static class RemoteAppenderType extends ClassAppenderType implements AppenderType {

        public RemoteAppenderType(Class<?>... clazz) {
            super(clazz);
        }

        public RemoteAppenderType(String... clazz) {
            super(clazz);
        }

        @Override
        public boolean isFileAppender(Object appender) {
            return false;
        }
    }
}
