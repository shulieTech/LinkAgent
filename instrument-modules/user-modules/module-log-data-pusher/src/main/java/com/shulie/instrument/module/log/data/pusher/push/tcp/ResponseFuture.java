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
package com.shulie.instrument.module.log.data.pusher.push.tcp;

/**
 * @author xiaobin.zfb
 * @since 2020/8/14 11:08 上午
 */
public class ResponseFuture<T> {
    private T resp;
    private boolean isOK = false;

    public synchronized T get() {
        while (!isOK) {
            try {
                this.wait();
            } catch (InterruptedException e) {
            }
        }
        return resp;
    }

    public synchronized void set(T resp) {
        this.resp = resp;
        this.isOK = true;
        this.notifyAll();
    }
}
