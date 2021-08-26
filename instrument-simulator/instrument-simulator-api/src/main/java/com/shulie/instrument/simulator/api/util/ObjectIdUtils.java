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
package com.shulie.instrument.simulator.api.util;

/**
 * 给指定的Java对象分配JVM唯一ID
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/10/23 10:45 下午
 */
public class ObjectIdUtils {

    /**
     * 映射Java对象为对象ID(JVM唯一)
     *
     * @param object 待映射的Java对象
     * @return 对象ID
     */
    public static int identity(final Object object) {
        if (object == null) {
            return 0;
        }
        return System.identityHashCode(object);
    }

}
