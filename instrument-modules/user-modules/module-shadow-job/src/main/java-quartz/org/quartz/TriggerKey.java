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
package org.quartz;

import org.quartz.utils.Key;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/7/23 8:27 下午
 */
public final class TriggerKey extends Key<TriggerKey> {

    private static final long serialVersionUID = 8070357886703449660L;

    public TriggerKey(String name) {
        super(name, null);
    }

    public TriggerKey(String name, String group) {
        super(name, group);
    }

    public static TriggerKey triggerKey(String name) {
        return new TriggerKey(name, null);
    }

    public static TriggerKey triggerKey(String name, String group) {
        return new TriggerKey(name, group);
    }

}
