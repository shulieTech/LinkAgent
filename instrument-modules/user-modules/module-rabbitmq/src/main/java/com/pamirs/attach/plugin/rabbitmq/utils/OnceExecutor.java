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
package com.pamirs.attach.plugin.rabbitmq.utils;

import java.util.HashSet;
import java.util.Set;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/11/18 5:01 下午
 */
public class OnceExecutor {

    private static final Set<Object> CACHE = new HashSet<Object>();

    public static <T> void execute(T key, Consumer<T> consumer) {
        if (!CACHE.contains(key)) {
            synchronized (OnceExecutor.class) {
                if (!CACHE.contains(key)) {
                    consumer.accept(key);
                    CACHE.add(key);
                }
            }
        }
    }

    public interface Consumer<T> {
        void accept(T key);
    }

    public static void clear() {
        CACHE.clear();
    }

}
