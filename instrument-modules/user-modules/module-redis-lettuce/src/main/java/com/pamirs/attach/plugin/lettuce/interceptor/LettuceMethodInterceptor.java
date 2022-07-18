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
import com.pamirs.attach.plugin.lettuce.LettuceConstants;
import com.pamirs.attach.plugin.lettuce.destroy.LettuceDestroy;
import com.pamirs.attach.plugin.lettuce.utils.ParameterUtils;
import com.pamirs.attach.plugin.lettuce.utils.ReflectFieldCache;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import io.lettuce.core.AbstractRedisAsyncCommands;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisURI;
import io.lettuce.core.masterslave.MasterSlaveConnectionProvider;
import io.lettuce.core.protocol.DefaultEndpoint;
import io.netty.channel.Channel;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.sql.Ref;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

@Destroyable(LettuceDestroy.class)
public class LettuceMethodInterceptor extends TraceInterceptorAdaptor {
    protected final static Logger logger = LoggerFactory.getLogger(LettuceMethodInterceptor.class);

    /**
     * 防止{@link AbstractRedisAsyncCommands#set(java.lang.Object, java.lang.Object)}等方法
     * 和{@link AbstractRedisAsyncCommands#dispatch(io.lettuce.core.protocol.ProtocolKeyword, io.lettuce.core.output.CommandOutput, io.lettuce.core.protocol.CommandArgs)}增强方法重复执行
     */
    public static ThreadLocal<Boolean> interceptorApplied = new ThreadLocal<Boolean>(){
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
        spanRecord.setService(methodName);
        spanRecord.setMethod(getMethodNameExt(args));
        spanRecord.setRequest(toArgs(args));
        spanRecord.setMiddlewareName(LettuceConstants.MIDDLEWARE_NAME);
        return spanRecord;
    }
    private void setRemoteIpAndPort(Object target,SpanRecord spanRecord){
        final List<Object> redisUris = manager.getDynamicField(target, LettuceConstants.DYNAMIC_FIELD_REDIS_URIS);
        if(redisUris != null && !redisUris.isEmpty()){
            final List<String> remoteIps = new ArrayList<String>();
            for (Object redisUri : redisUris) {
                final String host = Reflect.on(redisUri).get("host");
                final Integer port = Reflect.on(redisUri).get("port");
                remoteIps.add(host + ":" + port);
            }
            spanRecord.setRemoteIp(StringUtils.join(remoteIps,","));
        }else {
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
            final Object connection = Reflect.on(target).get(ReflectFieldCache.get(LettuceConstants.REFLECT_FIELD_CONNECTION, target));

            final Object t = Reflect.on(connection).get(ReflectFieldCache.get(LettuceConstants.REFLECT_FIELD_CHANNEL_WRITER, connection));
            ;
            DefaultEndpoint endpoint = null;
            if ("io.lettuce.core.masterslave.MasterSlaveChannelWriter".equals(t.getClass().getName())) {
                try {
                    /**
                     * 这是主从的
                     */
                    MasterSlaveConnectionProvider provider = Reflect.on(t).get(ReflectFieldCache.get("masterSlaveConnectionProvider", t));
                    RedisURI redisUri = Reflect.on(provider).get(ReflectFieldCache.get("initialRedisUri", provider));
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
                    Object sentinelConnector = Reflect.on(t).get(ReflectFieldCache.get("this$0", t));
                    RedisURI redisURI = Reflect.on(sentinelConnector).get(ReflectFieldCache.get("redisURI", sentinelConnector));
                    List<RedisURI> sentinels = redisURI.getSentinels();
                    RedisURI current = sentinels.get(0);
                    spanRecord.setRemoteIp(current.getHost());
                    spanRecord.setPort(current.getPort());
                } catch (Throwable thx) {
                    spanRecord.setRemoteIp(LettuceConstants.ADDRESS_UNKNOW);
                }
                return;
            }
            if (t instanceof DefaultEndpoint) {
                endpoint = (DefaultEndpoint) t;
            } else {
                try {
                    endpoint = Reflect.on(t).get(ReflectFieldCache.get(LettuceConstants.REFLECT_FIELD_DEFAULT_WRITER, t));
                } catch (Throwable w) {
                    endpoint = Reflect.on(t).get(ReflectFieldCache.get(LettuceConstants.REFLECT_FIELD_WRITER, t));
                }
            }
            if (endpoint == null) {
                spanRecord.setRemoteIp(LettuceConstants.ADDRESS_UNKNOW);
                return;
            }
            Channel channel = Reflect.on(endpoint).get(ReflectFieldCache.get(LettuceConstants.REFLECT_FIELD_CHANNEL, endpoint));
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
