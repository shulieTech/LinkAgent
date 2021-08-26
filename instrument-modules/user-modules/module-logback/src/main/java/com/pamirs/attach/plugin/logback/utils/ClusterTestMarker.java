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
package com.pamirs.attach.plugin.logback.utils;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/06/22 2:49 下午
 */
public class ClusterTestMarker {

    private static ThreadLocal<Boolean> clusterTestThreadLocal = new ThreadLocal<Boolean>();

    public static void mark(boolean isClusterTest) {
        clusterTestThreadLocal.set(isClusterTest);
    }

    public static boolean isClusterTestThenClear() {
        Boolean result = clusterTestThreadLocal.get();
        clusterTestThreadLocal.remove();
        return result != null && result;
    }

    public static void release() {
        clusterTestThreadLocal = null;
    }
}
