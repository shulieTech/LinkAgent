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
package com.pamirs.attach.plugin.lettuce.interceptor;

import com.pamirs.attach.plugin.dynamic.ResourceManager;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.attach.plugin.lettuce.LettuceConstants;
import com.pamirs.attach.plugin.lettuce.destroy.LettuceDestroy;
import com.pamirs.attach.plugin.lettuce.utils.ParameterUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import io.lettuce.core.AbstractRedisAsyncCommands;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.masterslave.MasterSlaveConnectionProvider;
import io.lettuce.core.protocol.DefaultEndpoint;
import io.netty.channel.Channel;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.util.StringJoiner;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;

@Destroyable(LettuceDestroy.class)
public class LettuceMethodInterceptor extends TraceInterceptorAdaptor {
    protected final static Logger logger = LoggerFactory.getLogger(LettuceMethodInterceptor.class);

    /**
     * 防止{@link AbstractRedisAsyncCommands#set(java.lang.Object, java.lang.Object)}等方法
     * 和{@link AbstractRedisAsyncCommands#dispatch(io.lettuce.core.protocol.ProtocolKeyword, io.lettuce.core.output.CommandOutput, io.lettuce.core.protocol.CommandArgs)}增强方法重复执行
     */
    public static ThreadLocal<Boolean> interceptorApplied = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    @Override
    public String getPluginName() {
        return LettuceConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return LettuceConstants.PLUGIN_TYPE;
    }

