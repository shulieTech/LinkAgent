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

import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;

import com.pamirs.attach.plugin.log4j.interceptor.utils.BeanUtil;
import com.pamirs.attach.plugin.log4j.interceptor.v2.holder.Cache.FileManagerCache;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mocheng
 * @since 2021/09/17 16:47
 */
public class ManagerHolder {

    private String ptFileName;
    private String ptFilePattern;

    static Logger logger = LoggerFactory.getLogger(ManagerHolder.class);

    public ManagerHolder(String ptFileName, String ptFilePattern) {
        this.ptFileName = ptFileName;
        this.ptFilePattern = ptFilePattern;
    }

    public Object copyManager(Object manager) throws ClassNotFoundException {
        Object ptManager = null;
        if (manager != null) {
            ptManager = FileManagerCache.get(manager);
            if (ptManager == null) {
                String className = manager.getClass().getName();
                if (className.equals("org.apache.logging.log4j.core.appender.FileManager")) {
                    ptManager = copyFileManager(manager);
                } else if (className.equals("org.apache.logging.log4j.core.appender.rolling.RollingFileManager")) {
                    ptManager = copyRollingFileManager(manager);
                } else if (className.equals("org.apache.logging.log4j.core.appender.rolling.RollingRandomAccessFileManager")) {
                    ptManager = copyRollingRandomAccessFileManager(manager);
                } else if (className.equals("org.apache.logging.log4j.core.appender.RandomAccessFileManager")) {
                    ptManager = copyRandomAccessFileManager(manager);
                } else {
                    logger.error("[Log4j-Plugin] not adapted. copyFileManager fail, FileManager ClassNotFound: {}", className);
                }
            }
        }
        if (ptManager != null) {
            BeanUtil.set(ptManager, "triggeringPolicy", TriggerPolicyHolder.copyTriggerPolicy(ptManager, Reflect.on(manager).get("triggeringPolicy")));
            BeanUtil.setIfPresent(ptManager, manager, "rolloverStrategy", "outputStream", "os");
        }
        return ptManager;
    }

    private Object copyFileManager(Object manager) throws ClassNotFoundException {
        Object ptManager = null;
        Constructor<?>[] constructors = manager.getClass().getDeclaredConstructors();
        for (Constructor<?> constructor: constructors) {
            if (constructor == null) {
                return null;
            }
            constructor.setAccessible(true);
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            Boolean isAppend = Reflect.on(manager).get("isAppend");
            int length = parameterTypes.length;
            if (length == 8) {
                if (parameterTypes[7].isAssignableFrom(Class.forName("java.io.ByteBuffer"))) {
                    // @since 2.6
                    ptManager = Reflect.on(manager).create(
                        ptFileName,
                        Reflect.on(manager).get("os"),//outputStream
                        isAppend,
                        Reflect.on(manager).get("isLocking"),
                        Reflect.on(manager).get("advertiseURI"),
                        Reflect.on(manager).get("layout"),
                        true,//writeHeader
                        ByteBuffer.wrap(new byte[Integer.parseInt((String)Reflect.on(manager).get("bufferSize"))])
                    ).get();
                    break;
                } else {
                    ptManager = Reflect.on(manager).create(
                        ptFileName,
                        Reflect.on(manager).get("os"),//outputStream
                        isAppend,
                        Reflect.on(manager).get("isLocking"),
                        Reflect.on(manager).get("advertiseURI"),
                        Reflect.on(manager).get("layout"),
                        Reflect.on(manager).get("bufferSize"),
                        true//writeHeader
                    ).get();
                }
            } else if (length == 10) {
                // @since 2.7
                ptManager = Reflect.on(manager).create(
                    Reflect.on(manager).get("loggerContext"),
                    ptFileName,
                    Reflect.on(manager).get("os"),// outputStream
                    isAppend,
                    Reflect.on(manager).get("isLocking"),
                    Reflect.on(manager).get("createOnDemand"),
                    Reflect.on(manager).get("advertiseURI"),
                    Reflect.on(manager).get("layout"),
                    true,// writeHeader
                    ByteBuffer.wrap(new byte[Integer.parseInt((String)Reflect.on(manager).get("bufferSize"))])
                ).get();
            } else {
                logger.error("[Log4j-Plugin] not adapted. copyFileManager fail, constructor args length: {}", length);
            }
        }
        return ptManager;
    }

    private Object copyRandomAccessFileManager(Object manager) {
        Object ptManager = null;
        Constructor<?>[] constructors = manager.getClass().getDeclaredConstructors();
        for (Constructor<?> constructor: constructors) {
            if (constructor == null) {
                return null;
            }
            constructor.setAccessible(true);
            int length = constructor.getParameterTypes().length;
            if (length == 8) {
                // since 2.8.2
                Boolean isAppend = Reflect.on(manager).get("isAppend");
                ptManager = Reflect.on(manager).create(
                    Reflect.on(manager).get("loggerContext"),
                    copyRandomAccessFile(isAppend, Reflect.on(manager).get("randomAccessFile")),
                    ptFileName,
                    Reflect.on(manager).get("os"),//outputStream
                    Reflect.on(manager).get("bufferSize"),
                    Reflect.on(manager).get("advertiseURI"),
                    Reflect.on(manager).get("layout"),
                    true//writeHeader
                ).get();
                break;
            } else {
                logger.error("[Log4j-Plugin] not adapted. copyRollingRandomAccessFileManager fail, constructor args length: {}", length);
            }
        }
        return ptManager;
    }

