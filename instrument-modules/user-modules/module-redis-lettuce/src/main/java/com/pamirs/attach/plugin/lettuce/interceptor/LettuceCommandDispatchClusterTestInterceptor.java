package com.pamirs.attach.plugin.lettuce.interceptor;

import com.pamirs.attach.plugin.common.datasource.redisserver.RedisClientMediator;
import com.pamirs.attach.plugin.dynamic.reflect.Reflect;
import com.pamirs.attach.plugin.lettuce.LettucePlugin;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import io.lettuce.core.protocol.ProtocolKeyword;

import java.util.*;

/**
 * @author jiangjibo
 * @date 2022/3/18 6:03 下午
 * @description:
 */
public class LettuceCommandDispatchClusterTestInterceptor extends ParametersWrapperInterceptorAdaptor {

    private static Set<String> SupportMethods = new HashSet<String>(256);

    static {
        for (String method : LettucePlugin.FIRST_ARGS_INCLUDE_METHODS) {
            SupportMethods.add(method);
        }
        SupportMethods.addAll(Arrays.asList("blpop", "brpop", "bzpopmin", "bzpopmax"));
        SupportMethods.addAll(Arrays.asList("eval", "evalsha"));
        SupportMethods.addAll(Arrays.asList("bitopAnd", "bitopNot", "bitopOr", "bitopXor", "rpoplpush", "sdiffstore", "sinterstore", "smove", "sunionstore", "pfmerge", "rename", "renamenx"));
        SupportMethods.addAll(Arrays.asList("sortStore"));
        SupportMethods.addAll(Arrays.asList("brpoplpush"));
        SupportMethods.addAll(Arrays.asList("xadd"));
        SupportMethods.addAll(Arrays.asList("xgroupCreate"));
        SupportMethods.addAll(Arrays.asList("xgroupSetid"));
        SupportMethods.addAll(Arrays.asList("xread"));
        SupportMethods.addAll(Arrays.asList("xreadgroup"));
        SupportMethods.addAll(Arrays.asList("zinterstore"));
        SupportMethods.addAll(Arrays.asList("zrange"));
        SupportMethods.addAll(Arrays.asList("zrangebyscore"));
        SupportMethods.addAll(Arrays.asList("zrangebyscoreWithScores"));
        SupportMethods.addAll(Arrays.asList("zrevrange"));
        SupportMethods.addAll(Arrays.asList("zrevrangeWithScores"));
        SupportMethods.addAll(Arrays.asList("zrevrangebyscore"));
        SupportMethods.addAll(Arrays.asList("zrevrangebyscoreWithScores"));
        SupportMethods.addAll(Arrays.asList("zrevrangebyscoreWithScores"));
        SupportMethods.addAll(Arrays.asList("zunionstore"));
        SupportMethods.addAll(Arrays.asList("hscan"));
        SupportMethods.addAll(Arrays.asList("hgetall"));
        SupportMethods.addAll(Arrays.asList("hkeys"));
        SupportMethods.addAll(Arrays.asList("keys"));
        SupportMethods.addAll(Arrays.asList("hmget"));
        SupportMethods.addAll(Arrays.asList("hvals"));
        SupportMethods.addAll(Arrays.asList("lrange"));
        SupportMethods.addAll(Arrays.asList("mget"));
        SupportMethods.addAll(Arrays.asList("migrate"));

        Set<String> lowerCases = new HashSet<String>(SupportMethods.size());
        for (String method : SupportMethods) {
            lowerCases.add(method.toUpperCase());
        }
        SupportMethods = lowerCases;
    }

    @Override
    protected Object[] getParameter0(Advice advice) throws Throwable {
        Object[] args = advice.getParameterArray();
        ClusterTestUtils.validateClusterTest();
        if (!Pradar.isClusterTest()) {
            return args;
        }

        if (RedisClientMediator.isShadowDb()) {
            return args;
        }

        String method = ((ProtocolKeyword) args[0]).name();
        if (!SupportMethods.contains(method)) {
            return args;
        }

        List singularArguments = Reflect.on(args[2]).get("singularArguments");
        Object keyArgument = singularArguments.get(0);

        byte[] bytes = Reflect.on(keyArgument).get("key");
        String rawKey = new String(bytes);
        String processedKey = processKey(rawKey);
        if (!rawKey.equals(processedKey)) {
            Reflect.on(keyArgument).set("key",processedKey.getBytes());
        }
        return args;
    }

    private String processKey(String key) {
        Collection<String> whiteList = GlobalConfig.getInstance().getCacheKeyWhiteList();
        if (ignore(whiteList, key)) {
            return key;
        }
        String str = key;
        if (!Pradar.isClusterTestPrefix(str)) {
            str = Pradar.addClusterTestPrefix(str);
        }
        return str;
    }

    private boolean ignore(Collection<String> whiteList, String key) {
        //白名单 忽略
        for (String white : whiteList) {
            if (key.startsWith(white)) {
                return true;
            }
        }
        return false;
    }
}
