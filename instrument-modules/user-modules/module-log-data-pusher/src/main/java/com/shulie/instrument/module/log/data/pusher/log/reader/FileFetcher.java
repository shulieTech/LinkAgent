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
package com.shulie.instrument.module.log.data.pusher.log.reader;


import org.apache.commons.lang.math.NumberUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 非线程安全的文件操作器
 *
 * @author pamirs
 */
public class FileFetcher {

    private final static int BUFFER_SIZE = 256;
    private final static char DEFAULT_SEPARATOR = '\n';
    private final FileChannel fc;
    private final char separator;
    private final File file;
    private final RandomAccessFile randomAccessFile;
    private ByteBuffer readBuffer;
    private volatile long readSize;
    private final long begin;
    private final static byte[] EMPTY = new byte[0];

    public FileFetcher(File file) throws FileNotFoundException {
        this(file, DEFAULT_SEPARATOR);
    }

    public FileFetcher(File file, char separator) throws FileNotFoundException {
        this.randomAccessFile = new RandomAccessFile(file, "r");
        this.fc = randomAccessFile.getChannel();
        this.file = file;
        this.separator = separator;
        this.readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        if (file.getName().indexOf('.') != -1) {
            String suffix = file.getName().substring(file.getName().lastIndexOf('.') + 1);
            if (NumberUtils.isDigits(suffix)) {
                this.begin = Long.valueOf(suffix);
            } else {
                this.begin = 0;
            }
        } else {
            this.begin = 0;
        }
    }

    public long getBegin() {
        return begin;
    }

    public String getName() {
        return file.getName();
    }

    public String getAbsolutePath() {
        return file.getAbsolutePath();
    }

    public FileChannel getFc() {
        return fc;
    }

    public File getFile() {
        return file;
    }

    /**
     * 读取最大 maxLength 的可读长度，有可能可读的数据比最大长度小，则返回可读长度
     * 也有可能比最大长度稍大一些，因为需要保证每次读取的日志的完整性
     *
     * @param begin     开始位置，这个位点则针对所有的文件，不是单个文件的位点
     * @param maxLength 最大读取的字节数
     * @return
     * @throws IOException
     */
    public long readAvailableLength(long begin, long maxLength) throws IOException {
        /**
         * 换算成当前文件的开始位点，如果发现需要读取位点已经超过该文件的范围则返回空
         */
        long start = begin - getBegin();
        /**
         * 要么不在当前文件中，要么是当前文件已经没有可读内容
         */
        if (start < 0) {
            return 0;
        }
        /**
         * 如果开始位置比当前文件长度还要大,则没有可读内容
         */
        if (start >= fc.size()) {
            return 0;
        }

        /**
         * 当前文件剩余可读取的字节数
         */
        long availableBytes = fc.size() - start;
        /**
         * 如果剩余可读字节小于等于最大长度,则直接返回剩余可读字节数
         */
        if (availableBytes <= maxLength) {
            return availableBytes;
        }

        //针对最大长度的偏移量
        int offset = 0;
        /**
         * 开始位置直接定位到 maxLength 的位置
         */
        long startPos = start + maxLength;
        while (true) {
            readBuffer.clear();
            /**
             * 先从最大长度位置开始定位往下找换行符
             */
            fc.position(startPos + offset);
            int bytesRead = fc.read(readBuffer);
            /**
             * 如果没有读出长度，则直接返回最大长度
             */
            if (bytesRead <= 0) {
                return maxLength + offset;
            }
            /**
             * 找换行符的下标
             */
            int enterIndex = enterOffset(readBuffer);
            if (enterIndex != -1) {
                return maxLength + offset + enterIndex;
            } else {
                offset += BUFFER_SIZE;
            }
        }
    }

    /**
     * 根据开始位置最大获取最大长度的数据，小于最大长度则获取可读取的数据
     * 也有可能比最大长度稍大一些，因为需要保证每次读取的日志的完整性
     *
     * @param begin     开始位置，这个位点则针对所有的文件，不是单个文件的位点
     * @param maxLength 最大读取的字节数
     * @return
     * @throws IOException
     */
    public byte[] read(long begin, long maxLength) throws IOException {
        /**
         * 换算成当前文件的开始位点，如果发现需要读取位点已经超过该文件的范围则返回空
         */
        long start = begin - getBegin();
        if (start < 0) {
            return EMPTY;
        }
        if (start > fc.size()) {
            return EMPTY;
        }
        /**
         * 当前文件剩余可读取的字节数
         */
        long availableBytes = fc.size() - start;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int offset = 0;
        while (true) {
            readBuffer.clear();
            /**
             * 定位开始位点
             */
            fc.position(start + offset);
            int bytesRead = fc.read(readBuffer);
            if (bytesRead <= 0) {
                break;
            }

            /**
             * 如果读取已经读取到最大长度
             */
            if (offset >= maxLength) {
                /**
                 * 找到换行符
                 */
                int enterIndex = enterOffset(readBuffer);
                if (enterIndex != -1) {
                    readBuffer.flip();
                    byte[] data = new byte[enterIndex + 1];
                    readBuffer.get(data, 0, enterIndex + 1);
                    bos.write(data, 0, data.length);
                    break;
                } else {
                    readBuffer.flip();
                    int length = readBuffer.limit();
                    byte[] data = new byte[length];
                    readBuffer.get(data, 0, length);
                    bos.write(data, 0, data.length);
                }
            } else if (offset >= availableBytes) {
                readBuffer.flip();
                int length = readBuffer.limit();
                byte[] data = new byte[length];
                readBuffer.get(data, 0, length);
                bos.write(data, 0, data.length);
                break;
            } else {
                readBuffer.flip();
                int length = readBuffer.limit();
                byte[] data = new byte[length];
                readBuffer.get(data, 0, length);
                bos.write(data, 0, data.length);
            }
            offset += BUFFER_SIZE;
        }
        byte[] data = bos.toByteArray();
        if (data.length == 0) {
            return data;
        }
        if (data[data.length - 1] == (byte) separator) {
            return data;
        }

        int lastIndex = -1;
        for (int i = data.length - 1; i >= 0; i--) {
            if (data[i] == (byte) separator) {
                lastIndex = i;
                break;
            }
        }
        /**
         * 如果未找到换行符,则返回空，说明数据并没有写完
         * 防止将非一行的数据读取给到服务端造成数据解析错误
         */
        if (lastIndex == -1) {
            return EMPTY;
        }

        byte[] newData = new byte[lastIndex + 1];
        System.arraycopy(data, 0, newData, 0, lastIndex + 1);
        return newData;
    }

    public void close() throws IOException {
        fc.close();
        if (randomAccessFile != null) {
            randomAccessFile.close();
        }
    }

    /**
     * 定位换行符的位置，返回偏移量
     *
     * @param buffer
     * @return
     */
    private int enterOffset(ByteBuffer buffer) {
        buffer.flip();
        byte[] data = new byte[buffer.limit()];
        buffer.get(data, 0, buffer.limit());
        int len = data.length;
        for (int i = 0; i < len; i++) {
            if (data[i] == (byte) separator) {
                return i;
            }
        }
        return -1;
    }

    public void readSize(long readSize) {
        this.readSize += readSize;
    }

    public long readSize() {
        return readSize;
    }
}
