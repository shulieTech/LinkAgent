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
package com.pamirs.attach.plugin.log4j.interceptor.v2.appender.creator;

import java.nio.file.attribute.PosixFilePermissions;

import com.pamirs.pradar.Pradar;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/11/05 11:52 上午
 */
public class FileShadowAppenderCreator implements ShadowAppenderCreator<FileAppender> {

    private final static Logger logger = LoggerFactory.getLogger(FileShadowAppenderCreator.class);

    @Override
    @SuppressWarnings({"rawtype"})
    public FileAppender creatorPtAppender(FileAppender appender, String bizShadowLogPath) {
        String ptAppenderName = Pradar.CLUSTER_TEST_PREFIX + appender.getName();
        String ptFileName = bizShadowLogPath + appender.getFileName();
        FileManager manager = appender.getManager();
        FileAppender.Builder<?> builder = FileAppender.newBuilder();
        Object advertisement = Reflect.on(appender).get("advertisement");
        if (advertisement != null) {
            builder.withAdvertise(true);
        }
        builder.withAppend(true)
            .withFileName(ptFileName)
            .withCreateOnDemand(manager.isCreateOnDemand())
            .withLocking(manager.isLocking())
            .withAdvertiseUri(Reflect.on(manager).get("advertiseURI"))
            .withImmediateFlush(true)
            .withLayout(appender.getLayout())
            .withConfiguration(manager.getLoggerContext().getConfiguration())
            .withIgnoreExceptions(appender.ignoreExceptions())
            .withName(ptAppenderName)
            .withFilter(appender.getFilter());
        try {
            builder.withFilePermissions(PosixFilePermissions.toString(manager.getFilePermissions()))
                .withFileOwner(manager.getFileOwner())
                .withFileGroup(manager.getFileGroup());
        } catch (Throwable ignore) {
            //低版本,忽略
        }
        return builder.build();
    }
}
