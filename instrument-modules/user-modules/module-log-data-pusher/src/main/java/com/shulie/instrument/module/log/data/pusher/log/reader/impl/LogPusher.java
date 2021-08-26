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
package com.shulie.instrument.module.log.data.pusher.log.reader.impl;

import com.shulie.instrument.module.log.data.pusher.log.reader.FileReader;
import com.shulie.instrument.simulator.api.executors.ExecutorServiceFactory;
import com.shulie.instrument.simulator.api.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * log日志文件推送
 *
 * @author xiaobin.zfb
 * @since 2020/8/6 8:52 下午
 */
public class LogPusher {
    /**
     * 所有的日志文件
     */
    private FileReader[] fileReaders;
    private List<FileReader> failures;
    private volatile boolean isStarted;
    private ScheduledFuture positionFuture;
    private ScheduledFuture failuresFuture;

    public LogPusher(List<LogPusherOptions> options) {
        if (CollectionUtils.isEmpty(options)) {
            return;
        }
        this.failures = new ArrayList<FileReader>();

        this.fileReaders = new FileReader[options.size()];
        int idx = 0;
        for (LogPusherOptions logPusherOptions : options) {
            fileReaders[idx++] = new DefaultFileReader(logPusherOptions.getDataType(), logPusherOptions.getVersion(),
                    logPusherOptions.getPath(), logPusherOptions.getLogCallback(), logPusherOptions.getMaxFailureSleepInterval());
        }

    }

    public void start() {
        if (isStarted) {
            return;
        }
        isStarted = true;
        if (this.fileReaders == null) {
            return;
        }
        /**
         * 启动所有的FileReader
         */
        for (FileReader fileReader : this.fileReaders) {
            try {
                boolean isSuccess = fileReader.start();
                if (!isSuccess) {
                    this.failures.add(fileReader);
                }
            } catch (Throwable e) {
                this.failures.add(fileReader);
            }
        }

        positionFuture = ExecutorServiceFactory.getFactory().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                for (FileReader fileReader : fileReaders) {
                    if (!fileReader.isStarted()) {
                        continue;
                    }
                    fileReader.savePosition();
                }
            }
        }, 2, 2, TimeUnit.SECONDS);

        if (!this.failures.isEmpty()) {
            failuresFuture = ExecutorServiceFactory.getFactory().scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    if (failures.isEmpty()) {
                        return;
                    }

                    Iterator<FileReader> it = failures.iterator();
                    while (it.hasNext()) {
                        FileReader fileReader = it.next();
                        try {
                            boolean isSuccess = fileReader.start();
                            if (isSuccess) {
                                it.remove();
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, 5, 5, TimeUnit.SECONDS);
        }
    }

    public void stop() {
        if (!isStarted) {
            return;
        }
        this.isStarted = false;
        if (positionFuture != null && !positionFuture.isDone() && !positionFuture.isCancelled()) {
            positionFuture.cancel(true);
        }
        if (failuresFuture != null && !failuresFuture.isDone() && !failuresFuture.isCancelled()) {
            failuresFuture.cancel(true);
        }
        if (this.fileReaders == null) {
            return;
        }
        for (FileReader fileReader : this.fileReaders) {
            if (fileReader.isStopped()) {
                continue;
            }
            fileReader.stop();
        }
        this.fileReaders = null;
    }
}
