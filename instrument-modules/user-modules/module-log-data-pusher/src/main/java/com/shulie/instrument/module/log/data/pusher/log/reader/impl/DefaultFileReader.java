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

import com.pamirs.pradar.Pradar;
import com.shulie.instrument.module.log.data.pusher.log.callback.LogCallback;
import com.shulie.instrument.module.log.data.pusher.log.reader.FileFetcher;
import com.shulie.instrument.module.log.data.pusher.log.reader.FileReader;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/8/6 7:51 下午
 */
class DefaultFileReader implements FileReader {
    private final static Logger LOGGER = LoggerFactory.getLogger(DefaultFileReader.class.getName());

    /**
     * 默认连续推送失败时的最大休眠间隔时长
     */
    private final static int DEFAULT_MAX_FAILURE_SLEEP_INTERVAL = 10000;
    public static final long[] ZERO = {0L, 0L};
    private String path;
    private volatile long position = -1;
    /**
     * 回调信息
     */
    private LogCallback callback;
    /**
     * 是否启动
     */
    private boolean isStarting;

    /**
     * 当前的文件内容获取器
     */
    private FileFetcher fileFetcher;

    /**
     * 数据类型
     */
    private byte dataType;

    /**
     * 版本号
     */
    private int version;

    /**
     * 范围
     */
    private volatile long[] fileRange;
    /**
     * 上次更新的范围
     */
    private volatile long lastTime;

    /**
     * 统计连续推送失败的次数，随着连续推送失败次数的增大，休眠时间会越来越长
     */
    private AtomicInteger pushFailureCount = new AtomicInteger(0);

    /**
     * 连续推送失败时的最大休眠间隔时长,默认为10000ms
     */
    private int maxFailureSleepInterval;

