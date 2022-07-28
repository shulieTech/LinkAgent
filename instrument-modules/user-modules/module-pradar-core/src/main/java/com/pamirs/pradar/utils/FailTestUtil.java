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

package com.pamirs.pradar.utils;

import com.pamirs.pradar.exception.PressureMeasureError;
import com.shulie.instrument.simulator.api.util.StringUtil;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/7/28 16:16
 */
public class FailTestUtil {

    /**
     * 主动抛出error异常，中断探针逻辑，进行测试
     */
    public static void failTest() {
        String enable = System.getProperty("pradar.agent.fail.test.enable");
        if (StringUtil.isEmpty(enable) || !"true".equals(enable)) {
            return;
        }
        throw new PressureMeasureError("this is a fail test");
    }
}
