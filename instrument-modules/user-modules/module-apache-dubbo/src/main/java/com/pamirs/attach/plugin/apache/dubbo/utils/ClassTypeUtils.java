/*
 * *
 *  * Copyright 2021 Shulie Technology, Co.Ltd
 *  * Email: shulie@shulie.io
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.pamirs.attach.plugin.apache.dubbo.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author angju
 * @date 2022/2/21 20:05
 */
public class ClassTypeUtils {
    private static Map<String, Integer> type2Code = new HashMap<String, Integer>();
    public static final int BOOLEAN = 1;
    public static final int INT = 2;
    public static final int LONG = 3;
    public static final int STRING = 4;
    public static final int SHORT = 5;
    public static final int FLOAT = 6;
    public static final int DOUBLE = 7;

    static {
        type2Code.put("boolean", BOOLEAN);
        type2Code.put("int", INT);
        type2Code.put("long", LONG);
        type2Code.put("String", STRING);
        type2Code.put("short", SHORT);
        type2Code.put("float", FLOAT);
        type2Code.put("double", DOUBLE);
    }

    public static Map<String, Integer> getType2Code() {
        return type2Code;
    }


}
