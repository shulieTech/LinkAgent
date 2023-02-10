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

import com.pamirs.attach.plugin.dynamic.Attachment;
import com.pamirs.attach.plugin.dynamic.ResourceManager;
import com.pamirs.attach.plugin.dynamic.template.RedisTemplate;
import com.pamirs.attach.plugin.lettuce.LettuceConstants;
import com.pamirs.attach.plugin.lettuce.utils.Version;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.ReflectionUtils;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @Auther: vernon
 * @Date: 2021/8/25 14:25
 * @Description:
 */
public class ConnectionInterceptor extends AroundInterceptor {

    Logger logger = LoggerFactory.getLogger(getClass());

    void config(Advice advice) {
        try {
            if (Pradar.isClusterTest() || Version.workWithSpringLettuce) {
                return;
            }
            Object t = advice.getTarget();
            String nodes = null;
            String password = null;
            Integer db = null;

            StringBuilder nodeBuilder = new StringBuilder();
            //版本问题 就不引用包了 全走反射
            if ("io.lettuce.core.masterreplica.MasterReplica".equals(advice.getTarget().getClass().getName())) {
                Boolean isSentinel = false;
                RedisURI redisUri = (RedisURI) advice.getParameterArray()[1];
                isSentinel = ReflectionUtils.invokeStatic(advice.getTargetClass(), "isSentinel", redisUri);
                if (isSentinel) {
                    List<String> indexes = new ArrayList<String>();
                    String masterId = redisUri.getSentinelMasterId();
                    List<RedisURI> sentinels = redisUri.getSentinels();
                    StringBuilder builder = new StringBuilder();
                    for (RedisURI sentinel : sentinels) {
                        password = sentinel.getPassword()
                                == null ? null : new String(sentinel.getPassword());
                        db = sentinel.getDatabase();
                        String node = sentinel.getHost().concat(":").concat(String.valueOf(sentinel.getPort()));
                        indexes.add(node);
                        builder.append(node)
                                .append(",");

                    }
                    nodes = builder.deleteCharAt(builder.length() - 1).toString();

                    ResourceManager.set(new Attachment(indexes,
                            LettuceConstants.MODULE_NAME, new String[]{LettuceConstants.MIDDLEWARE_NAME}
                            , new RedisTemplate.LettuceSentinelTemplate()
                            .setMaster(masterId)
                            .setDatabase(db)
                            .setNodes(nodes)
                            .setPassword(password)
                    ));

                }
            } else if (RedisClusterClient.class.isAssignableFrom(advice.getTarget().getClass())) {
                /**
                 * 集群模式
                 */
                RedisClusterClient client = (RedisClusterClient) t;
                Iterable<RedisURI> iterable = ReflectionUtils.get(client,"initialUris");
                Iterator iterator = iterable.iterator();

                List<String> indexes = new ArrayList<String>();


                while (iterator.hasNext()) {
                    RedisURI redisURI = (RedisURI) iterator.next();
                    String node = redisURI.getHost().concat(":").concat(String.valueOf(redisURI.getPort()));
                    indexes.add(node);
                    nodeBuilder.append(node)
                            .append(",");
                    password = redisURI.getPassword() == null ? null : new String(redisURI.getPassword());
                    db = redisURI.getDatabase();
                }
                nodes = nodeBuilder.deleteCharAt(nodeBuilder.length() - 1).toString();

                Attachment attachment = new Attachment(indexes, "redis-lettuce",
                        new String[]{"redis"},
                        new RedisTemplate.LettuceClusterTemplate()
                                .setDatabase(db)
                                .setNodes(nodes)
                                .setPassword(password));

                ResourceManager.set(attachment);

            } else if (RedisClient.class.isAssignableFrom(advice.getTarget().getClass())) {
                RedisClient client = (RedisClient) t;
                RedisURI redisURI = ReflectionUtils.get(client,"redisURI");
                nodes = redisURI.getHost().concat(":").concat(String.valueOf(redisURI.getPort()));
                String index = redisURI.getHost().concat(":").concat(String.valueOf(redisURI.getPort()));
                password = redisURI.getPassword() == null ? null : new String(redisURI.getPassword());
                db = redisURI.getDatabase();
                Attachment attachment = new Attachment(index, "redis-lettuce",
                        new String[]{"redis"},
                        new RedisTemplate.LettuceSingleTemplate()
                                .setDatabase(db)
                                .setNodes(nodes)
                                .setPassword(password));
                ResourceManager.set(attachment);
            }
        } catch (Throwable t) {
            logger.error(t.getMessage());
        }
    }

    @Override
    public void doAfter(Advice advice) throws Throwable {
        config(advice);
    }

    @Override
    public void doException(Advice advice) throws Throwable {
        config(advice);
    }
}
