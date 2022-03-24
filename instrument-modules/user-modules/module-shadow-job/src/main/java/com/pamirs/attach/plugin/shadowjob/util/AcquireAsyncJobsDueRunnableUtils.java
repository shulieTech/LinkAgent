package com.pamirs.attach.plugin.shadowjob.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author angju
 * @date 2021/10/14 10:26
 */
public class AcquireAsyncJobsDueRunnableUtils {
    private static ConcurrentHashMap<Integer, Integer> concurrentHashMap = new ConcurrentHashMap<Integer, Integer>(16, 1);


    public static boolean inited(Object target){
        Object old = concurrentHashMap.putIfAbsent(System.identityHashCode(target), System.identityHashCode(target));
        return old != null;
    }

    public static void setValue(Object key, Object value){
        concurrentHashMap.put(System.identityHashCode(key), System.identityHashCode(value));
    }

    public static boolean ptInited(Object target){
        return concurrentHashMap.containsValue(System.identityHashCode(target));
    }
}
