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
package com.shulie.instrument.simulator.api.event;

import com.shulie.instrument.simulator.api.ProcessControlException;

/**
 * 事件枚举类型
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/10/23 10:45 下午
 */
public abstract class EventType {
    /**
     * 加载事件
     */
    public final static int LOAD = 1;

    /**
     * 调用:BEFORE
     */
    public final static int BEFORE = 2;

    /**
     * 调用:RETURN
     */
    public final static int RETURN = 3;

    /**
     * 调用:THROWS
     */
    public final static int THROWS = 4;

    /**
     * 调用:LINE
     * 一行被调用了
     */
    public final static int LINE = 5;

    /**
     * 调用:CALL_BEFORE
     * 一个方法被调用之前
     */
    public final static int CALL_BEFORE = 6;

    /**
     * 调用:CALL_RETURN
     * 一个方法被调用正常返回之后
     */
    public final static int CALL_RETURN = 7;

    /**
     * 调用:CALL_THROWS
     * 一个方法被调用抛出异常之后
     */
    public final static int CALL_THROWS = 8;


    /**
     * 立即调用:RETURN
     * 由{@link ProcessControlException#throwReturnImmediately(Object)}触发
     */
    public final static int IMMEDIATELY_RETURN = 9;

    /**
     * 立即调用:THROWS
     * 由{@link ProcessControlException#throwThrowsImmediately(Throwable)}触发
     */
    public final static int IMMEDIATELY_THROWS = 10;

    /**
     * 空类型
     */
    public final static Integer[] EMPTY = new Integer[0];

    public static String name(int type) {
        switch (type) {
            case LOAD:
                return "LOAD";
            case BEFORE:
                return "BEFORE";
            case RETURN:
                return "RETURN";
            case THROWS:
                return "THROWS";
            case LINE:
                return "LINE";
            case CALL_BEFORE:
                return "CALL_BEFORE";
            case CALL_RETURN:
                return "CALL_RETURN";
            case CALL_THROWS:
                return "CALL_THROWS";
            case IMMEDIATELY_RETURN:
                return "IMMEDIATELY_RETURN";
            case IMMEDIATELY_THROWS:
                return "IMMEDIATELY_THROWS";

        }
        return "UNKNOW";
    }
}
