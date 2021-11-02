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
package com.pamirs.attach.plugin.okhttp.v3;

import okhttp3.Headers;
import okhttp3.internal.http.RealResponseBody;
import okio.BufferedSource;

import java.lang.reflect.Constructor;


/**
 * @Auther: vernon
 * @Date: 2021/7/14 11:18
 * @Description:
 */
public class Version {


    public static Constructor constructor;

    public static boolean isOverload_3_10_0;


    static {
        isOverload_3_10_0 = isOverload_3_10_0();
        constructor = constructor();
    }

    /**
     * 是否3.10.0以上版本
     *
     * @return
     */
    public static boolean isOverload_3_10_0() {
        try {
            RealResponseBody.class.getDeclaredConstructor(String.class, long.class, BufferedSource.class);
            return Boolean.TRUE;
        } catch (NoSuchMethodException e) {
            return Boolean.FALSE;
        }
    }

    public static Constructor constructor() {
        if (isOverload_3_10_0) {
            try {
                return RealResponseBody.class.getDeclaredConstructor(String.class, long.class, BufferedSource.class);
            } catch (NoSuchMethodException e) {
            }
        }
        try {
            return RealResponseBody.class.getDeclaredConstructor(Headers.class, BufferedSource.class);
        } catch (NoSuchMethodException e) {

        }
        return null;
    }
}
