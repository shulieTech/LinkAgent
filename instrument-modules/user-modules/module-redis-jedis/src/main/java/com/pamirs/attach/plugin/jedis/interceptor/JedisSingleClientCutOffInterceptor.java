/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.jedis.interceptor;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.attach.plugin.jedis.shadowserver.JedisFactory;
import com.pamirs.attach.plugin.jedis.util.JedisConstant;
import com.pamirs.attach.plugin.jedis.util.Model;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.ThrowableUtils;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        String className = advice.getTargetClass().getName();

        if (JedisConstant.CONNECTION.equals(className)) {
            if (!GlobalConfig.getInstance().isShadowDbRedisServer()) {
                return CutOffResult.PASSED;
            } else {
                String msg = "jedis connection直接调用不支持影子库模式";
                ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.RedisServer)
                        .setErrorCode("redis-jedis-0002")
                        .setMessage(msg)
                        .report();
                throw new PressureMeasureError(msg);
            }
        }

        if (ignore(advice)) {
            return CutOffResult.PASSED;
        }

        if (JedisConstant.JEDIS.equals(className)
                || JedisConstant.BINARY_JEDIS.equals(className)) {

            try {
                Object t = ReflectionUtils.invoke(JedisFactory.getFactory().getClient(advice.getTarget()), advice.getBehavior().getName(), advice.getParameterArray());
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
