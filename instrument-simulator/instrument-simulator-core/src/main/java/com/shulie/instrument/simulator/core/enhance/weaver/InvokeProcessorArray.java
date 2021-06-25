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
package com.shulie.instrument.simulator.core.enhance.weaver;

import java.util.Arrays;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/6/1 11:04 下午
 */
public class InvokeProcessorArray {

    private InvokeProcessor[] processors;
    private volatile int length;
    /**
     * record assignment index of map
     */
    private byte[] idxMap;

    public InvokeProcessorArray(int length) {
        this.processors = new InvokeProcessor[length];
        this.idxMap = new byte[length];
    }

    /**
     * find empty index
     *
     * @return
     */
    public synchronized int getNextListenerId() {
        int index = -1;
        for (int i = 0, len = processors.length; i < len; i++) {
            if (processors[i] == null && idxMap[i] == 0) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            ensureCapacity(processors.length);
            index = processors.length;
        }

        idxMap[index] = 1;
        return index;
    }

    public synchronized void set(int index, InvokeProcessor processor) {
        ensureCapacity(index);
        processors[index] = processor;
        idxMap[index] = 1;
        length++;
    }

    private void ensureCapacity(int index) {
        if (index >= processors.length) {
            int oldCapacity = processors.length;
            int newCapacity = oldCapacity + (oldCapacity >> 1);
            processors = Arrays.copyOf(processors, newCapacity);
            idxMap = Arrays.copyOf(idxMap, newCapacity);
        }
    }

    public InvokeProcessor get(int index) {
        if (index >= processors.length) {
            return null;
        }
        return processors[index];
    }

    public synchronized InvokeProcessor remove(int index) {
        if (index >= processors.length) {
            return null;
        }
        InvokeProcessor invokeProcessor = processors[index];
        processors[index] = null;
        idxMap[index] = 0;
        return invokeProcessor;
    }

    public int length() {
        return length;
    }
}
