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
