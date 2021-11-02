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
package com.shulie.instrument.simulator.core.enhance.weaver.asm;

import com.shulie.instrument.simulator.message.Messager;
import org.objectweb.asm.commons.Method;

/**
 * 常用的ASM method 集合
 * 省得我到处声明
 */
public interface AsmMethods {

    /**
     * asm method of {@link Messager#invokeOnBefore(Object[], int, String, int, int, Class, String, String, Object)}
     */
    Method MESSAGER_INVOKE_ON_BEFORE = AsmMethodHelper.getAsmMethod(
            Messager.class,
            "invokeOnBefore",
            Object[].class, int.class, String.class, int.class, int.class, Class.class, String.class, String.class, Object.class
    );

    /**
     * asm method of {@link Messager#invokeOnReturn(Object, Class, int, String, int, int)}
     */
    Method MESSAGER_INVOKE_ON_RETURN = AsmMethodHelper.getAsmMethod(
            Messager.class,
            "invokeOnReturn",
            Object.class, Class.class, int.class, String.class, int.class, int.class
    );

    /**
     * asm method of {@link Messager#invokeOnThrows(Throwable, Class, int, String, int, int)}
     */
    Method MESSAGER_INVOKE_ON_THROWS = AsmMethodHelper.getAsmMethod(
            Messager.class,
            "invokeOnThrows",
            Throwable.class, Class.class, int.class, String.class, int.class, int.class
    );


    /**
     * asm method of {@link Messager#invokeOnLine(int, Class, int, String, int, int)}
     */
    Method MESSAGER_INVOKE_ON_LINE = AsmMethodHelper.getAsmMethod(
            Messager.class,
            "invokeOnLine",
            int.class, Class.class, int.class, String.class, int.class, int.class
    );

    /**
     * asm method of {@link Messager#invokeOnCallBefore(int, boolean, String, String, String, Class, int, String, int, int)}
     */
    Method MESSAGER_INVOKE_ON_CALL_BEFORE = AsmMethodHelper.getAsmMethod(
            Messager.class,
            "invokeOnCallBefore",
            int.class, boolean.class, String.class, String.class, String.class, Class.class, int.class, String.class, int.class, int.class
    );

    /**
     * asm method of {@link Messager#invokeOnCallReturn(boolean, Class, int, String, int, int)}
     */
    Method MESSAGER_INVOKE_ON_CALL_RETURN = AsmMethodHelper.getAsmMethod(
            Messager.class,
            "invokeOnCallReturn",
            boolean.class, Class.class, int.class, String.class, int.class, int.class
    );

    /**
     * asm method of {@link Messager#invokeOnCallThrows(Throwable, boolean, Class, int, String, int, int)}
     */
    Method MESSAGER_INVOKE_ON_CALL_THROWS = AsmMethodHelper.getAsmMethod(
            Messager.class,
            "invokeOnCallThrows",
            Throwable.class, boolean.class, Class.class, int.class, String.class, int.class, int.class
    );

    /**
     * asm method of {@link Messager#getExecutionTag(int)}}
     */
    Method GET_EXECUTION_TAG = AsmMethodHelper.getAsmMethod(
            Messager.class,
            "getExecutionTag",
            int.class
    );
}
