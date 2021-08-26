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
package com.pamirs.attach.plugin.shadowjob.common.lts;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author angju
 * @date 2020/7/20 17:51
 */
public final class JobRunnerShadowRegistry {
    private static Set<String> shardValues = new HashSet<String>();

    public static synchronized void addShardValue(String shardValue) {
        shardValues.add(shardValue);
    }

    public static synchronized void remove(String shardValue) {
        shardValues.remove(shardValue);
    }

    public static Collection<String> getShardValues() {
        return Collections.unmodifiableSet(shardValues);
    }

    public static boolean isShadowJob(String shardValue) {
        return shardValues.contains(shardValue);
    }

    public static void release() {
        if (shardValues != null) {
            shardValues.clear();
            shardValues = null;
        }
    }
}
