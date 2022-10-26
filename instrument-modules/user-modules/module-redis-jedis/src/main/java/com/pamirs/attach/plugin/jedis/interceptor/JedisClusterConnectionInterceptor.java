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

import com.pamirs.attach.plugin.dynamic.Attachment;
import com.pamirs.attach.plugin.dynamic.ResourceManager;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.attach.plugin.dynamic.template.RedisTemplate;
import com.pamirs.attach.plugin.jedis.RedisConstants;
import com.pamirs.attach.plugin.jedis.destroy.JedisDestroyed;
import com.pamirs.attach.plugin.jedis.shadowserver.JedisClusterFactory;
import com.pamirs.attach.plugin.jedis.util.Model;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.Throwables;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisClusterInfoCache;
import redis.clients.jedis.JedisSlotBasedConnectionHandler;

import java.util.ArrayList;
import java.util.Map;

/**
 * @Author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @package: com.pamirs.attach.plugin.jedis.interceptor
 * @Date 2020/12/2 4:43 下午
 */
@Destroyable(JedisDestroyed.class)
public class JedisClusterConnectionInterceptor extends CutoffInterceptorAdaptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JedisClusterConnectionInterceptor.class.getName());
    Model model = Model.INSTANCE();

    void attachment(Advice advice) {
        try {
            if (Pradar.isClusterTest()) {
                return;
            }
            JedisSlotBasedConnectionHandler handler = (JedisSlotBasedConnectionHandler) advice.getTarget();
            JedisClusterInfoCache cache = ReflectionUtils.get(handler, "cache");
            String password = ReflectionUtils.get(cache, "password");
            Map hashmap = ReflectionUtils.get(cache, "nodes");

            StringBuilder builder = new StringBuilder();
            for (Object node : hashmap.keySet()) {
                model.setClusterMode(String.valueOf(node));
                builder.append(node)
                        .append(",");
            }
            builder.deleteCharAt(builder.length() - 1);
            ResourceManager.set(new Attachment(new ArrayList(hashmap.keySet()), RedisConstants.PLUGIN_NAME,
                    new String[]{RedisConstants.MIDDLEWARE_NAME}
                    , new RedisTemplate.JedisClusterTemplate()
                    .setNodes(builder.toString())
                    .setPassword(password)
            ));
        } catch (Throwable t) {

        }
    }

    @Override
    public CutOffResult cutoff0(Advice advice) {
        Object[] args = advice.getParameterArray();
        attachment(advice);
        Object target = advice.getTarget();
        try {
            JedisSlotBasedConnectionHandler client = (JedisSlotBasedConnectionHandler) JedisClusterFactory.getFactory().getClient(target);
            if (null != args && args.length == 1) {
                return CutOffResult.cutoff(client.getConnectionFromSlot((Integer) args[0]));
            } else {
                return CutOffResult.cutoff(client.getConnection());
            }
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
