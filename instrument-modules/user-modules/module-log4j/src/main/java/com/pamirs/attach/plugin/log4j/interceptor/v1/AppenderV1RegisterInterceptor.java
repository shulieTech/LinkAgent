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
package com.pamirs.attach.plugin.log4j.interceptor.v1;

import com.pamirs.attach.plugin.log4j.destroy.Log4jDestroy;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.message.ConcurrentWeakHashMap;
import org.apache.log4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @Auther: vernon
 * @Date: 2020/12/9 11:22
 * @Description:
 */
@Destroyable(Log4jDestroy.class)
public class AppenderV1RegisterInterceptor extends AroundInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(AppenderV1RegisterInterceptor.class);

    private ConcurrentWeakHashMap cache = new ConcurrentWeakHashMap();

    protected boolean isBusinessLogOpen;
    protected String bizShadowLogPath;

    public AppenderV1RegisterInterceptor(boolean isBusinessLogOpen, String bizShadowLogPath) {
        this.isBusinessLogOpen = isBusinessLogOpen;
        this.bizShadowLogPath = bizShadowLogPath;
    }

    @Override
    public void doAfter(Advice advice) {
        if (!isBusinessLogOpen) {
            return;
        }

        Object[] args = advice.getParameterArray();
        if (args == null || args.length != 1) {
            return;
        }
        Appender appender = (Appender) args[0];
        if (cache.get(appender) != null) {
            return;
        }

        Object target = advice.getTarget();
        if (!(target instanceof Category)) {
            return;
        }
        if (appender.getName().startsWith(Pradar.CLUSTER_TEST_PREFIX)) {
            return;
        }
        Category category = (Category) target;

        if (appender instanceof FileAppender) {
            if (appender instanceof DailyRollingFileAppender) {
                DailyRollingFileAppender oldAppender = (DailyRollingFileAppender) appender;
                try {
                    DailyRollingFileAppender ptAppender = new DailyRollingFileAppender(
                            oldAppender.getLayout()
                            , bizShadowLogPath + oldAppender.getFile()
                            , oldAppender.getDatePattern()
                    );
                    ptAppender.setName(Pradar.CLUSTER_TEST_PREFIX + oldAppender.getName());
                    ptAppender.setAppend(oldAppender.getAppend());
                    ptAppender.setBufferedIO(oldAppender.getBufferedIO());
                    ptAppender.setBufferSize(oldAppender.getBufferSize());
                    ptAppender.setEncoding(oldAppender.getEncoding());
                    ptAppender.setErrorHandler(oldAppender.getErrorHandler());
                    ptAppender.setImmediateFlush(oldAppender.getImmediateFlush());
                    category.addAppender(ptAppender);
                    cache.put(appender, ptAppender);
                } catch (IOException e) {
                    logger.error("add DailyRollingFileAppender to category in Log4j module for v1 error.", e);
                }
            } else if (appender instanceof RollingFileAppender) {
                RollingFileAppender oldAppender = (RollingFileAppender) appender;
                try {
                    RollingFileAppender ptAppender = new RollingFileAppender(
                            oldAppender.getLayout()
                            , bizShadowLogPath + oldAppender.getFile(),
                            oldAppender.getAppend()
                    );
                    ptAppender.setName(Pradar.CLUSTER_TEST_PREFIX + oldAppender.getName());
                    ptAppender.setMaxBackupIndex(oldAppender.getMaxBackupIndex());
                    ptAppender.setMaximumFileSize(oldAppender.getMaximumFileSize());
                    ptAppender.setMaxFileSize(String.valueOf(oldAppender.getMaximumFileSize()));
                    ptAppender.setBufferedIO(oldAppender.getBufferedIO());
                    ptAppender.setBufferSize(oldAppender.getBufferSize());
                    ptAppender.setEncoding(oldAppender.getEncoding());
                    ptAppender.setErrorHandler(oldAppender.getErrorHandler());
                    ptAppender.setImmediateFlush(oldAppender.getImmediateFlush());
                    category.addAppender(ptAppender);
                    cache.put(appender, ptAppender);
                } catch (IOException e) {
                    logger.error("add RollingFileAppend to category in Log4j module for v1 error.", e);
                }

            } else if ("com.csair.engine.fq.commons.log.log4j.DailyRollingFileAppender".equals(appender.getClass().getName())) {
                com.csair.engine.fq.commons.log.log4j.DailyRollingFileAppender oldAppender
                        = (com.csair.engine.fq.commons.log.log4j.DailyRollingFileAppender) appender;

                try {
                    com.csair.engine.fq.commons.log.log4j.DailyRollingFileAppender ptAppender
                            = new com.csair.engine.fq.commons.log.log4j.DailyRollingFileAppender(
                            oldAppender.getLayout()
                            , bizShadowLogPath + oldAppender.getFile()
                            , oldAppender.getDatePattern()
                    );
                    ptAppender.setImmediateFlush(oldAppender.getImmediateFlush());
                    ptAppender.setErrorHandler(oldAppender.getErrorHandler());
                    ptAppender.setName(Pradar.CLUSTER_TEST_PREFIX + oldAppender.getName());
                    ptAppender.setThreshold(oldAppender.getThreshold());
                    ptAppender.setBufferedIO(oldAppender.getBufferedIO());
                    ptAppender.setEncoding(oldAppender.getEncoding());
                    ptAppender.setAppend(oldAppender.getAppend());
                    category.addAppender(ptAppender);
                    cache.put(appender, ptAppender);

                } catch (IOException e) {
                    logger.error("add com.csair.engine.fq.commons.log.log4j.DailyRollingFileAppender to category in Log4j module for v1 error.", e);
                }
            } else {
                FileAppender oldAppender = (FileAppender) appender;
                try {
                    FileAppender ptAppender = new FileAppender(
                            oldAppender.getLayout()
                            , bizShadowLogPath + oldAppender.getFile()
                            , oldAppender.getAppend()
                            , oldAppender.getBufferedIO()
                            , oldAppender.getBufferSize()
                    );
                    ptAppender.setImmediateFlush(oldAppender.getImmediateFlush());
                    ptAppender.setErrorHandler(oldAppender.getErrorHandler());
                    ptAppender.setName(Pradar.CLUSTER_TEST_PREFIX + oldAppender.getName());
                    ptAppender.setThreshold(oldAppender.getThreshold());
                    ptAppender.setBufferedIO(oldAppender.getBufferedIO());
                    ptAppender.setEncoding(oldAppender.getEncoding());
                    ptAppender.setAppend(oldAppender.getAppend());
                    category.addAppender(ptAppender);
                    cache.put(appender, ptAppender);
                } catch (IOException e) {
                    logger.error("add RollingFileAppend to category in Log4j module for v1 error.", e);
                }
            }
        }
    }

    @Override
    protected void clean() {
        cache.clear();
    }
}
