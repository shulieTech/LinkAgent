package com.pamirs.attach.plugin.jedis.interceptor;

import com.pamirs.attach.plugin.common.datasource.redisserver.RedisClientMediator;
import com.pamirs.attach.plugin.jedis.destroy.JedisDestroyed;
import com.pamirs.attach.plugin.jedis.util.RedisUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Protocol;

import java.util.Collection;
import java.util.Set;

/**
 * @author jiangjibo
 * @date 2022/3/17 9:58 上午
 * @description:
 */
@Destroyable(JedisDestroyed.class)
@ListenerBehavior(isFilterClusterTest = true)
public class JedisConnectionSendCommandInterceptor extends MJedisInterceptor {

    private static final Set<String> METHOD_KEYS = RedisUtils.get().keySet();

    protected final static Logger LOGGER = LoggerFactory.getLogger(JedisInterceptor.class.getName());

    @Override
    public Object[] getParameter0(Advice advice) {
        ClusterTestUtils.validateClusterTest();

        Object[] args = advice.getParameterArray();

        printInvokeEnvs("before", advice.getBehaviorName(), args);

        if (JedisInterceptor.interceptorApplied.get()) {
            return args;
        }
        if (args == null || args.length < 2) {
            return args;
        }


        if (!Pradar.isClusterTest()) {
            return args;
        }

        if (RedisClientMediator.isShadowDb()) {
            return args;
        }
        Protocol.Command cmd = (Protocol.Command) args[0];
        String methodName = cmd.name().toLowerCase();

        if (!METHOD_KEYS.contains(methodName)) {
            return args;
        }

        if (RedisUtils.IGNORE_NAME.contains(methodName)) {
            return args;
        }

        //jedis db非0时候选择不做处理
        if ("select".equals(methodName)) {
            return args;
        }

        Collection<String> whiteList = GlobalConfig.getInstance().getCacheKeyWhiteList();
        boolean canMatchWhiteList = false;
        if (readMethod.contains(methodName)) {
            canMatchWhiteList = true;
        }

        Object[] args1;
        if (args[1] instanceof String[]) {
            int length = ((String[]) args[1]).length;
            args1 = new Object[length];
            System.arraycopy(args[1], 0, args1, 0, length);
        } else {
            int length = ((byte[][]) args[1]).length;
            args1 = new Object[length];
            System.arraycopy(args[1], 0, args1, 0, length);
        }
        args = args1;

        if (RedisUtils.EVAL_METHOD_NAME.contains(methodName)) {
            args = processEvalMethodName(args, whiteList, canMatchWhiteList);
        } else if (RedisUtils.METHOD_MORE_KEYS.containsKey(methodName)) {
            args = processMoreKeys(methodName, args, whiteList, canMatchWhiteList);
        } else if ("xread".equals(methodName)) {
            args = processXRead(args, whiteList);
        } else if ("xreadGroup".equals(methodName)) {
            args = processXReadGroup(args, whiteList);
        } else if ("mset".equals(methodName) || "msetnx".equals(methodName)) {
            args = processMset(args, whiteList, canMatchWhiteList);
        } else {
            args = process(args, whiteList, canMatchWhiteList);
        }

        Object[] values = new Object[2];
        values[0] = cmd;

        Object array;
        if (args[0] instanceof byte[]) {
            array = new byte[args.length][];
        } else {
            array = new String[args.length];
        }
        System.arraycopy(args, 0, array, 0, args.length);
        values[1] = array;

        printInvokeEnvs("after", advice.getBehaviorName(), values);

        return values;
    }

    private void printInvokeEnvs(String order, String method, Object[] args) {
        if (args == null || args.length == 0) {
            return;
        }
        Object[] params = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof String) {
                params[i] = args[i];
            } else if (args[i] instanceof String[]) {
                params[i] = args[i];
            } else if (args[i] instanceof byte[]) {
                params[i] = new String((byte[]) args[i]);
            } else if (args[i] instanceof byte[][]) {
                byte[][] bts = (byte[][]) args[i];
                String[] strings = new String[bts.length];
                for (int i1 = 0; i1 < bts.length; i1++) {
                    strings[i] = new String(bts[i]);
                }
                params[i] = strings;
            }else if(args[i] instanceof Number){
                params[i] = ((Number)args[i]).toString();
            }
        }
        LOGGER.info("[redis-jedis], class:{},  order:{}, method:{}, params:{}, nanno time:{}", this.getClass().getSimpleName(), order, method, params, System.nanoTime());
    }

}
