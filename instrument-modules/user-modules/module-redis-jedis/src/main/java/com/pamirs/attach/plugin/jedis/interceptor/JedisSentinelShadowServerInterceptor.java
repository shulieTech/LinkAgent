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
package com.pamirs.attach.plugin.jedis.interceptor;

import com.pamirs.attach.plugin.jedis.destroy.JedisDestroyed;
import com.pamirs.attach.plugin.jedis.shadowserver.JedisSentinelFactory;
import com.pamirs.attach.plugin.jedis.util.Model;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.Throwables;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;


/**
 * @Auther: vernon
 * @Date: 2021/9/10 00:12
 * @Description:
 */
@Destroyable(JedisDestroyed.class)
public class JedisSentinelShadowServerInterceptor extends CutoffInterceptorAdaptor {

    Model model = new Model();
    private static final Logger LOGGER = LoggerFactory.getLogger(JedisSentinelShadowServerInterceptor.class.getName());

    @Override
    public CutOffResult cutoff0(Advice advice) {
        if (!Pradar.isClusterTest()) {
            return CutOffResult.PASSED;
        }
        if (!model.isSentinelMode(advice.getTarget())) {
            return CutOffResult.PASSED;
        }
        Object target = advice.getTarget();
        try {
            final Object object = JedisSentinelFactory.getFactory().getClient(target);
            if(!(object instanceof JedisSentinelPool)){
                return CutOffResult.PASSED;
            }
            JedisSentinelPool pool = (JedisSentinelPool) object;
            Jedis client = pool.getResource();
            return CutOffResult.cutoff(Reflect.on(client)
                    .call(advice.getBehavior().getName()
                            , advice.getParameterArray()).get());
        } catch (PressureMeasureError e) {
            throw e;
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            if (Pradar.isClusterTest()) {
                throw new PressureMeasureError(Throwables.getStackTraceAsString(e));
            }

        }
        return CutOffResult.passed();
    }


}