    private Object copyRollingFileManager(Object manager) throws ClassNotFoundException {
        Object ptManager = null;
        Constructor<?>[] constructors = manager.getClass().getDeclaredConstructors();
        for (Constructor<?> constructor: constructors) {
            if (constructor == null) {
                return null;
            }
            constructor.setAccessible(true);
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            int length = parameterTypes.length;
            Boolean isAppend = Reflect.on(manager).get("isAppend");
            if (length == 12) {
                // since 2.8.2
                if (parameterTypes[7].isAssignableFrom(Class.forName("java.io.ByteBuffer"))) {
                    ptManager = Reflect.on(manager).create(
                        ptFileName,
                        ptFilePattern,
                        Reflect.on(manager).get("os"),//outputStream
                        isAppend,
                        Reflect.on(manager).get("size"),
                        Reflect.on(manager).get("initialTime"),
                        null,
                        Reflect.on(manager).get("rolloverStrategy"),
                        Reflect.on(manager).get("advertiseURI"),
                        Reflect.on(manager).get("layout"),
                        true,//writeHeader
                        ByteBuffer.wrap(new byte[Integer.parseInt((String)Reflect.on(manager).get("bufferSize"))])
                    ).get();
                } else {
                    ptManager = Reflect.on(manager).create(
                        ptFileName,
                        ptFilePattern,
                        Reflect.on(manager).get("os"),//outputStream
                        isAppend,
                        Reflect.on(manager).get("size"),
                        Reflect.on(manager).get("initialTime"),
                        null,
                        Reflect.on(manager).get("rolloverStrategy"),
                        Reflect.on(manager).get("advertiseURI"),
                        Reflect.on(manager).get("layout"),
                        Reflect.on(manager).get("bufferSize"),
                        true//writeHeader
                    ).get();
                }
            } else if (length == 14) {
                ptManager = Reflect.on(manager).create(
                    Reflect.on(manager).get("loggerContext"),
                    ptFileName,
                    ptFilePattern,
                    Reflect.on(manager).get("os"),//outputStream
                    isAppend,
                    Reflect.on(manager).get("createOnDemand"),
                    Reflect.on(manager).get("size"),
                    Reflect.on(manager).get("initialTime"),
                    null,
                    Reflect.on(manager).get("rolloverStrategy"),
                    Reflect.on(manager).get("advertiseURI"),
                    Reflect.on(manager).get("layout"),
                    true,//writeHeader
                    ByteBuffer.wrap(new byte[Integer.parseInt((String)Reflect.on(manager).get("bufferSize"))])
                ).get();
            } else {
                logger.error("[Log4j-Plugin] not adapted. copyRollingFileManager fail, constructor args length: {}", length);
            }
        }
        if (ptManager != null) {
            Reflect.on(ptManager).set("initialized", Reflect.on(manager).get("initialized"));
        }
        return ptManager;
    }

    public Object copyRollingRandomAccessFileManager(Object manager) {
        Object ptManager = null;
        Constructor<?>[] constructors = manager.getClass().getDeclaredConstructors();
        for (Constructor<?> constructor: constructors) {
            if (constructor == null) {
                return null;
            }
            constructor.setAccessible(true);
            int length = constructor.getParameterTypes().length;
            if (length == 15) {
                // since 2.8.2
                Boolean isAppend = Reflect.on(manager).get("isAppend");
                ptManager = Reflect.on(manager).create(
                    Reflect.on(manager).get("loggerContext"),
                    copyRandomAccessFile(isAppend, Reflect.on(manager).get("randomAccessFile")),
                    ptFileName,
                    ptFilePattern,
                    null,//outputStream
                    isAppend,
                    true,//Reflect.on(manager).get("immediateFlush"),
                    Reflect.on(manager).get("bufferSize"),
                    Reflect.on(manager).get("size"),
                    Reflect.on(manager).get("initialTime"),
                    null,
                    Reflect.on(manager).get("rolloverStrategy"),
                    Reflect.on(manager).get("advertiseURI"),
                    Reflect.on(manager).get("layout"),
                    true
                ).get();
                Reflect.on(ptManager).set("initialized", Reflect.on(manager).get("initialized"));
                break;
            } else {
                logger.error("[Log4j-Plugin] not adapted. copyRollingRandomAccessFileManager fail, constructor args length: {}", length);
            }
        }
        return ptManager;
    }

    private Object copyRandomAccessFile(Boolean isAppend, Object randomAccessFile) {
        Object ptRandomAccessFile = null;
        if (randomAccessFile != null) {
            String className = randomAccessFile.getClass().getName();
            if (className.equals("java.io.RandomAccessFile")) {
                File ptFile = new File(ptFileName);
                if (!(Boolean) (Reflect.on(ptFile).call("exists").get())) {
                    Object parentFile = Reflect.on(ptFile).call("getParentFile").get();
                    if(!(Boolean) (Reflect.on(parentFile).call("exists").get())) {
                        Reflect.on(parentFile).call("mkdirs");
                    }
                    Reflect.on(ptFile).call("createNewFile");
                }
                ptRandomAccessFile = Reflect.on(randomAccessFile).create(ptFile, "rw").get();
                if (ptRandomAccessFile != null) {
                    if (isAppend) {
                        final long length = Reflect.on(ptRandomAccessFile).call("length").get();
                        Reflect.on(ptRandomAccessFile).call("seek", length);
                    } else {
                        Reflect.on(ptRandomAccessFile).call("seek", 0);
                    }
                }
            } else {
                logger.error("[Log4j-Plugin] not adapted. copyRandomAccessFile fail, RandomAccessFile ClassNotFound: {}", className);
            }
        }
        return ptRandomAccessFile;
    }

}
