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
import com.pamirs.attach.plugin.dynamic.resource.ConcurrentWeakHashMap;
import com.pamirs.attach.plugin.dynamic.template.RedisTemplate;
import com.pamirs.attach.plugin.jedis.RedisConstants;
import com.pamirs.attach.plugin.jedis.destroy.JedisDestroyed;
import com.pamirs.attach.plugin.jedis.util.Model;
import com.pamirs.attach.plugin.jedis.util.RedisUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import redis.clients.jedis.*;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Set;

/**
 * @author vincent
 * @version v0.1 2016/12/29 11:10
 */
@Destroyable(JedisDestroyed.class)
@ListenerBehavior(isFilterBusinessData = true)
public class JedisInterceptor extends TraceInterceptorAdaptor {
    Model model = Model.INSTANCE();
    private static ConcurrentWeakHashMap<Class, Field> pipelineClientFieldCache = new ConcurrentWeakHashMap<Class, Field>();

    @Override
    public String getPluginName() {
        return RedisConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return RedisConstants.PLUGIN_TYPE;
    }

    private Object[] toArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return args;
        }
        Object[] ret = new Object[args.length];
        for (int i = 0, len = args.length; i < len; i++) {
            Object arg = args[i];
            if (arg instanceof String) {
                ret[i] = arg;
            } else if (arg instanceof byte[]) {
                ret[i] = new String((byte[])arg);
            } else if (arg instanceof char[]) {
                ret[i] = new String((char[])arg);
            } else {
                ret[i] = arg;
            }
        }
        return ret;
    }

    void attachment(Advice advice, int stage) {
        try {

            if (Pradar.isClusterTest()) {
                return;
            }

            Jedis jedis = (Jedis) advice.getTarget();
            Client client = Reflect.on(jedis).get("client");
            String node = client.getHost().concat(":").concat(String.valueOf(client.getPort()));
            Object dataSource = null;
            boolean isSentinel = false;
            try {

                /**
                 * 哨兵模式的端口会经过处理，不是配置的，所以这里判断一下
                 */
                dataSource = Reflect.on(jedis).get("dataSource");
                if (dataSource != null &&
                        JedisSentinelPool.class.isAssignableFrom(dataSource.getClass())) {
                    isSentinel = true;
                }

                /**
                 * 判断下拦截的方法，如果是slaveOf,说明是主从模式，和哨兵模式有一定区别
                 */
                String method = advice.getBehavior().getName();
                /**
                 * 这个实在难搞，加上对哨兵的过滤，因为哨兵也是基于主从做的
                 */
                if ("slaveof".equals(method.toLowerCase()) && !isSentinel) {
                    Object[] parameters = advice.getParameterArray();
                    String master = parameters[0] + ":" + parameters[1];
                    String slave = node;
                    model.setMasterSlaveMode(slave, master);
                }
            } catch (Throwable t) {

            }

            switch (stage) {
                case 0: {
                    /**
                     * 主从模式
                     */
                    if (model.isMasterSlave(node)) {
                        String slave = node;
                        String master = model.getMasterBySlave(slave);
                        /**
                         * 忽略掉没拿到master的数据
                         * 暂时不支持jedis的masterslave
                         */
                      /*  if (master == null) {
                            break;
                        }
                        Attachment ext = new Attachment(
                                null, RedisConstants.PLUGIN_NAME, new String[]{RedisConstants.MIDDLEWARE_NAME},
                                new RedisTemplate.JedisMasterSlaveTemplate()
                                        .setMaster(master)
                                        .setNodes(slave));
                        Pradar.getInvokeContext().setExt(ext);*/
                        break;


                    } else if (model.isClusterMode(node)) {
                        /**
                         * 集群模式
                         */
                        Pradar.getInvokeContext().setIndex(node);
                        break;
                    } else if (isSentinel) {
                        /**
                         * 哨兵模式
                         */
                        JedisSentinelPool jedisSentinelPool = (JedisSentinelPool) dataSource;
                        String password = Reflect.on(jedisSentinelPool).get("password");
                        Integer database = Reflect.on(jedisSentinelPool).get("database");
                        Set set = Reflect.on(jedisSentinelPool).get("masterListeners");
                        Iterator iterator = set.iterator();
                        StringBuilder nodeBuilder = new StringBuilder();
                        String masterName = null;
                        while (iterator.hasNext()) {
                            Object t = iterator.next();
                            Reflect ref = Reflect.on(t);
                            masterName = ref.get("masterName");
                            String host = ref.get("host");
                            String port = String.valueOf(ref.get("port"));
                            nodeBuilder.append(host.concat(":").concat(port))
                                    .append(",");

                        }
                        Attachment ext = new Attachment(
                                null, RedisConstants.PLUGIN_NAME, new String[]{RedisConstants.MIDDLEWARE_NAME},
                                new RedisTemplate.JedisSentinelTemplate()
                                        .setMaster(masterName)
                                        .setNodes(nodeBuilder.deleteCharAt(nodeBuilder.length() - 1).toString())
                                        .setDatabase(database)
                                        .setPassword(password));
                        Pradar.getInvokeContext().setExt(ext);
                    } else {
                        //单机模式
                        String password = Reflect.on(client).get("password");
                        int db = Integer.parseInt(String.valueOf(Reflect.on(client).get("db")));
                        Attachment ext = new Attachment(node, RedisConstants.PLUGIN_NAME,
                                new String[]{RedisConstants.MIDDLEWARE_NAME}
                                , new RedisTemplate.JedisSingleTemplate()
                                .setNodes(node)
                                .setPassword(password)
                                .setDatabase(db)
                        );
                        Pradar.getInvokeContext().setExt(ext);
                        break;
                    }

                }
                case 1:
                    return;

            }


        } catch (Throwable t) {

        }
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        String methodName = advice.getBehaviorName();
        Object target = advice.getTarget();

        SpanRecord record = new SpanRecord();
        record.setService(methodName);
        record.setMethod(methodName);
        record.setRequestSize(0);

        Client client = getClient(target);
        record.setRemoteIp(client.getHost());
        record.setPort(client.getPort());
        //record.setCallbackMsg(client.getDB()+"");

        record.setRequest(toArgs(args));
        record.setMiddlewareName(RedisConstants.MIDDLEWARE_NAME);
        return record;
    }


    private Client getClientFromPipeline(Object target) {
        if (target == null) {
            return null;
        }
        Field field = pipelineClientFieldCache.get(target.getClass());
        if (field == null) {
            Field oldField = pipelineClientFieldCache.putIfAbsent(target.getClass(), Reflect.on(target).field0("client"));
            if (oldField != null) {
                field = oldField;
            }
        }
        try {
            return (Client) field.get(target);
        } catch (Throwable e) {
            return Reflect.on(target).get("client");
        }
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        Object result = advice.getReturnObj();
        SpanRecord record = new SpanRecord();
        record.setResponse(result);
        record.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
        record.setMiddlewareName(RedisConstants.MIDDLEWARE_NAME);
        attachment(advice, 0);
        return record;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        SpanRecord record = new SpanRecord();
        record.setResponse(advice.getThrowable());
        record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        record.setMiddlewareName(RedisConstants.MIDDLEWARE_NAME);
        return record;
    }

    private Client getClient(Object target) {
        Client client;
        if (target instanceof BinaryJedis) {
            BinaryJedis binaryJedis = (BinaryJedis) target;
            client = binaryJedis.getClient();
        } else if (target instanceof Pipeline) {
            client = getClientFromPipeline(target);
        } else {
            throw new PressureMeasureError("target is not support: " + target.getClass());
        }
        return client;
    }
}
