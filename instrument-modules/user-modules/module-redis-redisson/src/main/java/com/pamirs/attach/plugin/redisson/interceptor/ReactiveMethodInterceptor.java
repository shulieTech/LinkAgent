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
package com.pamirs.attach.plugin.redisson.interceptor;

import com.pamirs.attach.plugin.redisson.RedissonConstants;
import com.pamirs.attach.plugin.redisson.destroy.RedissonDestroy;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/9/8 11:30 上午
 */
@Destroyable(RedissonDestroy.class)
public class ReactiveMethodInterceptor extends TraceInterceptorAdaptor {
    @Override
    public String getPluginName() {
        return RedissonConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return RedissonConstants.PLUGIN_TYPE;
    }
}
