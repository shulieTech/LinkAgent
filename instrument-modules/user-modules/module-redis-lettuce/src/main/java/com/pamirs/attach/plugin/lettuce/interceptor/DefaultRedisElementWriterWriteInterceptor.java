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
package com.pamirs.attach.plugin.lettuce.interceptor;

import com.pamirs.attach.plugin.lettuce.utils.RedisSerializerHolder;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * @author angju
 * @date 2021/9/7 15:41
 */
public class DefaultRedisElementWriterWriteInterceptor extends ParametersWrapperInterceptorAdaptor {
    @Override
    protected Object[] getParameter0(Advice advice) throws Throwable {
        if (RedisSerializerHolder.redisSerializer == null){
            synchronized (RedisSerializerHolder.class){
                if (RedisSerializerHolder.redisSerializer == null){
                    RedisSerializerHolder.redisSerializer = (RedisSerializer)advice.getTarget().getClass().getDeclaredField("serializer").get(advice.getTarget());
                }
            }
        }
        return advice.getParameterArray();
    }
}
