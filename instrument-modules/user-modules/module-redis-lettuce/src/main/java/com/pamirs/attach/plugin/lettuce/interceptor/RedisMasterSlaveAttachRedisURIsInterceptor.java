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
import com.pamirs.attach.plugin.lettuce.destroy.LettuceDestroy;
import com.pamirs.attach.plugin.lettuce.utils.LettuceUtils;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.ThrowableUtils;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import com.shulie.instrument.simulator.api.util.StringUtil;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @package: com.pamirs.attach.plugin.lettuce.interceptor
 * @Date 2020/12/7 9:50 下午
 */
@Destroyable(LettuceDestroy.class)
public class RedisMasterSlaveAttachRedisURIsInterceptor extends AroundInterceptor {
    Logger logger = LoggerFactory.getLogger(getClass());
    @Resource
    protected DynamicFieldManager manager;

    @Override
    public void doBefore(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (!(args[0] instanceof RedisClient)) {
            return;
        }
        /* LettuceConstants.masterSlave.set(false);*/
        if (args[2] instanceof RedisURI) {
            manager.setDynamicField(args[0], LettuceConstants.DYNAMIC_FIELD_REDIS_URIS, Collections.singletonList((RedisURI) args[2]));
            /**
             * 缓存masterSlave
             */
            LettuceUtils.cacheMasterSlave(Collections.singletonList((RedisURI) args[2]));
            cacheNodesInfo(Collections.singletonList((RedisURI) args[2]));
        } else {
            manager.setDynamicField(args[0], LettuceConstants.DYNAMIC_FIELD_REDIS_URIS, (List<RedisURI>) args[2]);
            /**
             * 缓存masterSlave
             */
            LettuceUtils.cacheMasterSlave((List<RedisURI>) args[2]);
            cacheNodesInfo((List<RedisURI>) args[2]);
        }
    }

    void cacheNodesInfo(List<RedisURI> redisURIS) {
        boolean isSentinel = false;
        try {
            List<String> indexes = new ArrayList<String>();
            StringBuilder nodeBuilder = new StringBuilder();
            String password = null;
            String nodes = "";
            String db = "";

            for (RedisURI redisURI : redisURIS) {
                /**
                 * 先判断是否哨兵模式，如果是哨兵模式，取里面的sentinels,masterId为sentinelMasterId
                 */
                if (!StringUtil.isEmpty(redisURI.getSentinelMasterId())) {
                    isSentinel = true;
                    String sentinelMasterId = redisURI.getSentinelMasterId();
                    List<RedisURI> lists = redisURI.getSentinels();
                    for (RedisURI inner : lists) {
                        String node = inner.getHost().concat(":").concat(String.valueOf(inner.getPort()));
                        indexes.add(node);
                        nodeBuilder.append(node).append(",");
                        password = inner.getPassword() == null ? null : new String(inner.getPassword());
                        db = String.valueOf(inner.getDatabase());
                    }
                    nodes = nodeBuilder.deleteCharAt(nodeBuilder.length() - 1).toString();
                    Attachment attachment = new Attachment(indexes, "redis-lettuce",
                            new String[]{"redis"},
                            new RedisTemplate.LettuceSentinelTemplate()
                                    .setMaster(sentinelMasterId)
                                    .setNodes(nodes)
                                    .setPassword(password));

                    ResourceManager.set(attachment);

                }
                if (isSentinel) {
                    continue;
                }
                /**
                 * 主从模式,lettuce的主从不需要用户关注master,为了页面展示，放一个节点到Master
                 */
                String node = redisURI.getHost().concat(":").concat(String.valueOf(redisURI.getPort()));
                indexes.add(node);
                nodeBuilder.append(node).append(",");
                password = redisURI.getPassword() == null ? null : new String(redisURI.getPassword());
                db = String.valueOf(redisURI.getDatabase());
            }
            nodes = nodeBuilder.deleteCharAt(nodeBuilder.length() - 1).toString();

            Attachment attachment = new Attachment(indexes, "redis-lettuce",
                    new String[]{"redis"},
                    new RedisTemplate.LettuceMasterSlaveTemplate()
                            .setMaster(null)
                            .setNodes(nodes)
                            .setPassword(password));

            ResourceManager.set(attachment);

        } catch (Throwable t) {
            logger.error(ThrowableUtils.toString(t));
        }
    }

    @Override
    public void doAfter(Advice advice) {
        Object[] args = advice.getParameterArray();
        String methodName = advice.getBehaviorName();
        Object target = advice.getTarget();
        Object result = advice.getReturnObj();
        manager.setDynamicField(args, LettuceConstants.DYNAMIC_FIELD_LETTUCE_RESULT, result);
        if (args[args.length-1] instanceof RedisURI) {
            manager.setDynamicField(args, LettuceConstants.DYNAMIC_FIELD_REDIS_URIS, Collections.singletonList((RedisURI) args[args.length-1]));
        } else {
            manager.setDynamicField(args, LettuceConstants.DYNAMIC_FIELD_REDIS_URIS, (List<RedisURI>) args[args.length-1]);
        }
        manager.setDynamicField(args, LettuceConstants.DYNAMIC_FIELD_LETTUCE_TARGET, target);
        manager.setDynamicField(args, LettuceConstants.DYNAMIC_FIELD_LETTUCE_METHOD, methodName);
        manager.setDynamicField(args, LettuceConstants.DYNAMIC_FIELD_LETTUCE_ARGS, args);
    }
}
