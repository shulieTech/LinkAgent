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
package com.pamirs.pradar;

import com.shulie.instrument.simulator.api.executors.ExecutorServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pradar 定时检查，主要有下面的行为： <ul> <li>删除 .deleted 结尾的文件 <li>检测文件开关 <li>间隔一段时间输出一次索引 <li>日志文件被删除，则尝试重新创建之
 * <li>定时强制输出文件内容 <li> </ul>
 */
class PradarLogDaemon implements Runnable {

    private static final String SWITCH_ON = "true";
    private static final String SWITCH_OFF = "false";

    private static AtomicBoolean running = new AtomicBoolean(false);

    private static final CopyOnWriteArrayList<PradarAppender> watchedAppenders
            = new CopyOnWriteArrayList<PradarAppender>();

    // 文件开关
    private static final File configFile = new File(Pradar.PRADAR_LOG_DIR + "config.properties");

    private final static Logger LOGGER = LoggerFactory.getLogger(PradarLogDaemon.class);

    private static ScheduledFuture scheduledFuture;

    /**
     * 定期检测日志文件：如果被删除，则尝试重新创建之；强制刷新 appender
     */
    static final PradarAppender watch(PradarAppender appender) {
        watchedAppenders.addIfAbsent(appender);
        return appender;
    }

    static final boolean unwatch(PradarAppender appender) {
        return watchedAppenders.remove(appender);
    }

    @Override
    public void run() {
        // 定时清理
        cleanupFiles();

        // 如果被删除，则尝试重新创建之；强制刷新 appender
        flushAndReload();
    }

    private void cleanupFiles() {
        for (PradarAppender watchedAppender : watchedAppenders) {
            try {
                watchedAppender.cleanup();
            } catch (Throwable e) {
                LOGGER.error("fail to cleanup: {}", watchedAppender, e);
            }
        }
    }

    private void flushAndReload() {
        for (PradarAppender watchedAppender : watchedAppenders) {
            try {
                watchedAppender.reload();
            } catch (Throwable e) {
                LOGGER.error("fail to reload: {}", watchedAppender, e);
            }
        }
    }

    private String getSystemProperty(String propertyName) {
        String value = System.getProperty(propertyName);
        if (value == null) {
            value = System.getenv(propertyName);
        }
        return value;
    }

    static void start() {
        if (PradarSwitcher.isPradarLogDaemonEnabled() && running.compareAndSet(false, true)) {
            int logDaemonInterval = Pradar.PRADAR_LOG_DAEMON_INTERVAL;
            scheduledFuture = ExecutorServiceFactory.getFactory().scheduleAtFixedRate(new PradarLogDaemon(), logDaemonInterval, logDaemonInterval, TimeUnit.SECONDS);
        } else {
            LOGGER.warn("PradarLogDaemon start failed. cause by logDaemonSwitcher: {}, runningStatus: {}", PradarSwitcher.isPradarLogDaemonEnabled(), running.get());
        }
    }

    static void shutdown() {
        if (scheduledFuture != null && !scheduledFuture.isDone() && !scheduledFuture.isCancelled()) {
            try {
                scheduledFuture.cancel(true);
            } catch (Throwable e) {
                LOGGER.error("shutdown PradarLogDaemon failed: ", e);
            }
        }
    }

    static void flushAndWait() {
        for (PradarAppender watchedAppender : watchedAppenders) {
            try {
                if (watchedAppender instanceof AsyncAppender) {
                    ((AsyncAppender) watchedAppender).flushAndWait();
                } else {
                    watchedAppender.flush();
                }
            } catch (Throwable e) {
                LOGGER.error("fail to flush: {}", watchedAppender, e);
            }
        }
    }
}
