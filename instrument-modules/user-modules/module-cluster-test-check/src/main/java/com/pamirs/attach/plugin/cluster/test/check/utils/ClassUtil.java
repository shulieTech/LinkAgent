/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pamirs.attach.plugin.cluster.test.check.utils;

import com.google.common.collect.HashBasedTable;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/24 13:40
 */
public class ClassUtil {

    private static HashBasedTable<String, String, Boolean> classInstancesCache = HashBasedTable.create();


    /**
     * 判读当前对象是否指定class的实例
     *
     * @param obj       对象
     * @param className 类名
     * @return true 属于，false 不属于
     */
    public static boolean instanceOf(Object obj, String className) {
        Class clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            return false;
        }
        return clazz.isInstance(obj);
    }

    public static boolean isInstances(Object obj, String className) {
        Class<?> clazz = obj.getClass();
        Boolean aBoolean = classInstancesCache.get(clazz.getName(), className);
        if (aBoolean != null) {
            return aBoolean;
        }

        Class<?> aClass = clazz;
        if (aClass.getName().equals(className)) {
            return true;
        }

        Class<?>[] interfaces = aClass.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            if (isInterfaceImpl(interfaces[i], className)) {
                classInstancesCache.put(aClass.getName(), className, true);
                return true;
            }
        }

        Class<?> superclass = clazz.getSuperclass();

        return true;
    }

    private static boolean isSuperClass(Class<?> clazz, String className) {
        if (clazz.getName().equals(className)) {
            return true;
        }
        if (clazz.getName().equals("java.lang.Object")) {
            return false;
        }
        Class<?>[] interfaces = clazz.getInterfaces();
        for (Class<?> anInterface : interfaces) {
            if (isInterfaceImpl(anInterface, className)) {
                return true;
            }
        }
        return isSuperClass(clazz.getSuperclass(), className);
    }

    private static boolean isInterfaceImpl(Class<?> clazz, String className) {
        Class<?>[] interfaces = clazz.getInterfaces();
        if (interfaces == null || interfaces.length == 0) {
            return false;
        }
        for (int i = 0; i < interfaces.length; i++) {
            Class<?> aClass = interfaces[i];
            if (aClass.getName().equals(className)) {
                return true;
            }
            boolean isInstances = isInterfaceImpl(aClass, className);
            if (isInstances) {
                return true;
            }
        }
        return false;
    }

}
