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
package com.pamirs.attach.plugin.caffeine.utils;

import java.util.function.BiFunction;

import com.pamirs.pradar.cache.ClusterTestCacheWrapperKey;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/03/30 8:16 下午
 */
public class WrapBiFunction implements WrapLambda, BiFunction {

    private final BiFunction function;

    public WrapBiFunction(BiFunction function) {this.function = function;}

    @Override
    public Object apply(Object o, Object o2) {
        Object param = (o instanceof ClusterTestCacheWrapperKey) ?
            ((ClusterTestCacheWrapperKey)o).getKey() : o;
        return this.function.apply(param, o2);
    }
}
