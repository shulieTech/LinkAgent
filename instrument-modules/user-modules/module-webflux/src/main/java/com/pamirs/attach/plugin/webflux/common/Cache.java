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
package com.pamirs.attach.plugin.webflux.common;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * @Auther: vernon
 * @Date: 2021/1/11 23:28
 * @Description:
 */
public class Cache<T> {

    static TransmittableThreadLocal threadLocal = new TransmittableThreadLocal();

    static public class RequestHolder {
        static public void set(Object request) {
            threadLocal.set(request);
        }

        static public void remove() {
            threadLocal.remove();
        }

        static public Object get() {
            return threadLocal.get();
        }
    }
}
