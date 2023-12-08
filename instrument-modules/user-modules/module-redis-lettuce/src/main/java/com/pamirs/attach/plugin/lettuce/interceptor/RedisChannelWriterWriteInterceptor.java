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

import com.pamirs.attach.plugin.lettuce.LettuceConstants;
import com.pamirs.attach.plugin.lettuce.destroy.LettuceDestroy;
import com.pamirs.attach.plugin.lettuce.utils.LettuceMetaData;
import com.pamirs.pradar.InvokeContext;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandWrapper;
import io.lettuce.core.protocol.DefaultEndpoint;
import io.lettuce.core.protocol.RedisCommand;
import io.netty.channel.Channel;

import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;

/**
 * @author angju
 * @date 2022/9/29 17:41
 */
@Destroyable(LettuceDestroy.class)
public class RedisChannelWriterWriteInterceptor extends AroundInterceptor {

    private static boolean endpointFieldResult = true;

    @Resource
    protected DynamicFieldManager manager;

    private static Map<String, Integer> classIndexMap = new HashMap<String, Integer>(8, 1);
    private static Set<String> excludeCommandSet = new HashSet<String>();

    static {
        classIndexMap.put("io.lettuce.core.protocol.CommandArgs$KeyArgument", 1);
        classIndexMap.put("io.lettuce.core.protocol.CommandArgs$ValueArgument", 2);
        classIndexMap.put("io.lettuce.core.protocol.CommandArgs$IntegerArgument", 2);
        classIndexMap.put("io.lettuce.core.protocol.CommandArgs$StringArgument", 2);
        classIndexMap.put("io.lettuce.core.protocol.CommandArgs$BytesArgument", 2);
        classIndexMap.put("io.lettuce.core.protocol.CommandArgs$DoubleArgument", 2);

        excludeCommandSet.add("HELLO");
        excludeCommandSet.add("AUTH");
        excludeCommandSet.add("SELECT");
    }

    @Override
    public void doBefore(Advice advice) {
        Object[] methodArguments = advice.getParameterArray();
        Object command = methodArguments[0];
        if (command instanceof CommandWrapper) {
            if ("io.lettuce.core.protocol.TracedCommand".equals(command.getClass().getName())) {
                if (Reflect.on(command).existsMethod("getDelegate")) {
                    command = Reflect.on(command).call("getDelegate").get();
                }
            }
        }

        if(!(command instanceof RedisCommand)){
            return;
        }
        if (manager.hasDynamicField(command,LettuceConstants.INVOKE_CONTENT)) {
            return;
        }
        final String methodName = ((RedisCommand) command).getType().name();
        if (excludeCommandSet.contains(methodName)) {
            return;
        }

        final StringBuilder args = new StringBuilder();
        RedisCommand<?, ?, ?> redisCommand = (RedisCommand<?, ?, ?>) command;
        String opt = String.valueOf(redisCommand.getType().name());
        args.append(getArgsStatement(redisCommand));
        LettuceMetaData lettuceMetaData = doGetLettuceMetaData(advice.getTarget());
        InvokeContext invokeContext = Pradar.startClientInvoke(lettuceMetaData.getHost(), opt);
        invokeContext.setRequest(args.toString());
        invokeContext.setCallBackMsg(opt + " " + args);
        invokeContext.setMiddlewareName(LettuceConstants.MIDDLEWARE_NAME);
        invokeContext.setRemoteIp(lettuceMetaData.getHost());
        invokeContext.setPort(String.valueOf(lettuceMetaData.getPort()));
        Map<String, String> contextMap = Pradar.getInvokeContextMap();
        manager.setDynamicField(command, LettuceConstants.INVOKE_CONTENT, contextMap);
    }


    @Override
    public void doAfter(Advice advice) throws Throwable {
        if (!Pradar.hasInvokeContext()) {
            return;
        }
        Pradar.popInvokeContext();
    }

    @Override
    public void doException(Advice advice) throws Throwable {
        Object[] methodArguments = advice.getParameterArray();
        Object command = methodArguments[0];
        if (command instanceof CommandWrapper) {
            if ("io.lettuce.core.protocol.TracedCommand".equals(command.getClass().getName())) {
                if (Reflect.on(command).existsMethod("getDelegate")) {
                    command = Reflect.on(command).call("getDelegate").get();
                }
            }
        }
        if(!(command instanceof RedisCommand)){
            return;
        }
        if (manager.hasDynamicField(command, LettuceConstants.INVOKE_CONTENT)) {
            manager.removeField(command, LettuceConstants.INVOKE_CONTENT);
        }
        if (!Pradar.hasInvokeContext()) {
            return;
        }
        Pradar.popInvokeContext();
    }




