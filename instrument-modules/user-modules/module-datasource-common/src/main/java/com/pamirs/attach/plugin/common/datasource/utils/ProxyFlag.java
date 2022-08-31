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

package com.pamirs.attach.plugin.common.datasource.utils;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/31 16:08
 */
public class ProxyFlag {

    private final static ThreadLocal<Boolean> inProxy = new ThreadLocal();

    /**
     * 进入代理
     */
    public static void enter() {
        inProxy.set(true);
    }

    /**
     * 退出代理
     */
    public static void exit() {
        inProxy.remove();
    }

    /**
     * 是否处于代理中
     *
     * @return true or false
     */
    public static boolean inProxy() {
        return inProxy.get() != null && inProxy.get();
    }

}
