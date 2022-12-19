package com.pamirs.attach.plugin.jedis.interceptor;

import com.pamirs.attach.plugin.dynamic.Attachment;
import com.pamirs.attach.plugin.dynamic.template.RedisTemplate;
import com.pamirs.attach.plugin.jedis.RedisConstants;
import com.pamirs.attach.plugin.jedis.destroy.JedisDestroyed;
import com.pamirs.attach.plugin.jedis.util.RedisUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import redis.clients.jedis.Client;
import redis.clients.jedis.Protocol;

import java.util.Set;

/**
 * @author jiangjibo
 * @date 2022/3/16 10:21 上午
 * @description:
 */
@Destroyable(JedisDestroyed.class)
@ListenerBehavior(isFilterBusinessData = true)
public class JedisProtocolSendCommandTraceInterceptor extends TraceInterceptorAdaptor {

    private static final Set<String> METHOD_KEYS = RedisUtils.get().keySet();

    @Override
    public String getPluginName() {
        return RedisConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return RedisConstants.PLUGIN_TYPE;
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {

        Object[] args = advice.getParameterArray();
        String method = new String((byte[]) args[1]);

        toArgs(args);

        printInvokeEnvs("trace_before", method, args);

        if (JedisInterceptor.interceptorApplied.get()) {
            return null;
        }

        Client client = (Client) advice.getTarget();
        if (!METHOD_KEYS.contains(method)) {
            return null;
        }

        printInvokeEnvs("trace_after", method, args);

        SpanRecord record = new SpanRecord();
        record.setService(method);
        record.setMethod(method);
        record.setRequestSize(0);
        record.setRemoteIp(client.getHost());
        record.setPort(client.getPort());
        record.setRequest(toArgs(args));
        record.setMiddlewareName(RedisConstants.MIDDLEWARE_NAME);
        return record;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        if (JedisInterceptor.interceptorApplied.get()) {
            return null;
        }
        String method = new String((byte[]) advice.getParameterArray()[1]);
        if (!METHOD_KEYS.contains(method)) {
            return null;
        }
        SpanRecord record = new SpanRecord();
        record.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
        record.setMiddlewareName(RedisConstants.MIDDLEWARE_NAME);
        attachment(advice);
        return record;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        if (JedisInterceptor.interceptorApplied.get()) {
            return null;
        }
        String method = ((Protocol.Command) advice.getParameterArray()[0]).name().toLowerCase();
        if (!METHOD_KEYS.contains(method)) {
            return null;
        }
        SpanRecord record = new SpanRecord();
        record.setResponse(advice.getThrowable());
        record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        record.setMiddlewareName(RedisConstants.MIDDLEWARE_NAME);
        return record;
    }

    private Object[] toArgs(Object[] args) {
        if (args.length <= 2) {
            return null;
        }
        Object[] ret = new Object[args.length - 2];
        for (int i = 2; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof String) {
                ret[i - 2] = arg;
            } else if (arg instanceof byte[]) {
                ret[i - 2] = new String((byte[]) arg);
            } else if (arg instanceof char[]) {
                ret[i - 2] = new String((char[]) arg);
            } else if (arg instanceof byte[][]) {
                byte[][] bts = (byte[][]) arg;
                String[] strings = new String[bts.length];
                for (int i1 = 0; i1 < bts.length; i1++) {
                    strings[i1] = new String(bts[i1]);
                }
                ret[i - 2] = strings;
            } else {
                ret[i - 2] = arg;
            }
        }
        return ret;
    }

    void attachment(Advice advice) {
        if (Pradar.isClusterTest()) {
            return;
        }
        Client client = (Client) advice.getTarget();
        //单机模式
        String node = client.getHost().concat(":").concat(String.valueOf(client.getPort()));
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
    }

    private void printInvokeEnvs(String order, String method, Object[] args) {
        if (args == null || args.length < 2) {
            return;
        }
        Object[] params = new Object[args.length - 2];
        for (int i = 0; i < args.length - 2; i++) {
            int x = i + 2;
            if (args[x] instanceof String) {
                params[i] = args[x];
            } else if (args[x] instanceof String[]) {
                params[i] = args[x];
            } else if (args[x] instanceof byte[]) {
                params[i] = new String((byte[]) args[x]);
            } else if (args[x] instanceof byte[][]) {
                byte[][] bts = (byte[][]) args[x];
                String[] strings = new String[bts.length];
                for (int i1 = 0; i1 < bts.length; i1++) {
                    strings[i1] = new String(bts[i1]);
                }
                params[i] = strings;
            } else if (args[x] instanceof Number) {
                params[i] = ((Number) args[i]).toString();
            } else if (args[x] instanceof Protocol.Command) {
                params[i] = ((Protocol.Command) args[i]).name();
            }
        }
        LOGGER.info("[redis-jedis], class:{},  order:{}, method:{}, params:{}, nanno time:{}", this.getClass().getSimpleName(), order, method, params, System.nanoTime());
    }

}
