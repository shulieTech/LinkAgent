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

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/24 13:40
 */
public class ClassUtil {


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
}