    /**
     * 推送日志线程使用单独的线程，避免公共线程池被占满时导致日志无法推送
     */
    private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "Log-Data-Pusher-Service");
            t.setDaemon(true);
            t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    LOGGER.error("Thread {} caught a unknow exception with UncaughtExceptionHandler", t.getName(), e);
                }
            });
            return t;
        }
    });

    public DefaultFileReader(byte dataType, int version, String path, LogCallback callback) {
        this(dataType, version, path, callback, DEFAULT_MAX_FAILURE_SLEEP_INTERVAL);
    }

    public DefaultFileReader(byte dataType, int version, String path, LogCallback callback, int maxFailureSleepInterval) {
        this.path = path;
        this.version = version;
        this.callback = callback;
        this.dataType = dataType;
        this.maxFailureSleepInterval = maxFailureSleepInterval;
    }

    @Override
    public boolean start() {
        File file = new File(path);
        if (!file.getParentFile().exists()) {
            return false;
        }
        File idxFile = new File(getIdxPath());
        initPosition(idxFile);

        boolean isSuccess = resetFileFetcher();
        if (!isSuccess) {
            return false;
        }
        /**
         * 将启动标识置成true
         */
        isStarting = true;
        service.schedule(new Runnable() {
            @Override
            public void run() {
                if (isStarting) {
                    try {
                        boolean isSuccess = push();
                        if (!isSuccess) {
                            pushFailureCount.incrementAndGet();
                            int sleep = pushFailureCount.get() * 1000;
                            if (sleep > maxFailureSleepInterval) {
                                sleep = maxFailureSleepInterval;
                            }
                            //如果失败则休眠一会再进行下一次推送
                            service.schedule(this, sleep, TimeUnit.MILLISECONDS);
                        } else {
                            pushFailureCount.set(0);
                            service.schedule(this, 0, TimeUnit.SECONDS);
                        }
                    } catch (Throwable e) {
                        service.schedule(this, 0, TimeUnit.SECONDS);
                    }
                }
            }
        }, 1, TimeUnit.SECONDS);
        return true;
    }

    /**
     * 重置FileFetcher
     *
     * @return
     */
    private boolean resetFileFetcher() {
        File target = new File(path);
        if (!target.exists() || !target.isFile()) {
            target = getTarget(path, this.position);
            if (target == null) {
                return false;
            }
        }

        try {
            FileFetcher newFileFetcher = new FileFetcher(target);
            if (this.fileFetcher != null) {
                try {
                    this.fileFetcher.close();
                } catch (IOException e) {
                    LOGGER.error("", e);
                }
            }
            this.fileFetcher = newFileFetcher;
        } catch (FileNotFoundException e) {
            File idxFile = new File(getIdxPath());
            /**
             * 有可能因为文件已经滚动了,再重新初始化一次位点
             */
            initPosition(idxFile);
            target = new File(path);
            if (!target.exists() || !target.isFile()) {
                target = getTarget(path, this.position);
                if (target == null) {
                    return false;
                }
            }
            try {
                FileFetcher newFileFetcher = new FileFetcher(target);
                if (this.fileFetcher != null) {
                    try {
                        this.fileFetcher.close();
                    } catch (IOException ex) {
                        LOGGER.error("", ex);
                    }
                }
                this.fileFetcher = newFileFetcher;
            } catch (FileNotFoundException ex) {
                /**
                 * 如果还是找不到文件,则返回失败
                 */
                return false;
            }
        }
        return true;
    }

    /**
     * 推送数据
     * 每两秒钟更新一次日志内容范围
     *
     * @return 返回是否下次不休眠继续推
     */
    private boolean push() {
        try {
            if (fileRange == null) {
                this.fileRange = getFileRange();
                lastTime = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - lastTime > 2000) {
                this.fileRange = getFileRange();
                lastTime = System.currentTimeMillis();
            }

            /**
             * 如果位点比最小位点还小，重置读取的文件并且将位点置为当前最小位点,返回true则让其赶紧拉取
             */
            if (this.position < fileRange[0]) {
                this.position = fileRange[0];
                resetFileFetcher();
                return true;
            }

            /**
             * 如果位点已经超过了最大位点则将位点重置，并且重置读取的文件
             */
            if (this.position > fileRange[1]) {
                this.position = fileRange[1];
                resetFileFetcher();
                return false;
            }
            /**
             * 没有新的内容则先不推
             */
            if (position == fileRange[1]) {
                return false;
            }
            /**
             * 检查一下是否是开启状态
             */
            if (!this.fileFetcher.getFc().isOpen()) {
                resetFileFetcher();
            }

            long length = this.fileFetcher.readAvailableLength(position, Pradar.PUSH_MAX_SIZE);
            /**
             * 如果未读取内容
             */
            if (length == 0) {
                /**
                 * 如果位点比最小位点还小，重置读取的文件并且将位点置为当前最小位点,返回true则让其赶紧拉取
                 */
                if (this.position < fileRange[0]) {
                    this.position = fileRange[0];
                    resetFileFetcher();
                    return true;
                }
                /**
                 * 如果位点已经超过了最大位点则将位点重置，并且重置读取的文件
                 */
                if (this.position > fileRange[1]) {
                    this.position = fileRange[1];
                    resetFileFetcher();
                    return false;
                }

                /**
                 * 如果位点还是在当前位点区间内,那可能是由于当前文件数据已经读取完了，则需要滚动到下一个文件
                 */
                if (this.position > fileRange[0] && this.position < fileRange[1]) {
                    resetFileFetcher();
                }

                return false;
            } else {
                if (this.callback != null) {
                    /**
                     * 当前文件的位点为 全局位点 - 当前文件开始的全局位点
                     */
                    long begin = position - this.fileFetcher.getBegin();
                    if (begin < 0) {
                        LOGGER.warn("push log data with a illegal start pos={}, file={}", begin, fileFetcher.getName());
                        return false;
                    }
                    boolean isSuccess = this.callback.call(this.fileFetcher.getFc(), begin, length, dataType, version);
                    if (isSuccess) {
                        /**
                         * 位点前进
                         */
                        position += length;
                        /**
                         * 如果推送不满最大推送字节数,则返回false,告诉外部调用可以休眠一会
                         */
                        if (length < ((Pradar.PUSH_MAX_SIZE * 2) / 3)) {
                            return false;
                        }
                        return true;
                    }
                    return false;
                }
                return false;
            }
        } catch (ClosedChannelException e) {
            try {
                resetFileFetcher();
            } catch (Throwable ex) {
            }
            return false;
        } catch (Throwable e) {
            return false;
        }
    }

    private File getTarget(String path, long begin) {
        File file = new File(path);
        final String fileName = file.getName();
        File dirFile = file.getParentFile();

        File[] files = dirFile.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.indexOf('.') == -1) {
                    return false;
                }
                String suffix = name.substring(name.lastIndexOf('.') + 1);
                return name.startsWith(fileName) && NumberUtils.isDigits(suffix);
            }
        });
        if (ArrayUtils.isEmpty(files)) {
            return null;
        }
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                long suffix1 = Long.valueOf(o1.getName().substring(o1.getName().lastIndexOf('.') + 1));
                long suffix2 = Long.valueOf(o2.getName().substring(o2.getName().lastIndexOf('.') + 1));
                return (int) (suffix1 - suffix2);
            }
        });

        File target = null;
        long lastFileSize = 0;
        for (File f : files) {
            long suffix = Long.valueOf(f.getName().substring(f.getName().lastIndexOf('.') + 1));
            if (suffix == begin) {
                return f;
            }
            //否则直接找下一个
            if (suffix > begin) {
                break;
            }
            lastFileSize = suffix;
            target = f;
        }
        if (lastFileSize + (target == null ? 0 : target.length()) < begin) {
            return null;
        }
        return target;
    }

    @Override
    public boolean isStarted() {
        return isStarting;
    }

    /**
     * 初始化位点信息
     *
     * @param idxFile
     */
    private void initPosition(File idxFile) {
        /**
         * 如果索引文件不存,则取当前文件范围的最小值
         */
        if (idxFile.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new java.io.FileReader(idxFile));
                String line = StringUtils.trim(reader.readLine());
                if (NumberUtils.isDigits(line)) {
                    this.position = Long.valueOf(line);
                }
            } catch (Throwable e) {
                LOGGER.error("read idx file {} err", idxFile.getAbsolutePath(), e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        LOGGER.error("close idx file {} err", idxFile.getAbsolutePath(), e);
                    }
                }
            }
        }

        long[] positionRange = getSizeRange();
        if (this.position < positionRange[0] || this.position > positionRange[1]) {
            this.position = positionRange[0];
        }
    }

    @Override
    public void stop() {
        isStarting = false;
        try {
            this.fileFetcher.close();
        } catch (IOException e) {
            LOGGER.error("fileFetcher close err! {} ", fileFetcher.getAbsolutePath(), e);
        }
        this.fileFetcher = null;
    }

    @Override
    public boolean isStoped() {
        return !isStarting;
    }

    @Override
    public long getPosition() {
        return position;
    }

    @Override
    public void savePosition() {
        String idxFilePath = getIdxPath();
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(idxFilePath));
            writer.println(position);
        } catch (IOException e) {
            LOGGER.error("save position index file err:{}", idxFilePath, e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Throwable e) {
                    LOGGER.error("close position index file err:{}", idxFilePath, e);
                }
            }
        }
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getIdxPath() {
        File file = new File(path);
        return new File(file.getParentFile(), file.getName() + ".idx").getAbsolutePath();
    }

    /**
     * 获取文件名中的位点范围
     *
     * @return
     */
    private long[] getFileRange() {
        try {
            File file = new File(path);
            final String fileName = file.getName();
            File dirFile = file.getParentFile();
            if (dirFile == null || !dirFile.exists()) {
                return ZERO;
            }

            File[] files = dirFile.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    if (name.indexOf('.') == -1) {
                        return false;
                    }
                    String suffix = name.substring(name.lastIndexOf('.') + 1);
                    return name.startsWith(fileName) && NumberUtils.isDigits(suffix);
                }
            });
            if (ArrayUtils.isEmpty(files)) {
                return ZERO;
            }
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    long suffix1 = Long.valueOf(o1.getName().substring(o1.getName().lastIndexOf('.') + 1));
                    long suffix2 = Long.valueOf(o2.getName().substring(o2.getName().lastIndexOf('.') + 1));
                    return (int) (suffix1 - suffix2);
                }
            });

            File first = files[0];
            File last = files[files.length - 1];
            long start = Long.valueOf(first.getName().substring(first.getName().lastIndexOf('.') + 1));
            long end = Long.valueOf(last.getName().substring(last.getName().lastIndexOf('.') + 1)) + last.length();
            return new long[]{start, end};
        } catch (Throwable e) {
            return ZERO;
        }
    }

    /**
     * 获取当前的文件的位点范围
     *
     * @return
     */
    private long[] getSizeRange() {
        try {
            File file = new File(path);
            final String fileName = file.getName();
            File dirFile = file.getParentFile();
            if (dirFile == null || !dirFile.exists()) {
                return ZERO;
            }

            File[] files = dirFile.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    if (name.indexOf('.') == -1) {
                        return false;
                    }
                    String suffix = name.substring(name.lastIndexOf('.') + 1);
                    return name.startsWith(fileName) && NumberUtils.isDigits(suffix);
                }
            });
            if (ArrayUtils.isEmpty(files)) {
                return ZERO;
            }
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    long suffix1 = Long.valueOf(o1.getName().substring(o1.getName().lastIndexOf('.') + 1));
                    long suffix2 = Long.valueOf(o2.getName().substring(o2.getName().lastIndexOf('.') + 1));
                    return (int) (suffix1 - suffix2);
                }
            });

            File first = files[0];
            File last = files[files.length - 1];
            long start = Long.valueOf(first.getName().substring(first.getName().lastIndexOf('.') + 1));
            long end = Long.valueOf(last.getName().substring(last.getName().lastIndexOf('.') + 1)) + last.length();
            return new long[]{start, end};
        } catch (Throwable e) {
            return ZERO;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultFileReader that = (DefaultFileReader) o;

        return path != null ? path.equals(that.path) : that.path == null;
    }

    @Override
    public int hashCode() {
        return path != null ? path.hashCode() : 0;
    }
}
