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
package com.pamirs.attach.plugin.rabbitmq.common;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/09/11 7:32 下午
 */
public class ThreadPoolInfo {

    private final int maxSize;

    private final int coreSize;

    private final int keepAliveTime;

    private final int blockQueueCapacity;

    public ThreadPoolInfo(int coreSize, int maxSize, int keepAliveTime, int blockQueueCapacity) {
        this.maxSize = maxSize;
        this.coreSize = coreSize;
        this.keepAliveTime = keepAliveTime;
        this.blockQueueCapacity = blockQueueCapacity;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getCoreSize() {
        return coreSize;
    }

    public int getKeepAliveTime() {
        return keepAliveTime;
    }

    public int getBlockQueueCapacity() {
        return blockQueueCapacity;
    }

    public ExecutorService build() {
        BlockingQueue<Runnable> blockingQueue = blockQueueCapacity > 0 ? new LinkedBlockingQueue<Runnable>(
            blockQueueCapacity) : new SynchronousQueue<Runnable>();
        return new ThreadPoolExecutor(this.coreSize, this.maxSize, keepAliveTime, TimeUnit.SECONDS, blockingQueue, new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "shadow-consumer-worker-" + threadNumber.getAndIncrement());
                if (t.isDaemon()) {t.setDaemon(false);}
                if (t.getPriority() != Thread.NORM_PRIORITY) {t.setPriority(Thread.NORM_PRIORITY);}
                return t;
            }
        });
    }


}