    @Resource
    protected DynamicFieldManager manager;

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        interceptorApplied.set(true);
        Object[] args = advice.getParameterArray();
        String methodName = advice.getBehaviorName();
        Object target = advice.getTarget();
        SpanRecord spanRecord = new SpanRecord();
        appendEndPoint(target, spanRecord);
        spanRecord.setMethod(methodName);
        spanRecord.setService(methodName);
        spanRecord.setRequest(toArgs(args));
        spanRecord.setMiddlewareName(LettuceConstants.MIDDLEWARE_NAME);
        return spanRecord;
    }

    private void setRemoteIpAndPort(Object target, SpanRecord spanRecord) {
        final List<Object> redisUris = manager.getDynamicField(target, LettuceConstants.DYNAMIC_FIELD_REDIS_URIS);
        if (redisUris != null && !redisUris.isEmpty()) {
            final List<String> remoteIps = new ArrayList<String>();
            for (Object redisUri : redisUris) {
                final String host = ReflectionUtils.get(redisUri, "host");
                final Integer port = ReflectionUtils.get(redisUri, "port");
                remoteIps.add(host + ":" + port);
            }
            spanRecord.setRemoteIp(StringUtils.join(remoteIps, ","));
        } else {
            spanRecord.setRemoteIp(LettuceConstants.ADDRESS_UNKNOW);
        }

    }

    private Object[] toArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return args;
        }
        Object[] ret = new Object[args.length];
        for (int i = 0, len = args.length; i < len; i++) {
            Object arg = args[i];
            if (arg instanceof byte[]) {
                ret[i] = new String((byte[]) arg);
            } else if (arg instanceof char[]) {
                ret[i] = new String((char[]) arg);
            } else {
                ret[i] = arg;
            }
        }
        return ret;
    }

    public static String getMethodNameExt(Object... args) {
        if (args == null || args.length == 0) {
            return "";
        }
        return ParameterUtils.toString(200, args[0]);
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        interceptorApplied.set(false);
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setMiddlewareName(LettuceConstants.MIDDLEWARE_NAME);
        spanRecord.setCallbackMsg(LettuceConstants.PLUGIN_NAME);
        /**
         * 附加属性
         */
        ext();
        return spanRecord;
    }


    void ext() {
        try {
            if (Pradar.isClusterTest()) {
                return;
            }
            String index = Pradar.getInvokeContext().getRemoteIp()
                    .concat(":")
                    .concat(Pradar.getInvokeContext().getPort());

            Object attachment =
                    ResourceManager.get(index, LettuceConstants.MIDDLEWARE_NAME);
            if (attachment != null) {
                Pradar.getInvokeContext().setExt(attachment);
            }

        } catch (Throwable t) {

        }
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        interceptorApplied.set(false);
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setResponse(advice.getThrowable());
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        spanRecord.setMiddlewareName(LettuceConstants.MIDDLEWARE_NAME);
        spanRecord.setCallbackMsg(LettuceConstants.PLUGIN_NAME);
        /**
         * 附加属性
         */
        ext();
        return spanRecord;
    }

    protected void appendEndPoint(Object target, final SpanRecord spanRecord) {
        try {
            final Object connection = ReflectionUtils.get(target, LettuceConstants.REFLECT_FIELD_CONNECTION);
            final Object t = ReflectionUtils.get(connection, LettuceConstants.REFLECT_FIELD_CHANNEL_WRITER);
            DefaultEndpoint endpoint = null;
            if ("io.lettuce.core.masterslave.MasterSlaveChannelWriter".equals(t.getClass().getName())) {
                try {
                    /**
                     * 这是主从的
                     */
                    MasterSlaveConnectionProvider provider = ReflectionUtils.get(t, "masterSlaveConnectionProvider");
                    RedisURI redisUri = ReflectionUtils.get(provider, "initialRedisUri");
                    spanRecord.setRemoteIp(redisUri.getHost());
                    spanRecord.setPort(redisUri.getPort());
                } catch (Throwable thx) {
                    spanRecord.setRemoteIp(LettuceConstants.ADDRESS_UNKNOW);
                }
                return;
                /**
                 * 这是哨兵的
                 */
            } else if ("io.lettuce.core.masterslave.SentinelConnector$1".equals(t.getClass().getName())) {
                try {
                    Object sentinelConnector = ReflectionUtils.get(t, "this$0");
                    RedisURI redisURI = ReflectionUtils.get(sentinelConnector, "redisURI");
                    List<RedisURI> sentinels = redisURI.getSentinels();
                    RedisURI current = sentinels.get(0);
                    spanRecord.setRemoteIp(current.getHost());
                    spanRecord.setPort(current.getPort());
                } catch (Throwable thx) {
                    spanRecord.setRemoteIp(LettuceConstants.ADDRESS_UNKNOW);
                }
                return;
            }
            /**
             * 集群模式
             */
            else if ("io.lettuce.core.cluster.ClusterDistributionChannelWriter".equals(t.getClass().getName())) {
                Partitions partitions = ReflectionUtils.getFieldValues(t, "clusterEventListener", "redisClusterClient", "partitions");
                Set<String> remoteIpPorts = new HashSet<String>();
                for (RedisClusterNode partition : partitions) {
                    RedisURI redisURI = partition.getUri();
                    remoteIpPorts.add(redisURI.getHost() + ":" + redisURI.getPort());
                }
                String join = StringUtils.join(remoteIpPorts, ",");
                spanRecord.setRemoteIp(join);
                return;
            }
            if (t instanceof DefaultEndpoint) {
                endpoint = (DefaultEndpoint) t;
            } else {
                try {
                    endpoint = ReflectionUtils.get(t, LettuceConstants.REFLECT_FIELD_DEFAULT_WRITER);
                } catch (Throwable w) {
                    endpoint = ReflectionUtils.get(t, LettuceConstants.REFLECT_FIELD_WRITER);
                }
            }
            if (endpoint == null) {
                spanRecord.setRemoteIp(LettuceConstants.ADDRESS_UNKNOW);
                return;
            }
            Channel channel = ReflectionUtils.get(endpoint, LettuceConstants.REFLECT_FIELD_CHANNEL);
            if (channel == null) {
                spanRecord.setRemoteIp(LettuceConstants.ADDRESS_UNKNOW);
                return;
            }
            SocketAddress socketAddress = channel.remoteAddress();
            if (socketAddress == null) {
                spanRecord.setRemoteIp(LettuceConstants.ADDRESS_UNKNOW);
                return;
            }
            if (!(socketAddress instanceof InetSocketAddress)) {
                spanRecord.setRemoteIp(LettuceConstants.ADDRESS_UNKNOW);
                return;
            }
            InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
            spanRecord.setRemoteIp(inetSocketAddress.getAddress().getHostAddress());
            spanRecord.setPort(inetSocketAddress.getPort());
        } catch (Throwable e) {
            spanRecord.setRemoteIp(LettuceConstants.ADDRESS_UNKNOW);
        }
    }
}
