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

import java.util.Map;
import java.util.Map.Entry;

import com.pamirs.pradar.cache.ClusterTestCacheWrapperKey;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/03/23 3:36 下午
 */
public class WrapEntry implements Map.Entry {

    private final Map.Entry entry;

    public WrapEntry(Entry entry) {this.entry = entry;}

    @Override
    public Object getKey() {
        return this.entry.getKey() instanceof ClusterTestCacheWrapperKey ?
            ((ClusterTestCacheWrapperKey)this.entry.getKey()).getKey() : this.entry.getKey();
    }

    @Override
    public Object getValue() {
        return this.entry.getValue();
    }

    @Override
    public Object setValue(Object value) {
        return this.entry.setValue(value);
    }
}