    private String getArgsStatement(RedisCommand<?, ?, ?> redisCommand) {
        String statement;
        CommandArgs<?, ?> args = redisCommand.getArgs();
        statement = (args != null) ? args.toCommandString() : "";
        return statement;
    }

    protected LettuceMetaData doGetLettuceMetaData(Object target) {
        if (target instanceof DefaultEndpoint) {
            return getLettuceMetaDataFromDefaultEndpoint(target);
        } else if ("io.lettuce.core.protocol.CommandExpiryWriter".equals(target.getClass().getName())) {
            return getLettuceMetaDataFromCommandExpiryWriter(target);
        } else if ("io.lettuce.core.cluster.ClusterDistributionChannelWriter".equals(target.getClass().getName())) {
            return getLettuceMetaDataFromClusterDistributionChannelWriter(target);
        }
        return LettuceMetaData.getDefault();
    }

    private LettuceMetaData getLettuceMetaDataFromCommandExpiryWriter(Object commandExpiryWriter) {
        try {
            Object defaultEndpoint = null;
            if (Reflect.on(commandExpiryWriter).existsField("delegate")) {
                defaultEndpoint = Reflect.on(commandExpiryWriter).get("delegate");
            } else if (Reflect.on(commandExpiryWriter).existsField("writer")) {
                defaultEndpoint = Reflect.on(commandExpiryWriter).get("writer");
            }
            if (defaultEndpoint instanceof DefaultEndpoint) {
                return getLettuceMetaDataFromDefaultEndpoint(defaultEndpoint);
            }

        } catch (Throwable e) {
            endpointFieldResult = false;
            LOGGER.error("get LettuceMetaData fail!", e);
        }
        return LettuceMetaData.getDefault();
    }

    private LettuceMetaData getLettuceMetaDataFromClusterDistributionChannelWriter(
            Object clusterDistributionChannelWriter) {
        try {
            Object defaultEndpoint = null;
            if (Reflect.on(clusterDistributionChannelWriter).existsField("defaultWriter")) {
                defaultEndpoint = Reflect.on(clusterDistributionChannelWriter).get("defaultWriter");
            }
            if (defaultEndpoint instanceof DefaultEndpoint) {
                return getLettuceMetaDataFromDefaultEndpoint(defaultEndpoint);
            } else {
                if (Reflect.on(defaultEndpoint).existsField("writer")) {
                    defaultEndpoint = Reflect.on(defaultEndpoint).get("writer");
                    return getLettuceMetaDataFromDefaultEndpoint(defaultEndpoint);
                }
            }

        } catch (Throwable e) {
            endpointFieldResult = false;
            LOGGER.error("get LettuceMetaData fail!", e);
        }
        return LettuceMetaData.getDefault();
    }

    private LettuceMetaData getLettuceMetaDataFromDefaultEndpoint(Object defaultEndpoint) {
        LettuceMetaData lettuceMetaData = LettuceMetaData.getDefault();
        try {
            if (!endpointFieldResult) {
                return lettuceMetaData;
            }

            Channel channel = Reflect.on(defaultEndpoint).get(LettuceConstants.REFLECT_FIELD_CHANNEL);
            if (channel == null) {
                endpointFieldResult = false;
                return lettuceMetaData;
            }
            SocketAddress socketAddress = channel.remoteAddress();
            if (!(socketAddress instanceof InetSocketAddress)) {
                String address = socketAddress.toString();
                lettuceMetaData.setHost(address);
                return lettuceMetaData;
            } else {
                lettuceMetaData.setHost(((InetSocketAddress) socketAddress).getAddress().getHostAddress());
                lettuceMetaData.setPort(((InetSocketAddress) socketAddress).getPort());
            }
            return lettuceMetaData;

        } catch (Throwable e) {
            endpointFieldResult = false;
            LOGGER.error("get LettuceMetaData fail!", e);
        }
        return lettuceMetaData;
    }
}
