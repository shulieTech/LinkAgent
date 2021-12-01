/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.log4j.interceptor.v2.holder;

import java.lang.reflect.Constructor;

import com.pamirs.attach.plugin.log4j.interceptor.utils.BeanUtil;
import com.pamirs.pradar.Pradar;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mocheng
 * @since 2021/09/14 15:21
 */
public class AppenderHolder {

    protected String bizShadowLogPath;
    private ManagerHolder managerHolder;

    private String ptAppenderName;
    private String ptFileName;
    private String ptFilePattern;

    static Logger logger = LoggerFactory.getLogger(AppenderHolder.class);

    public AppenderHolder(String bizShadowLogPath) {
        this.bizShadowLogPath = bizShadowLogPath;
    }

    public Object copyAppender(Object appender) throws ClassNotFoundException {
        Object ptAppender = null;
        if (appender != null) {
            try {
                ptAppenderName = Pradar.CLUSTER_TEST_PREFIX + Reflect.on(appender).get("name");
                ptFileName = bizShadowLogPath + Reflect.on(appender).get("fileName");
                ptFilePattern = bizShadowLogPath + Reflect.on(appender).get("filePattern");
                managerHolder = new ManagerHolder(ptFileName, ptFilePattern);
                String className = appender.getClass().getName();
                if (className.equals("org.apache.logging.log4j.core.appender.RollingFileAppender")) {
                    ptAppender = copyRollingFileAppender(appender);
                } else if (className.equals("org.apache.logging.log4j.core.appender.FileAppender")) {
                    ptAppender = copyFileAppender(appender);
                } else if (className.equals("org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender")) {
                    ptAppender = copyRollingRandomAccessFileAppender(appender);
                } else {
                    logger.error("[Log4j-Plugin] not adapted. copyAppender fail, Appender ClassNotFound: {}", className);
                }
                if (ptAppender != null) {
                    Reflect.on(ptAppender).call("start").get();
                }
            } catch (Exception e) {
                logger.error("copyAppender fail , appender is {}", appender.getClass().getName());
                throw new RuntimeException(e);
            }
        }
        return ptAppender;
    }

    public Object copyRollingFileAppender(Object appender) throws ClassNotFoundException {
        Object ptAppender = null;
        Constructor<?>[] constructors = appender.getClass().getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            if (constructor == null) {
                return null;
            }
            constructor.setAccessible(true);
            int length = constructor.getTypeParameters().length;
            Object manager = Reflect.on(appender).call("getManager").get();
            Object ptManager = managerHolder.copyManager(manager);
            if (length == 10) {
                ptAppender = Reflect.on(appender).create(
                    ptAppenderName,
                    Reflect.on(appender).get("layout"),
                    Reflect.on(appender).get("filter"),
                    ptManager,
                    ptFileName,
                    ptFilePattern,
                    Reflect.on(appender).get("ignoreExceptions"),
                    Reflect.on(appender).get("immediateFlush"),
                    Reflect.on(appender).get("advertiser"),
                    Reflect.on(appender).get("properties")
                ).get();
                break;
            } else if (length == 9) {
                ptAppender = Reflect.on(appender).create(
                    ptAppenderName,
                    Reflect.on(appender).get("layout"),
                    Reflect.on(appender).get("filter"),
                    ptManager,
                    ptFileName,
                    ptFilePattern,
                    Reflect.on(appender).get("ignoreExceptions"),
                    Reflect.on(appender).get("immediateFlush"),
                    Reflect.on(appender).get("advertiser")
                ).get();
                break;
            } else {
                logger.error("[Log4j-Plugin] not adapted. copyRollingFileAppender fail, construct args length: {}", length);
            }
        }
        return ptAppender;
    }

    public Object copyFileAppender(Object appender) throws ClassNotFoundException {
        Object ptAppender = null;
        Constructor<?>[] constructors = appender.getClass().getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            if (constructor == null) {
                return null;
            }
            constructor.setAccessible(true);
            int length = constructor.getTypeParameters().length;
            Object manager = Reflect.on(appender).call("getManager").get();
            Object ptManager = managerHolder.copyManager(manager);
            if (length == 9) {
                ptAppender = Reflect.on(appender).create(
                    ptAppenderName,
                    Reflect.on(appender).get("layout"),
                    Reflect.on(appender).get("filter"),
                    ptManager,
                    ptFileName,
                    Reflect.on(appender).get("ignoreExceptions"),
                    Reflect.on(appender).get("immediateFlush"),
                    Reflect.on(appender).get("advertiser"),
                    Reflect.on(appender).get("properties")
                ).get();
                break;
            } else if (length == 8) {
                ptAppender = Reflect.on(appender).create(
                    ptAppenderName,
                    Reflect.on(appender).get("layout"),
                    Reflect.on(appender).get("filter"),
                    ptManager
                    , ptFileName
                    , Reflect.on(appender).get("ignoreExceptions")
                    , Reflect.on(appender).get("immediateFlush")
                    , Reflect.on(appender).get("advertiser")
                ).get();
                break;
            } else {
                logger.error("[Log4j-Plugin] not adapted. copyFileAppender fail, construct args length: {}", length);
            }
        }
        return ptAppender;
    }

    public Object copyRollingRandomAccessFileAppender(Object appender) throws ClassNotFoundException {
        Object ptAppender = BeanUtil.copyBean(appender);
        Constructor<?>[] constructors = appender.getClass().getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            if (constructor == null) {
                return null;
            }
            constructor.setAccessible(true);
            int length = constructor.getParameterTypes().length;
            Object manager = Reflect.on(appender).call("getManager").get();
            Object ptManager = managerHolder.copyManager(manager);
            if (length == 11) {
                //v2 2.14.4
                Reflect.on(ptAppender).set("name", ptAppenderName);
                Reflect.on(ptAppender).set("fileName", ptFileName);
                Reflect.on(ptAppender).set("filePattern", ptFilePattern);
                Reflect.on(ptAppender).set("manager", ptManager);
                Reflect.on(ptAppender).set("name", ptAppenderName);
                break;
            } else if (length == 8) {
                //v2 2.11.1 or 2.5
                ptAppender = Reflect.on(appender).create(
                    ptAppenderName
                    , Reflect.on(appender).get("layout")
                    , Reflect.on(appender).get("filter")
                    , ptManager
                    , ptFileName
                    , ptFilePattern
                    , Reflect.on(appender).get("ignoreExceptions")
                    , Reflect.on(appender).get("immediateFlush")
                    , Reflect.on(appender).get("bufferSize")
                    , Reflect.on(appender).get("advertiser")
                ).get();
                break;
            } else {
                logger.error(
                    "[Log4j-Plugin] not adapted. copyRollingRandomAccessFileAppender fail, construct args length: {}",
                    length);
            }
        }
        return ptAppender;
    }
}
