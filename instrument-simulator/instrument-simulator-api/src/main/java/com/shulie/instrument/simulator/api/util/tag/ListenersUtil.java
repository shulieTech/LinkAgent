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
package com.shulie.instrument.simulator.api.util.tag;

/**
 * @author Licey
 * @date 2021/9/26
 */
public class ListenersUtil {
    public static final int FILTER_CLUSTER_TEST_INDEX = 1;
    public static final int FILTER_BUSINESS_DATA_INDEX = 2;
    public static final int FILTER_NO_SILENCE_INDEX = 3;

    public static boolean isFilterClusterTest(int data) {
        return TagUtil.checkTag(FILTER_CLUSTER_TEST_INDEX, data);
    }

    public static boolean isFilterBusinessData(int data) {
        return TagUtil.checkTag(FILTER_BUSINESS_DATA_INDEX, data);
    }

    public static boolean isNoSilence(int data) {
        return TagUtil.checkTag(FILTER_NO_SILENCE_INDEX, data);
    }
}
