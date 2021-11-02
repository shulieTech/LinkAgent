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

import com.pamirs.attach.plugin.jedis.shadowserver.JedisFactory;
import com.pamirs.attach.plugin.jedis.util.JedisConstant;
import com.pamirs.attach.plugin.jedis.util.Model;
import com.pamirs.attach.plugin.jedis.util.RedisUtils;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.shulie.instrument.simulator.api.ThrowableUtils;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Client;

import java.util.HashSet;
import java.util.Set;

/**
 * @Auther: vernon
 * @Date: 2021/8/30 14:41
 * @Description:jedis单节点路由器
 */
public class JedisSingleClientCutOffInterceptor extends CutoffInterceptorAdaptor {
    Logger logger = LoggerFactory.getLogger(getClass());
    Model model = Model.INSTANCE();

    /**
     * target只能是redis.clients.jedis.BinaryJedis或者redis.clients.jedis.Jedis
     *
     * @param advice
     * @return
     * @throws Throwable
     */
    @Override
    public CutOffResult cutoff0(Advice advice) throws Throwable {
        return process(advice);
    }


    CutOffResult process(Advice advice) {

        if (ignore(advice)) {
            return CutOffResult.PASSED;
        }

        String className = advice.getTargetClass().getName();
        if (JedisConstant.JEDIS.equals(className)
                || JedisConstant.BINARY_JEDIS.equals(className)) {

            try {
                Object t = Reflect.on(
                        JedisFactory.getFactory().getClient(advice.getTarget())
                ).call(
                        advice.getBehavior().getName()
                        , advice.getParameterArray()
                ).get();
                return CutOffResult.cutoff(t);
            } catch (Throwable t) {
                logger.error(ThrowableUtils.toString(t));
                ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.RedisServer)
                        .setErrorCode("redis-jedis-0001")
                        .setMessage("jedis影子库调用失败！")
                        .setDetail(ThrowableUtils.toString(t)).report();
                throw new PressureMeasureError(t);
            }
        } else {
            return CutOffResult.PASSED;
        }

    }


    boolean ignore(Advice advice) {
        //非压测流量ignore
        if (!Pradar.isClusterTest()) {
            return true;
        }

        //select 不用管了
        if ("select".equals(advice.getBehaviorName())) {
            return true;
        }

        //非单机模式ignore

        return !model.isSingleMode(advice.getTarget());


    }
}
